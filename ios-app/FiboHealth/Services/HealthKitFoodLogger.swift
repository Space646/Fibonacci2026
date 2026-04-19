import HealthKit
import Combine
import Foundation
import os

/// Reconciles the Pi's food log into HealthKit food correlations.
///
/// Source of truth for the Pi↔HK mapping is HealthKit metadata
/// (`pi_food_id` + `source_app == "FiboHealth"`) — there is no local cache,
/// so deletions on the Pi cascade cleanly to HealthKit.
final class HealthKitFoodLogger: ObservableObject {
    static let metaPiId      = "pi_food_id"
    static let metaSourceApp = "source_app"
    static let sourceAppValue = "FiboHealth"

    @Published var lastError: String?

    private let store = HKHealthStore()
    private var activeReconcile: Task<Void, Never>?

    private static let log = Logger(
        subsystem: "com.fibohealth.app",
        category: "HealthKitFoodLogger"
    )

    private static let isoFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withFullDate, .withTime, .withColonSeparatorInTime,
                           .withDashSeparatorInDate]
        return f
    }()

    // MARK: - Pure core (tested in HealthKitFoodLoggerDiffTests)

    static func diff(piIds: Set<Int>, hkIds: Set<Int>)
        -> (toInsert: Set<Int>, toDelete: Set<Int>)
    {
        return (toInsert: piIds.subtracting(hkIds),
                toDelete: hkIds.subtracting(piIds))
    }

    // MARK: - Authorization

    func requestWriteAuthorization() async {
        await MainActor.run { self.lastError = nil }
        guard HKHealthStore.isHealthDataAvailable() else { return }
        do {
            try await store.requestAuthorization(
                toShare: HealthKitService.nutritionShareTypes, read: []
            )
        } catch {
            await MainActor.run { self.lastError = error.localizedDescription }
        }
    }

    // MARK: - Reconciliation

    /// Add HK correlations for entries newly seen on the Pi; delete HK
    /// correlations whose `pi_food_id` no longer appears in the Pi log.
    func reconcile(with entries: [FoodLogEntry]) async {
        // Issue 3: clear stale error at the start of every run.
        await MainActor.run { self.lastError = nil }

        // Issue 1: single-flight serialization — wait for any in-flight reconcile
        // before starting a new one so two concurrent callers cannot both read
        // hkIds before either writes and produce duplicate correlations.
        await activeReconcile?.value
        let task = Task<Void, Never> { [weak self] in
            guard let self else { return }
            await self._reconcileBody(entries: entries)
        }
        activeReconcile = task
        await task.value
    }

    private func _reconcileBody(entries: [FoodLogEntry]) async {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        guard let foodCorrelationType = HKObjectType.correlationType(
            forIdentifier: .food
        ) else { return }

        let existing: [HKCorrelation]
        do {
            existing = try await fetchFiboHealthCorrelations(of: foodCorrelationType)
        } catch {
            await MainActor.run { self.lastError = "HK query failed: \(error.localizedDescription)" }
            return
        }

        // Issue 2: group fetched correlations by pi_food_id to detect duplicates.
        var grouped: [Int: [HKCorrelation]] = [:]
        for c in existing {
            guard let s = c.metadata?[Self.metaPiId] as? String,
                  let i = Int(s) else { continue }
            grouped[i, default: []].append(c)
        }

        let piIds: Set<Int> = Set(entries.map { $0.id })

        // Prune duplicates: for ids present in piIds keep first, delete rest;
        // for ids absent from piIds all copies will be handled in the delete loop.
        var toDeleteDuplicateUUIDs: Set<UUID> = []
        var toDeleteDuplicates: [HKCorrelation] = []
        for (id, copies) in grouped where copies.count > 1 {
            Self.log.warning("Found \(copies.count) duplicate correlations for pi_food_id=\(id); pruning.")
            // Keep the first copy if the id still belongs in HK; delete the rest.
            let extras = piIds.contains(id) ? Array(copies.dropFirst()) : copies
            toDeleteDuplicates.append(contentsOf: extras)
            extras.forEach { toDeleteDuplicateUUIDs.insert($0.uuid) }
        }
        for c in toDeleteDuplicates {
            do { try await store.delete([c] + Array(c.objects)) }
            catch {
                await MainActor.run {
                    self.lastError = "HK duplicate prune failed: \(error.localizedDescription)"
                }
            }
        }

        // Recompute surviving hkIds after duplicate pruning so the diff is accurate.
        let survivingHkIds: Set<Int> = Set(grouped.compactMap { id, copies -> Int? in
            let surviving = copies.filter { !toDeleteDuplicateUUIDs.contains($0.uuid) }
            return surviving.isEmpty ? nil : id
        })

        let (toInsert, toDelete) = Self.diff(piIds: piIds, hkIds: survivingHkIds)

        // Inserts — build correlation per new entry.
        let entriesById = Dictionary(uniqueKeysWithValues: entries.map { ($0.id, $0) })
        for id in toInsert {
            guard let entry = entriesById[id] else { continue }
            guard let correlation = buildCorrelation(for: entry) else {
                Self.log.warning("Skipping entry id=\(id): buildCorrelation returned nil")
                continue
            }
            do { try await store.save(correlation) }
            catch {
                await MainActor.run {
                    self.lastError = "HK save failed for id \(id): \(error.localizedDescription)"
                }
            }
        }

        // Deletes — find surviving correlations matching pi ids in toDelete.
        let toDeleteCorrelations = existing.filter {
            guard !toDeleteDuplicateUUIDs.contains($0.uuid) else { return false }
            guard let s = $0.metadata?[Self.metaPiId] as? String,
                  let i = Int(s) else { return false }
            return toDelete.contains(i)
        }
        for c in toDeleteCorrelations {
            do { try await store.delete([c] + Array(c.objects)) }
            catch {
                await MainActor.run {
                    self.lastError = "HK delete failed: \(error.localizedDescription)"
                }
            }
        }
    }

    /// Delete every food correlation this app has written. Returns counts.
    func removeAllFiboHealthEntries() async -> (removed: Int, failed: Int) {
        await MainActor.run { self.lastError = nil }
        guard HKHealthStore.isHealthDataAvailable() else { return (0, 0) }
        guard let foodCorrelationType = HKObjectType.correlationType(
            forIdentifier: .food
        ) else { return (0, 0) }

        let existing: [HKCorrelation]
        do { existing = try await fetchFiboHealthCorrelations(of: foodCorrelationType) }
        catch {
            Self.log.error("Failed to fetch entries: \(error.localizedDescription)")
            await MainActor.run {
                self.lastError = "Failed to fetch entries: \(error.localizedDescription)"
            }
            return (0, 0)
        }

        var removed = 0
        var failed = 0
        for c in existing {
            do {
                try await store.delete([c] + Array(c.objects))
                removed += 1
            } catch {
                failed += 1
                Self.log.error("HK delete failed: \(error.localizedDescription)")
                await MainActor.run {
                    self.lastError = "HK delete failed: \(error.localizedDescription)"
                }
            }
        }
        return (removed, failed)
    }

    // MARK: - Internals

    private func fetchFiboHealthCorrelations(of type: HKCorrelationType)
        async throws -> [HKCorrelation]
    {
        let predicate = HKQuery.predicateForObjects(
            withMetadataKey: Self.metaSourceApp,
            operatorType: .equalTo,
            value: Self.sourceAppValue
        )
        return try await withCheckedThrowingContinuation { cont in
            let q = HKSampleQuery(
                sampleType: type, predicate: predicate,
                limit: HKObjectQueryNoLimit, sortDescriptors: nil
            ) { _, samples, error in
                if let error = error { cont.resume(throwing: error); return }
                cont.resume(returning: (samples as? [HKCorrelation]) ?? [])
            }
            store.execute(q)
        }
    }

    /// Returns nil if the entry can't be turned into a correlation (no
    /// parseable date or no calories).
    private func buildCorrelation(for entry: FoodLogEntry) -> HKCorrelation? {
        guard let date = Self.isoFormatter.date(from: entry.timestamp) else {
            Self.log.warning("Skipping entry id=\(entry.id): unparseable timestamp '\(entry.timestamp)'")
            return nil
        }
        let metadata: [String: Any] = [
            Self.metaPiId:      String(entry.id),
            Self.metaSourceApp: Self.sourceAppValue,
            HKMetadataKeyFoodType: entry.foodName,
        ]

        // Calories required.
        guard let kcalType = HKQuantityType.quantityType(
            forIdentifier: .dietaryEnergyConsumed
        ) else { return nil }
        var members = Set<HKSample>()
        members.insert(HKQuantitySample(
            type: kcalType,
            quantity: HKQuantity(unit: .kilocalorie(), doubleValue: entry.calories),
            start: date, end: date, metadata: metadata
        ))

        // Macros — written even when 0; only nil/missing values are skipped.
        func add(_ id: HKQuantityTypeIdentifier, _ grams: Double?) {
            guard let g = grams,
                  let t = HKQuantityType.quantityType(forIdentifier: id) else { return }
            members.insert(HKQuantitySample(
                type: t,
                quantity: HKQuantity(unit: .gram(), doubleValue: g),
                start: date, end: date, metadata: metadata
            ))
        }
        add(.dietaryProtein,  entry.proteinG)
        add(.dietaryFatTotal, entry.fatG)
        add(.dietarySugar,    entry.sugarG)
        add(.dietaryFiber,    entry.fiberG)

        guard let foodCorrelationType = HKObjectType.correlationType(
            forIdentifier: .food
        ) else { return nil }
        return HKCorrelation(
            type: foodCorrelationType,
            start: date, end: date,
            objects: members, metadata: metadata
        )
    }
}

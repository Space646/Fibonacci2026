import HealthKit
import Combine
import Foundation

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

        let hkIds: Set<Int> = Set(existing.compactMap {
            ($0.metadata?[Self.metaPiId] as? String).flatMap(Int.init)
        })
        let piIds: Set<Int> = Set(entries.map { $0.id })
        let (toInsert, toDelete) = Self.diff(piIds: piIds, hkIds: hkIds)

        // Inserts — build correlation per new entry.
        let entriesById = Dictionary(uniqueKeysWithValues: entries.map { ($0.id, $0) })
        for id in toInsert {
            guard let entry = entriesById[id],
                  let correlation = buildCorrelation(for: entry) else { continue }
            do { try await store.save(correlation) }
            catch {
                await MainActor.run {
                    self.lastError = "HK save failed for id \(id): \(error.localizedDescription)"
                }
            }
        }

        // Deletes — find correlations matching pi ids in toDelete.
        let toDeleteCorrelations = existing.filter {
            guard let s = $0.metadata?[Self.metaPiId] as? String,
                  let i = Int(s) else { return false }
            return toDelete.contains(i)
        }
        for c in toDeleteCorrelations {
            do { try await store.delete(c) }
            catch {
                await MainActor.run {
                    self.lastError = "HK delete failed: \(error.localizedDescription)"
                }
            }
        }
    }

    /// Delete every food correlation this app has written. Returns counts.
    func removeAllFiboHealthEntries() async -> (removed: Int, failed: Int) {
        guard HKHealthStore.isHealthDataAvailable() else { return (0, 0) }
        guard let foodCorrelationType = HKObjectType.correlationType(
            forIdentifier: .food
        ) else { return (0, 0) }

        let existing: [HKCorrelation]
        do { existing = try await fetchFiboHealthCorrelations(of: foodCorrelationType) }
        catch { return (0, 0) }

        var removed = 0
        var failed = 0
        for c in existing {
            do { try await store.delete(c); removed += 1 }
            catch { failed += 1 }
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

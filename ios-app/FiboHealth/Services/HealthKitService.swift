import HealthKit
import Combine
import Foundation

final class HealthKitService: ObservableObject {
    @Published var snapshot: HealthSnapshot = .empty
    @Published var isAuthorized: Bool = false

    private let store = HKHealthStore()

    private let readTypes: Set<HKObjectType> = {
        var types = Set<HKObjectType>()
        let ids: [HKQuantityTypeIdentifier] = [
            .stepCount, .activeEnergyBurned, .basalEnergyBurned, .appleExerciseTime
        ]
        for id in ids {
            if let t = HKQuantityType.quantityType(forIdentifier: id) { types.insert(t) }
        }
        types.insert(HKObjectType.workoutType())
        return types
    }()

    // Nutrition write set — used for the food-logging feature. Calories +
    // four macros (carbohydrates intentionally omitted; see spec).
    static let nutritionShareTypes: Set<HKSampleType> = {
        var types = Set<HKSampleType>()
        let ids: [HKQuantityTypeIdentifier] = [
            .dietaryEnergyConsumed, .dietaryProtein, .dietaryFatTotal,
            .dietarySugar, .dietaryFiber
        ]
        for id in ids {
            if let t = HKQuantityType.quantityType(forIdentifier: id) { types.insert(t) }
        }
        return types
    }()

    func requestAuthorization() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        store.requestAuthorization(toShare: HealthKitService.nutritionShareTypes,
                                   read: readTypes) { [weak self] success, _ in
            DispatchQueue.main.async {
                self?.isAuthorized = success
                if success { self?.fetchToday() }
            }
        }
    }

    func fetchToday() {
        let group = DispatchGroup()
        var steps = 0
        var caloriesBurned = 0.0
        var activeMinutes = 0
        var workouts = 0

        let calendar = Calendar.current
        let start = calendar.startOfDay(for: Date())
        let predicate = HKQuery.predicateForSamples(withStart: start, end: Date())

        // Steps
        group.enter()
        querySum(.stepCount, unit: .count(), predicate: predicate) { value in
            steps = Int(value); group.leave()
        }

        // Active energy
        group.enter()
        querySum(.activeEnergyBurned, unit: .kilocalorie(), predicate: predicate) { value in
            caloriesBurned = value; group.leave()
        }

        // Exercise time
        group.enter()
        querySum(.appleExerciseTime, unit: .minute(), predicate: predicate) { value in
            activeMinutes = Int(value); group.leave()
        }

        // Workouts
        group.enter()
        let workoutQuery = HKSampleQuery(
            sampleType: .workoutType(),
            predicate: predicate,
            limit: HKObjectQueryNoLimit,
            sortDescriptors: nil
        ) { _, samples, _ in
            workouts = samples?.count ?? 0
            group.leave()
        }
        store.execute(workoutQuery)

        group.notify(queue: .main) { [weak self] in
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd"
            self?.snapshot = HealthSnapshot(
                date: formatter.string(from: Date()),
                steps: steps,
                caloriesBurned: caloriesBurned,
                activeMinutes: activeMinutes,
                workouts: workouts
            )
        }
    }

    private func querySum(_ id: HKQuantityTypeIdentifier, unit: HKUnit,
                          predicate: NSPredicate, completion: @escaping (Double) -> Void) {
        guard let type = HKQuantityType.quantityType(forIdentifier: id) else {
            completion(0); return
        }
        let query = HKStatisticsQuery(quantityType: type, quantitySamplePredicate: predicate,
                                       options: .cumulativeSum) { _, stats, _ in
            completion(stats?.sumQuantity()?.doubleValue(for: unit) ?? 0)
        }
        store.execute(query)
    }
}

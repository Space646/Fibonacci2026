import HealthKit
import Combine
import Foundation

final class HealthKitFoodLogger: ObservableObject {
    // Reconciliation core — pure, testable.
    static func diff(piIds: Set<Int>, hkIds: Set<Int>)
        -> (toInsert: Set<Int>, toDelete: Set<Int>)
    {
        return (toInsert: piIds.subtracting(hkIds),
                toDelete: hkIds.subtracting(piIds))
    }
}

import HealthKit
import Combine
import Foundation

final class HealthKitFoodLogger: ObservableObject {
    // Reconciliation core — pure, testable.
    static func diff(piIds: [Int], hkIds: [Int])
        -> (toInsert: [Int], toDelete: [Int])
    {
        let piSet = Set(piIds)
        let hkSet = Set(hkIds)
        return (toInsert: piSet.subtracting(hkSet).sorted(),
                toDelete: hkSet.subtracting(piSet).sorted())
    }
}

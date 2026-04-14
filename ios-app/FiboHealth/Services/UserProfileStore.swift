import SwiftData
import Combine
import Foundation

@MainActor
final class UserProfileStore: ObservableObject {
    @Published var profile: UserProfile

    private let modelContext: ModelContext

    init(modelContext: ModelContext) {
        self.modelContext = modelContext
        // Load existing profile or create a default one
        let descriptor = FetchDescriptor<UserProfile>()
        if let existing = try? modelContext.fetch(descriptor).first {
            self.profile = existing
        } else {
            let newProfile = UserProfile()
            modelContext.insert(newProfile)
            try? modelContext.save()
            self.profile = newProfile
        }
    }

    func save() {
        try? modelContext.save()
        objectWillChange.send()
    }

    func update(name: String, age: Int, weightKg: Double, heightCm: Double,
                sex: String, activityLevel: String, dailyCalorieGoal: Double?) {
        profile.name = name
        profile.age = age
        profile.weightKg = weightKg
        profile.heightCm = heightCm
        profile.sex = sex
        profile.activityLevel = activityLevel
        profile.dailyCalorieGoal = dailyCalorieGoal
        save()
    }
}

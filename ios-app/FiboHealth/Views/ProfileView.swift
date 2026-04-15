import SwiftUI

struct ProfileView: View {
    @EnvironmentObject var env: AppEnvironment

    // Fields that use number pads have no return key — we attach a
    // keyboard toolbar Done button, wired via FocusState, so the user
    // can commit a value and dismiss the keyboard.
    private enum Field: Hashable {
        case name, age, weight, height, goalOverride
    }
    @FocusState private var focusedField: Field?

    @State private var name: String = ""
    @State private var age: String = ""
    @State private var weight: String = ""
    @State private var height: String = ""
    @State private var sex: String = "other"
    @State private var activity: String = "moderate"
    @State private var goalOverride: String = ""

    private let sexOptions = ["male", "female", "other"]
    private let activityOptions = ["sedentary", "light", "moderate", "active"]

    var body: some View {
        NavigationStack {
            Form {
                Section("Identity") {
                    TextField("Name", text: $name)
                        .focused($focusedField, equals: .name)
                    TextField("Age", text: $age)
                        .keyboardType(.numberPad)
                        .focused($focusedField, equals: .age)
                    Picker("Sex", selection: $sex) {
                        ForEach(sexOptions, id: \.self) { Text($0.capitalized) }
                    }
                }
                Section("Body") {
                    TextField("Weight (kg)", text: $weight)
                        .keyboardType(.decimalPad)
                        .focused($focusedField, equals: .weight)
                    TextField("Height (cm)", text: $height)
                        .keyboardType(.decimalPad)
                        .focused($focusedField, equals: .height)
                }
                Section("Activity") {
                    Picker("Activity Level", selection: $activity) {
                        ForEach(activityOptions, id: \.self) { Text($0.capitalized) }
                    }
                }
                Section("Calorie Goal") {
                    TextField("Override (blank = calculated)", text: $goalOverride)
                        .keyboardType(.decimalPad)
                        .focused($focusedField, equals: .goalOverride)
                    Text("Calculated: \(Int(env.profileStore.profile.calculatedDailyGoal)) kcal")
                        .foregroundColor(env.theme.textMuted)
                        .font(.system(size: 12))
                }
                Section {
                    Button("Save & Sync to Pi") {
                        focusedField = nil
                        env.profileStore.update(
                            name: name, age: Int(age) ?? 25,
                            weightKg: Double(weight) ?? 70,
                            heightCm: Double(height) ?? 170,
                            sex: sex, activityLevel: activity,
                            dailyCalorieGoal: Double(goalOverride)
                        )
                        env.syncToPi()
                    }
                    .foregroundColor(Color(hex: "6366f1"))
                }
            }
            .scrollContentBackground(.hidden)
            .background(env.theme.bgPrimary.ignoresSafeArea())
            .navigationTitle("Profile")
            .toolbar {
                // Number-pad/decimal-pad keyboards have no return key, so
                // without this toolbar the user can't commit an edit.
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") { focusedField = nil }
                        .foregroundColor(Color(hex: "6366f1"))
                        .fontWeight(.semibold)
                }
            }
            .onAppear {
                let p = env.profileStore.profile
                name = p.name; age = "\(p.age)"
                weight = "\(p.weightKg)"; height = "\(p.heightCm)"
                sex = p.sex; activity = p.activityLevel
                goalOverride = p.dailyCalorieGoal.map { "\($0)" } ?? ""
            }
        }
    }
}

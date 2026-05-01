import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var proManager: ProManager
    @AppStorage("profile_name") private var name = ""
    @AppStorage("profile_age") private var age = ""
    @AppStorage("profile_weight") private var weight = ""
    @AppStorage("profile_height") private var height = ""
    @AppStorage("profile_gender") private var gender = "Not specified"
    @AppStorage("profile_goal") private var fitnessGoal = "Build Muscle"
    @AppStorage("profile_experience") private var experienceLevel = "Intermediate"
    @AppStorage("profile_weightUnit") private var weightUnit = "kg"

    let genders = ["Male", "Female", "Not specified"]
    let goals = ["Build Muscle", "Lose Weight", "Gain Strength", "Improve Endurance", "General Fitness"]
    let levels = ["Beginner", "Intermediate", "Advanced"]

    var body: some View {
        NavigationStack {
            Form {
                // Profile header
                Section {
                    HStack(spacing: 16) {
                        Image(systemName: "person.circle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.accentColor)
                        VStack(alignment: .leading) {
                            Text(name.isEmpty ? "Set up your profile" : name)
                                .font(.headline)
                            Text(proManager.isPro ? "Pro Member" : "Free Plan")
                                .font(.caption)
                                .foregroundColor(proManager.isPro ? .accentColor : .secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }

                // Profile info
                Section("Profile") {
                    TextField("Name", text: $name)
                    TextField("Age", text: $age)
                        .keyboardType(.numberPad)

                    HStack {
                        TextField("Weight", text: $weight)
                            .keyboardType(.decimalPad)
                        Text(weightUnit)
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        TextField("Height", text: $height)
                            .keyboardType(.numberPad)
                        Text("cm")
                            .foregroundColor(.secondary)
                    }

                    Picker("Gender", selection: $gender) {
                        ForEach(genders, id: \.self) { Text($0) }
                    }
                }

                // Fitness preferences
                Section("Fitness Preferences") {
                    Picker("Fitness Goal", selection: $fitnessGoal) {
                        ForEach(goals, id: \.self) { Text($0) }
                    }

                    Picker("Experience Level", selection: $experienceLevel) {
                        ForEach(levels, id: \.self) { Text($0) }
                    }

                    Picker("Weight Unit", selection: $weightUnit) {
                        Text("kg").tag("kg")
                        Text("lbs").tag("lbs")
                    }
                    .pickerStyle(.segmented)
                }

                // Subscription
                Section("Subscription") {
                    NavigationLink(destination: UpgradeView()) {
                        HStack {
                            Image(systemName: "star.fill")
                                .foregroundColor(proManager.isPro ? .accentColor : .yellow)
                            VStack(alignment: .leading) {
                                Text(proManager.isPro ? "Pro Member" : "Upgrade to Pro")
                                    .font(.subheadline.bold())
                                Text(proManager.isPro ? "All features unlocked" : "ML insights, all programs, and more")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }

                // About
                Section("About") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0").foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Data Storage")
                        Spacer()
                        Text("On-device only").foregroundColor(.secondary)
                    }
                    Text("Exercise images provided by ExerciseDB and wger Workout Manager (wger.de) under CC BY-SA license.")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Settings")
        }
    }
}

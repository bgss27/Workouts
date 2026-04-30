package com.fittrack.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fittrack.app.billing.ProManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val name: String = "",
    val age: String = "",
    val weightKg: String = "",
    val heightCm: String = "",
    val gender: String = "Not specified",
    val fitnessGoal: String = "Build Muscle",
    val experienceLevel: String = "Intermediate",
    val weightUnit: String = "kg"
)

class SettingsViewModel(
    private val context: Context,
    val proManager: ProManager
) : ViewModel() {

    companion object {
        private const val PREFS = "fittrack_profile"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<UserProfile> = _profile

    private fun loadProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString("name", "") ?: "",
            age = prefs.getString("age", "") ?: "",
            weightKg = prefs.getString("weightKg", "") ?: "",
            heightCm = prefs.getString("heightCm", "") ?: "",
            gender = prefs.getString("gender", "Not specified") ?: "Not specified",
            fitnessGoal = prefs.getString("fitnessGoal", "Build Muscle") ?: "Build Muscle",
            experienceLevel = prefs.getString("experienceLevel", "Intermediate") ?: "Intermediate",
            weightUnit = prefs.getString("weightUnit", "kg") ?: "kg"
        )
    }

    fun updateProfile(profile: UserProfile) {
        _profile.value = profile
        prefs.edit()
            .putString("name", profile.name)
            .putString("age", profile.age)
            .putString("weightKg", profile.weightKg)
            .putString("heightCm", profile.heightCm)
            .putString("gender", profile.gender)
            .putString("fitnessGoal", profile.fitnessGoal)
            .putString("experienceLevel", profile.experienceLevel)
            .putString("weightUnit", profile.weightUnit)
            .apply()
    }

    class Factory(
        private val context: Context,
        private val proManager: ProManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(context, proManager) as T
        }
    }
}

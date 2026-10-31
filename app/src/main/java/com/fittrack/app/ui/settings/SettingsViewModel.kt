package com.fittrack.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fittrack.app.billing.ProManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

data class UserProfile(
    val name: String = "",
    /** Date of birth; age is derived from it so it never goes stale. */
    val birthDate: LocalDate? = null,
    /**
     * Age typed in before birthdate existed. Shown only until a birthdate is
     * set, so upgrading users don't lose what they entered.
     */
    val legacyAge: String = "",
    val weightKg: String = "",
    val heightCm: String = "",
    val gender: String = "Not specified",
    val fitnessGoal: String = "Build Muscle",
    val experienceLevel: String = "Intermediate",
    val weightUnit: String = "kg"
) {
    /** Whole years, computed from [birthDate] as of today; falls back to [legacyAge]. */
    val age: Int?
        get() = birthDate?.let { Period.between(it, LocalDate.now()).years }
            ?: legacyAge.toIntOrNull()
}

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
            birthDate = prefs.getString("birthDate", null)?.let {
                try { LocalDate.parse(it) } catch (_: DateTimeParseException) { null }
            },
            legacyAge = prefs.getString("age", "") ?: "",
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
            .putString("birthDate", profile.birthDate?.toString())
            .putString("age", profile.legacyAge)
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

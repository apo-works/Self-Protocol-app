package jp.bodyprotocol.app

import java.time.Instant
import java.time.LocalDate

data class WeightPoint(val time: Instant, val kg: Double)
data class HealthSnapshot(
    val latestWeightKg: Double? = null,
    val latestBodyFatPct: Double? = null,
    val todaySteps: Long? = null,
    val sevenDayAverageKg: Double? = null,
    val sourceReady: Boolean = false,
    val message: String = ""
)

data class MealCheckIn(
    val craving: String,
    val leftovers: String,
    val plans: String,
    val gymDays: String
)

data class MealDay(
    val date: LocalDate,
    val breakfast: String,
    val lunch: String,
    val dinner: String,
    val snack: String,
    val proteinG: Int,
    val kcal: Int,
    val note: String = ""
)

data class MealPlan(
    val days: List<MealDay>,
    val shopping: List<String>,
    val prep: List<String>,
    val checkIn: MealCheckIn
)

data class Recipe(
    val name: String,
    val kcal: Int,
    val proteinG: Int,
    val costYen: Int,
    val ingredients: List<String>,
    val tags: Set<String>,
    val makeAhead: Boolean = true
)

data class Targets(
    val calories: Int = 2000,
    val proteinG: Int = 140,
    val goalKg: Double = 68.0,
    val mealBudgetYen: Int = 300,
    val gymPerWeek: Int = 3,
    val steps: Int = 8000
)

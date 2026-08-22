package jp.bodyprotocol.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("body_protocol", Context.MODE_PRIVATE)

    fun gymDays(): Set<LocalDate> = prefs.getStringSet("gym_days", emptySet())
        .orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()

    fun setGym(date: LocalDate, done: Boolean) {
        val values = prefs.getStringSet("gym_days", emptySet()).orEmpty().toMutableSet()
        if (done) values += date.toString() else values -= date.toString()
        prefs.edit().putStringSet("gym_days", values).apply()
    }

    fun supplementDone(date: LocalDate, key: String): Boolean =
        prefs.getBoolean("supp_${date}_$key", false)

    fun setSupplementDone(date: LocalDate, key: String, done: Boolean) {
        prefs.edit().putBoolean("supp_${date}_$key", done).apply()
    }

    fun saveManualWeight(kg: Double) {
        prefs.edit().putFloat("manual_weight", kg.toFloat()).putString("manual_weight_date", LocalDate.now().toString()).apply()
    }

    fun manualWeight(): Double? = if (prefs.contains("manual_weight")) prefs.getFloat("manual_weight", 0f).toDouble() else null

    fun targets(): Targets = Targets(
        calories = prefs.getInt("target_calories", 2000),
        proteinG = prefs.getInt("target_protein", 140),
        goalKg = prefs.getFloat("target_goal", 68f).toDouble(),
        mealBudgetYen = prefs.getInt("target_budget", 300),
        gymPerWeek = prefs.getInt("target_gym", 3),
        steps = prefs.getInt("target_steps", 8000)
    )

    fun saveTargets(t: Targets) {
        prefs.edit()
            .putInt("target_calories", t.calories)
            .putInt("target_protein", t.proteinG)
            .putFloat("target_goal", t.goalKg.toFloat())
            .putInt("target_budget", t.mealBudgetYen)
            .putInt("target_gym", t.gymPerWeek)
            .putInt("target_steps", t.steps)
            .apply()
    }

    fun savePlan(plan: MealPlan) {
        val root = JSONObject()
        root.put("craving", plan.checkIn.craving)
        root.put("leftovers", plan.checkIn.leftovers)
        root.put("plans", plan.checkIn.plans)
        root.put("gymDays", plan.checkIn.gymDays)
        val days = JSONArray()
        plan.days.forEach { d ->
            days.put(JSONObject().apply {
                put("date", d.date.toString()); put("breakfast", d.breakfast); put("lunch", d.lunch)
                put("dinner", d.dinner); put("snack", d.snack); put("protein", d.proteinG); put("kcal", d.kcal); put("note", d.note)
            })
        }
        root.put("days", days)
        root.put("shopping", JSONArray(plan.shopping))
        root.put("prep", JSONArray(plan.prep))
        prefs.edit().putString("meal_plan", root.toString()).apply()
    }

    fun loadPlan(): MealPlan? {
        val raw = prefs.getString("meal_plan", null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val check = MealCheckIn(root.optString("craving"), root.optString("leftovers"), root.optString("plans"), root.optString("gymDays"))
            val dayArray = root.getJSONArray("days")
            val days = (0 until dayArray.length()).map { i ->
                val o = dayArray.getJSONObject(i)
                MealDay(LocalDate.parse(o.getString("date")), o.getString("breakfast"), o.getString("lunch"), o.getString("dinner"), o.getString("snack"), o.getInt("protein"), o.getInt("kcal"), o.optString("note"))
            }
            fun array(name: String): List<String> { val a = root.getJSONArray(name); return (0 until a.length()).map { a.getString(it) } }
            MealPlan(days, array("shopping"), array("prep"), check)
        }.getOrNull()
    }
}

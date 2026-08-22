package jp.bodyprotocol.app

import java.time.LocalDate

object MealEngine {
    private val recipes = listOf(
        Recipe("鶏むねと玉ねぎの生姜焼き", 390, 42, 220, listOf("鶏むね肉 320g", "玉ねぎ 1個", "キャベツ 1/4玉"), setOf("和食","肉","鶏","玉ねぎ")),
        Recipe("鶏むね卵のふわふわ中華炒め", 410, 44, 240, listOf("鶏むね肉 300g", "卵 2個", "玉ねぎ 1個", "もやし 1袋"), setOf("中華","肉","鶏","卵","玉ねぎ")),
        Recipe("豚こま・豆腐・もやしの旨辛炒め", 430, 36, 280, listOf("豚こま 250g", "豆腐 300g", "もやし 1袋", "玉ねぎ 1個"), setOf("中華","肉","豚","豆腐","玉ねぎ")),
        Recipe("サバと玉ねぎの味噌蒸し", 420, 31, 290, listOf("サバ 2切れ", "玉ねぎ 1個", "きのこ 1袋"), setOf("和食","魚","サバ","玉ねぎ")),
        Recipe("ツナ卵豆腐チャンプルー", 360, 34, 240, listOf("ノンオイルツナ 2缶", "卵 2個", "豆腐 300g", "もやし 1袋"), setOf("和食","魚","ツナ","卵","豆腐")),
        Recipe("鶏むねとブロッコリーの塩だれ炒め", 370, 43, 250, listOf("鶏むね肉 320g", "冷凍ブロッコリー 250g", "玉ねぎ 1個"), setOf("さっぱり","肉","鶏","ブロッコリー","玉ねぎ")),
        Recipe("豚こまとキャベツの味噌炒め", 440, 34, 290, listOf("豚こま 250g", "キャベツ 1/3玉", "玉ねぎ 1個"), setOf("和食","肉","豚","キャベツ","玉ねぎ")),
        Recipe("鶏むね・きのこ・豆腐の和風煮", 380, 42, 260, listOf("鶏むね肉 280g", "豆腐 300g", "きのこ 1袋", "玉ねぎ 1個"), setOf("和食","さっぱり","鶏","豆腐","玉ねぎ"))
    )

    fun generate(check: MealCheckIn, targets: Targets, start: LocalDate = LocalDate.now()): MealPlan {
        val words = (check.craving + " " + check.leftovers).lowercase()
        val ranked = recipes.sortedByDescending { r ->
            r.tags.count { words.contains(it.lowercase()) } * 4 + r.ingredients.count { ing -> words.split(" ", "、", ",", "・").any { token -> token.length >= 2 && ing.contains(token) } }
        }
        val dinners = (ranked + recipes).distinctBy { it.name }.take(3)
        val breakfast = "Gold Standard Whey 1杯＋バナナ1本＋無糖ヨーグルト150g＋ミックスナッツ15g"
        val snack = "不足時のみ Gold Standard Whey 1杯（1日P140gに合わせる）"
        val days = (0..2).map { i ->
            val dinner = dinners[i]
            val lunch = if (i == 0) "作り置き：ツナ卵豆腐ボウル＋ご飯200g" else "前夜の「${dinners[i-1].name}」を1食分取り分け＋ご飯200g"
            val external = if (check.plans.isBlank()) "" else "外食予定メモ：${check.plans}"
            val gym = if (check.gymDays.isBlank()) "" else "ジム予定：${check.gymDays}。トレ前後は炭水化物を極端に抜かない。"
            MealDay(
                date = start.plusDays(i.toLong()),
                breakfast = breakfast,
                lunch = lunch,
                dinner = "${dinner.name}＋ご飯150〜200g（2食分作って翌昼へ）",
                snack = snack,
                proteinG = targets.proteinG,
                kcal = targets.calories,
                note = listOf(external, gym).filter { it.isNotBlank() }.joinToString(" / ")
            )
        }
        val shopping = buildList {
            dinners.flatMap { it.ingredients }.forEach { add(it) }
            add("バナナ 3本")
            add("無糖ヨーグルト 450g程度")
            add("ツナ缶 1〜2缶")
            add("卵 4〜6個")
            add("果物を追加するならキウイ/みかん等")
        }.distinct()
        val prep = listOf(
            "夕食は必ず2食分作り、完成直後に翌昼分を容器へ取り分ける",
            "鶏むねは買った日に1食150〜160g単位へ小分け",
            "玉ねぎ・キャベツは2〜3日分をまとめて切る",
            "1食の食材費は米代を除き¥${targets.mealBudgetYen}以内を目安に調整",
            if (check.leftovers.isBlank()) "残り物指定なし" else "残り物を先に使う：${check.leftovers}"
        )
        return MealPlan(days, shopping, prep, check)
    }
}

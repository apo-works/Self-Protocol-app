package jp.bodyprotocol.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MealReminderScheduler.schedule(this)
        setContent { BodyProtocolApp() }
    }
}

private enum class Tab(val label: String) { HOME("ホーム"), MEAL("献立"), GYM("ジム"), SUPP("サプリ"), SETTINGS("設定") }

@Composable
fun BodyProtocolApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as ComponentActivity
    val store = remember { AppStore(context) }
    val health = remember { HealthConnectManager(context) }
    var tab by remember { mutableStateOf(if (activity.intent.getBooleanExtra("open_meal", false)) Tab.MEAL else Tab.HOME) }
    var snapshot by remember { mutableStateOf(HealthSnapshot()) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var hasPermission by remember { mutableStateOf(false) }

    val healthPermissionLauncher = rememberLauncherForActivityResult(health.permissionContract) { granted ->
        hasPermission = granted.containsAll(health.permissions); refreshTick++
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(refreshTick) {
        if (health.sdkStatus() == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
            hasPermission = health.hasPermissions()
            if (hasPermission) snapshot = health.readSnapshot()
        }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        val icon = when(t){Tab.HOME->Icons.Default.Home;Tab.MEAL->Icons.Default.Restaurant;Tab.GYM->Icons.Default.FitnessCenter;Tab.SUPP->Icons.Default.Medication;Tab.SETTINGS->Icons.Default.Settings}
                        NavigationBarItem(selected = tab == t, onClick = { tab = t }, icon = { Icon(icon, null) }, label = { Text(t.label) })
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when(tab) {
                    Tab.HOME -> HomeScreen(store, snapshot, hasPermission, onGrant = { healthPermissionLauncher.launch(health.permissions) }, onRefresh = { refreshTick++ }, onGym = { tab = Tab.GYM }, onMeal = { tab = Tab.MEAL })
                    Tab.MEAL -> MealScreen(store)
                    Tab.GYM -> GymScreen(store)
                    Tab.SUPP -> SupplementScreen(store)
                    Tab.SETTINGS -> SettingsScreen(store, snapshot, hasPermission, onGrant = { healthPermissionLauncher.launch(health.permissions) }, onRefresh = { refreshTick++ })
                }
            }
        }
    }
}

@Composable private fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(20.dp, 20.dp, 20.dp, 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun MetricCard(title: String, value: String, sub: String = "") {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); if(sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall) } }
}

@Composable
private fun HomeScreen(store: AppStore, health: HealthSnapshot, hasPermission: Boolean, onGrant: () -> Unit, onRefresh: () -> Unit, onGym: () -> Unit, onMeal: () -> Unit) {
    val targets = store.targets(); val gym = store.gymDays(); val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val weekly = gym.count { !it.isBefore(monday) && !it.isAfter(today) }
    val weight = health.latestWeightKg ?: store.manualWeight()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Header("BODY PROTOCOL", "減量・筋力維持・食事・運動をひとつに") }
        item { Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { MetricCard("現在体重", weight?.let { "%.1f kg".format(it) } ?: "--", "目標 %.1f kg".format(targets.goalKg)) }
            Box(Modifier.weight(1f)) { MetricCard("今週ジム", "$weekly / ${targets.gymPerWeek}", "週3回を基本") }
        } }
        item { Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { MetricCard("7日平均", health.sevenDayAverageKg?.let { "%.1f kg".format(it) } ?: "--") }
            Box(Modifier.weight(1f)) { MetricCard("今日の歩数", health.todaySteps?.let { "%,d".format(it) } ?: "--", "目標 %,d".format(targets.steps)) }
        } }
        item { Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("今日の基本", fontWeight = FontWeight.Bold)
            Text("${targets.calories} kcal / P ${targets.proteinG}g / 8,000歩 / 睡眠7〜7.5h")
            Text("Gold Standard Whey 1〜2杯・クレアチン5g/日・VITASは手持ち分のみ")
        } } }
        if(!hasPermission) item { Button(onClick = onGrant, modifier = Modifier.padding(horizontal=20.dp).fillMaxWidth()) { Text("Samsung Healthの体重を連携") } }
        else item { OutlinedButton(onClick = onRefresh, modifier = Modifier.padding(horizontal=20.dp).fillMaxWidth()) { Text("Health Connectを更新") } }
        item { Row(Modifier.padding(horizontal=20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick=onGym, modifier=Modifier.weight(1f)) { Icon(Icons.Default.FitnessCenter,null); Spacer(Modifier.width(6.dp)); Text("ジム記録") }
            Button(onClick=onMeal, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Restaurant,null); Spacer(Modifier.width(6.dp)); Text("3日献立") }
        } }
    }
}

@Composable
private fun MealScreen(store: AppStore) {
    var craving by remember { mutableStateOf("") }; var leftovers by remember { mutableStateOf("") }; var plans by remember { mutableStateOf("") }; var gymDays by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf(store.loadPlan()) }
    val next = remember { MealReminderScheduler.nextOccurrence() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(bottom=30.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Header("3日献立", "次のチェック：${next.format(DateTimeFormatter.ofPattern("M/d HH:mm"))}") }
        item { Text("まず答える", modifier=Modifier.padding(horizontal=20.dp), fontWeight=FontWeight.Bold) }
        item { OutlinedTextField(craving,{craving=it}, label={Text("次の3日で何が食べたい？（例：中華、魚、さっぱり）")}, modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()) }
        item { OutlinedTextField(leftovers,{leftovers=it}, label={Text("残っている食材（例：玉ねぎ、豆腐、卵）")}, modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()) }
        item { OutlinedTextField(plans,{plans=it}, label={Text("外食・飲み会・予定")}, modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()) }
        item { OutlinedTextField(gymDays,{gymDays=it}, label={Text("ジム予定日（分かれば）")}, modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()) }
        item { Button(onClick={ val p=MealEngine.generate(MealCheckIn(craving,leftovers,plans,gymDays),store.targets()); store.savePlan(p); plan=p }, modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()) { Text("この条件で3日分を作る") } }
        plan?.let { p ->
            item { Text("次の3日", modifier=Modifier.padding(20.dp,12.dp,20.dp,0.dp), style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold) }
            items(p.days) { d -> Card(Modifier.padding(horizontal=20.dp).fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(d.date.format(DateTimeFormatter.ofPattern("M/d (E)")), fontWeight=FontWeight.Bold)
                Text("朝：${d.breakfast}"); Text("昼：${d.lunch}"); Text("夜：${d.dinner}"); Text("間食：${d.snack}")
                Text("目安 ${d.kcal}kcal / P${d.proteinG}g", color=MaterialTheme.colorScheme.primary)
                if(d.note.isNotBlank()) Text(d.note, style=MaterialTheme.typography.bodySmall)
            } } }
            item { Card(Modifier.padding(horizontal=20.dp).fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("買い物リスト",fontWeight=FontWeight.Bold); p.shopping.forEach{Text("• $it")} } } }
            item { Card(Modifier.padding(horizontal=20.dp).fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("作り置き",fontWeight=FontWeight.Bold); p.prep.forEach{Text("• $it")} } } }
        }
    }
}

@Composable
private fun GymScreen(store: AppStore) {
    var days by remember { mutableStateOf(store.gymDays()) }; val today=LocalDate.now(); val done=today in days
    val exercises=listOf("レッグプレス 3×8〜12","チェストプレス 3×8〜12","ラットプルダウン 3×8〜12","シーテッドレッグカール 2×10〜15","ショルダープレス 2×8〜12","アブドミナル 2×10〜15")
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(bottom=24.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Header("ジム記録", "Anytime Fitness / 週3回×約40分") }
        item { Button(onClick={store.setGym(today,!done);days=store.gymDays()}, modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()) { Icon(if(done) Icons.Default.CheckCircle else Icons.Default.FitnessCenter,null); Spacer(Modifier.width(8.dp)); Text(if(done) "今日のジム記録済み（取り消す）" else "今日ジムに行った") } }
        item { Card(Modifier.padding(horizontal=20.dp).fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("基本メニュー",fontWeight=FontWeight.Bold); exercises.forEach{Text("• $it")}; Spacer(Modifier.height(8.dp)); Text("各セットはあと1〜3回できる余力を残す。上限回数を全セット達成したら少し重量UP。",style=MaterialTheme.typography.bodySmall) } } }
        item { Text("直近21日",modifier=Modifier.padding(horizontal=20.dp),fontWeight=FontWeight.Bold) }
        items((0..20).map{today.minusDays(it.toLong())}) { d ->
            ListItem(headlineContent={Text(d.format(DateTimeFormatter.ofPattern("M/d (E)")))}, trailingContent={if(d in days) Icon(Icons.Default.CheckCircle,null,tint=MaterialTheme.colorScheme.primary)})
        }
    }
}

@Composable
private fun SupplementScreen(store: AppStore) {
    val today=LocalDate.now(); val items=listOf(
        Triple("vitas","VITAS VITA POWER","4粒（手持ち3袋を飲み切るまで）"),
        Triple("creatine","クレアチン","5g/日＝現在品は4カプセル"),
        Triple("whey1","Whey 1杯目","食事のP不足を補う"),
        Triple("whey2","Whey 2杯目","必要な日だけ。P140gに届けば不要")
    )
    var tick by remember{mutableIntStateOf(0)}
    @Suppress("UNUSED_VARIABLE") val recomposeKey = tick
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(bottom=24.dp)) {
        item { Header("サプリ", "種類を増やさず、リターンの大きいものだけ") }
        items(items) { (key,name,detail) ->
            val checked=store.supplementDone(today,key)
            ListItem(headlineContent={Text(name)},supportingContent={Text(detail)},trailingContent={Checkbox(checked,{store.setSupplementDone(today,key,it);tick++})})
        }
        item { Card(Modifier.padding(20.dp).fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("今は買わなくていい",fontWeight=FontWeight.Bold); Text("BCAA / EAA / HMB / グルタミン / カフェイン錠 / 単体亜鉛 / 単体ビタミンC") } } }
        item { Card(Modifier.padding(horizontal=20.dp).fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("条件次第",fontWeight=FontWeight.Bold); Text("魚が週2〜3回未満 → オメガ3を再評価\n日光・血液検査次第 → ビタミンDを再評価\n食事で不足 → マグネシウム/食物繊維を再評価") } } }
    }
}

@Composable
private fun SettingsScreen(store: AppStore, snapshot: HealthSnapshot, hasPermission: Boolean, onGrant: () -> Unit, onRefresh: () -> Unit) {
    var t by remember { mutableStateOf(store.targets()) }; var manual by remember { mutableStateOf(store.manualWeight()?.toString() ?: "") }; var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=24.dp)) {
        Header("設定", "目標値はいつでも変更できます")
        NumberField("1日カロリー", t.calories.toString()) { it.toIntOrNull()?.let { v->t=t.copy(calories=v) } }
        NumberField("1日タンパク質 g", t.proteinG.toString()) { it.toIntOrNull()?.let { v->t=t.copy(proteinG=v) } }
        NumberField("目標体重 kg", t.goalKg.toString()) { it.toDoubleOrNull()?.let { v->t=t.copy(goalKg=v) } }
        NumberField("1食予算（米代除く）", t.mealBudgetYen.toString()) { it.toIntOrNull()?.let { v->t=t.copy(mealBudgetYen=v) } }
        NumberField("目標歩数", t.steps.toString()) { it.toIntOrNull()?.let { v->t=t.copy(steps=v) } }
        Button(onClick={store.saveTargets(t);saved=true},modifier=Modifier.padding(20.dp,8.dp).fillMaxWidth()){Text(if(saved)"保存しました" else "目標を保存")}
        HorizontalDivider(Modifier.padding(vertical=10.dp))
        Text("Samsung Health / Health Connect",modifier=Modifier.padding(horizontal=20.dp),fontWeight=FontWeight.Bold)
        Text("体重・体脂肪率・歩数を読み取り専用で利用。アプリ外へ送信しません。",modifier=Modifier.padding(horizontal=20.dp,vertical=6.dp))
        if(hasPermission) OutlinedButton(onClick=onRefresh,modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()){Text("今すぐ同期")}
        else Button(onClick=onGrant,modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()){Text("Health Connectを許可")}
        Text(snapshot.message,modifier=Modifier.padding(horizontal=20.dp,vertical=6.dp),style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(manual,{manual=it},label={Text("手入力体重（連携できない場合）")},modifier=Modifier.padding(horizontal=20.dp,vertical=6.dp).fillMaxWidth())
        OutlinedButton(onClick={manual.toDoubleOrNull()?.let{store.saveManualWeight(it)}},modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth()){Text("手入力体重を保存")}
        HorizontalDivider(Modifier.padding(vertical=10.dp))
        Text("献立通知",modifier=Modifier.padding(horizontal=20.dp),fontWeight=FontWeight.Bold)
        Text("3日ごと18:30。通知をタップすると『何が食べたい？／何が残ってる？／外食予定／ジム予定』を入力してから3日分を生成します。",modifier=Modifier.padding(horizontal=20.dp,vertical=6.dp))
    }
}

@Composable private fun NumberField(label:String,value:String,onChange:(String)->Unit){OutlinedTextField(value,onChange,label={Text(label)},modifier=Modifier.padding(horizontal=20.dp,vertical=4.dp).fillMaxWidth(),singleLine=true)}

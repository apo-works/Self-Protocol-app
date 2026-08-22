package jp.bodyprotocol.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    val permissionContract get() = PermissionController.createRequestPermissionResultContract()

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    suspend fun hasPermissions(): Boolean {
        if (sdkStatus() != HealthConnectClient.SDK_AVAILABLE) return false
        return client().permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun readSnapshot(): HealthSnapshot {
        if (sdkStatus() != HealthConnectClient.SDK_AVAILABLE) return HealthSnapshot(message = "Health Connectが利用できません")
        if (!hasPermissions()) return HealthSnapshot(sourceReady = true, message = "Health Connectの読み取り許可が必要です")
        val hc = client()
        val now = Instant.now()
        val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS)
        val weights = hc.readRecords(ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.between(thirtyDaysAgo, now))).records
            .sortedBy { it.time }
        val latestWeight = weights.lastOrNull()?.weight?.inKilograms
        val sevenCut = now.minus(7, ChronoUnit.DAYS)
        val sevenWeights = weights.filter { it.time >= sevenCut }
        val sevenAvg = sevenWeights.takeIf { it.isNotEmpty() }?.map { it.weight.inKilograms }?.average()

        val fats = hc.readRecords(ReadRecordsRequest(BodyFatRecord::class, TimeRangeFilter.between(thirtyDaysAgo, now))).records
            .sortedBy { it.time }
        val latestFat = fats.lastOrNull()?.percentage?.value

        val zone = ZoneId.systemDefault()
        val startToday = LocalDate.now().atStartOfDay(zone).toInstant()
        val steps = hc.readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(startToday, now))).records.sumOf { it.count }

        return HealthSnapshot(latestWeight, latestFat, steps, sevenAvg, true, "Samsung Health → Health Connect")
    }
}

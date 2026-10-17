package com.example.integrations

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.audit.AuditLogger
import com.example.model.AuditCategory
import com.example.model.AuditSeverity
import com.example.permission.JarvisPermissionManager
import java.io.File

class DeviceIntegrations(
    private val context: Context,
    private val permissionManager: JarvisPermissionManager
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "jarvis_tasks_channel"
        const val CHANNEL_NAME = "JARVIS AI Alerts"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Task notifications and diagnostic alerts from JARVIS AI"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // --- Battery & Device Info ---

    data class BatteryInfo(
        val percentage: Int,
        val isCharging: Boolean,
        val chargingType: String,
        val health: String,
        val temperatureCelsius: Float,
        val isPowerSaveMode: Boolean,
        val voltageMv: Int
    )

    fun getBatteryInfo(): BatteryInfo {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val bStatus = context.registerReceiver(null, iFilter)

        val level = bStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val status = bStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = bStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargingType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Charging" else "Discharging"
        }

        val healthCode = bStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: 0
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        val temp = bStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperatureCelsius = temp / 10.0f
        val voltage = bStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isPowerSave = powerManager.isPowerSaveMode

        return BatteryInfo(
            percentage = percentage,
            isCharging = isCharging,
            chargingType = chargingType,
            health = health,
            temperatureCelsius = temperatureCelsius,
            isPowerSaveMode = isPowerSave,
            voltageMv = voltage
        )
    }

    data class DeviceSpec(
        val model: String,
        val manufacturer: String,
        val androidVersion: String,
        val sdkInt: Int,
        val board: String,
        val totalMemoryMb: Long,
        val freeStorageMb: Long,
        val totalStorageMb: Long
    )

    fun getDeviceSpec(): DeviceSpec {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        val totalStorageMb = (totalBlocks * blockSize) / (1024 * 1024)
        val freeStorageMb = (availableBlocks * blockSize) / (1024 * 1024)

        return DeviceSpec(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            board = Build.BOARD,
            totalMemoryMb = Runtime.getRuntime().totalMemory() / (1024 * 1024),
            freeStorageMb = freeStorageMb,
            totalStorageMb = totalStorageMb
        )
    }

    // --- Flashlight / Torch ---

    private var isTorchOn = false

    fun toggleTorch(enable: Boolean? = null): Result<Boolean> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return Result.failure(IllegalStateException("Camera service unavailable"))

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result.failure(IllegalStateException("No camera with flash found"))

            val targetState = enable ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState

            AuditLogger.log(
                category = AuditCategory.SYSTEM,
                severity = AuditSeverity.INFO,
                action = "Toggle Flashlight",
                details = "Flashlight turned ${if (targetState) "ON" else "OFF"}",
                outcome = "SUCCESS"
            )
            Result.success(targetState)
        } catch (e: CameraAccessException) {
            AuditLogger.log(
                category = AuditCategory.SYSTEM,
                severity = AuditSeverity.ALERT,
                action = "Toggle Flashlight Failed",
                details = e.message ?: "CameraAccessException",
                outcome = "FAILED"
            )
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isFlashlightActive(): Boolean = isTorchOn

    // --- Application Launching ---

    data class InstalledApp(
        val name: String,
        val packageName: String,
        val isSystemApp: Boolean
    )

    fun getInstalledApps(): List<InstalledApp> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return packages.mapNotNull { appInfo ->
            val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent != null) {
                InstalledApp(
                    name = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            } else null
        }.sortedBy { it.name.lowercase() }
    }

    fun launchApp(query: String): Result<String> {
        val pm = context.packageManager
        val apps = getInstalledApps()

        // Match by package name or display label
        val matched = apps.firstOrNull {
            it.packageName.equals(query, ignoreCase = true) ||
                    it.name.equals(query, ignoreCase = true) ||
                    it.name.contains(query, ignoreCase = true)
        }

        if (matched == null) {
            return Result.failure(IllegalArgumentException("No installed application matching '$query' found."))
        }

        val launchIntent = pm.getLaunchIntentForPackage(matched.packageName)
            ?: return Result.failure(IllegalStateException("Cannot launch application ${matched.name}."))

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        AuditLogger.log(
            category = AuditCategory.SYSTEM,
            severity = AuditSeverity.INFO,
            action = "Launch Application",
            details = "Launched '${matched.name}' (${matched.packageName})",
            outcome = "LAUNCHED"
        )
        return Result.success("Successfully launched ${matched.name}")
    }

    // --- Alarm / Clock Intents ---

    fun setAlarm(hour: Int, minutes: Int, message: String): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                AuditLogger.log(
                    category = AuditCategory.SYSTEM,
                    severity = AuditSeverity.INFO,
                    action = "Set Device Alarm",
                    details = "Alarm scheduled for %02d:%02d - '$message'".format(hour, minutes),
                    outcome = "INTENT_DISPATCHED"
                )
                Result.success("Alarm intent dispatched for %02d:%02d: $message".format(hour, minutes))
            } else {
                Result.failure(IllegalStateException("No clock/alarm app available on device to handle request."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Quick Camera Launch ---

    fun triggerCameraCapture(): Result<String> {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                AuditLogger.log(
                    category = AuditCategory.SYSTEM,
                    severity = AuditSeverity.INFO,
                    action = "Launch Camera",
                    details = "Dispatched MediaStore.ACTION_IMAGE_CAPTURE",
                    outcome = "SUCCESS"
                )
                Result.success("Camera capture launched.")
            } else {
                Result.failure(IllegalStateException("No camera capture application found."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Location ---

    data class LocationCoords(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
        val provider: String
    )

    fun getLastKnownLocation(): Result<LocationCoords> {
        if (!permissionManager.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !permissionManager.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            return Result.failure(SecurityException("Location permission not granted."))
        }

        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            var bestLoc: Location? = null

            for (prov in providers) {
                @Suppress("MissingPermission")
                val loc = lm.getLastKnownLocation(prov) ?: continue
                if (bestLoc == null || loc.accuracy < bestLoc.accuracy) {
                    bestLoc = loc
                }
            }

            if (bestLoc != null) {
                Result.success(
                    LocationCoords(
                        latitude = bestLoc.latitude,
                        longitude = bestLoc.longitude,
                        accuracyMeters = bestLoc.accuracy,
                        provider = bestLoc.provider ?: "system"
                    )
                )
            } else {
                Result.failure(IllegalStateException("No cached location found. Ensure GPS is enabled."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Notification Dispatch ---

    fun postNotification(title: String, message: String): Result<Boolean> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!permissionManager.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                return Result.failure(SecurityException("POST_NOTIFICATIONS permission is not granted."))
            }
        }

        return try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val notificationId = (System.currentTimeMillis() % 100000).toInt()
            notificationManager.notify(notificationId, builder.build())

            AuditLogger.log(
                category = AuditCategory.SYSTEM,
                severity = AuditSeverity.INFO,
                action = "Post Notification",
                details = "Posted notification [$title]: $message",
                outcome = "DELIVERED"
            )
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

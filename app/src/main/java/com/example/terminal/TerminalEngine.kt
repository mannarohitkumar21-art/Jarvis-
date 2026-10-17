package com.example.terminal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.example.audit.AuditLogger
import com.example.integrations.DeviceIntegrations
import com.example.model.AuditCategory
import com.example.model.AuditSeverity
import com.example.model.CommandResult
import com.example.model.CommandStatus
import com.example.model.RegisteredCommand
import com.example.permission.JarvisPermissionManager
import com.example.voice.VoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class TerminalEngine(
    private val context: Context,
    private val deviceIntegrations: DeviceIntegrations,
    private val permissionManager: JarvisPermissionManager,
    private val voiceEngine: VoiceEngine
) {
    val registeredCommands: List<RegisteredCommand> = listOf(
        RegisteredCommand("help", "help [cmd]", "List available commands or detail specific command"),
        RegisteredCommand("status", "status", "Display JARVIS system and integration telemetry"),
        RegisteredCommand("battery", "battery", "Query battery percentage, charging state, health, temp"),
        RegisteredCommand("torch", "torch [on|off|toggle]", "Control device LED flashlight", Manifest.permission.CAMERA),
        RegisteredCommand("launch", "launch <app_name>", "Search and launch installed application"),
        RegisteredCommand("alarm", "alarm <hh> <mm> [label]", "Schedule Android alarm clock event"),
        RegisteredCommand("notify", "notify <title> <message>", "Post a system notification", Manifest.permission.POST_NOTIFICATIONS),
        RegisteredCommand("location", "location", "Get device GPS coordinates", Manifest.permission.ACCESS_FINE_LOCATION),
        RegisteredCommand("storage", "storage", "Check internal storage and partition metrics"),
        RegisteredCommand("ping", "ping [host]", "Measure network connectivity latency to host"),
        RegisteredCommand("speak", "speak <text>", "Synthesize vocal response via TTS engine"),
        RegisteredCommand("termux", "termux", "Inspect Termux integration status and setup instructions"),
        RegisteredCommand("shizuku", "shizuku", "Inspect Shizuku privileged API availability"),
        RegisteredCommand("adb", "adb", "Check Android Debug Bridge wireless / USB debugging status"),
        RegisteredCommand("audit", "audit [limit]", "Inspect recent security and operation audit records"),
        RegisteredCommand("clear", "clear", "Clear terminal buffer output")
    )

    private val commandHistory = mutableListOf<String>()

    fun getHistory(): List<String> = commandHistory.toList()

    suspend fun execute(rawCommand: String): CommandResult = withContext(Dispatchers.IO) {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) {
            return@withContext CommandResult(rawCommand, 0, "")
        }

        commandHistory.add(trimmed)
        val tokens = trimmed.split("\\s+".toRegex())
        val cmdName = tokens[0].lowercase()
        val args = tokens.drop(1)

        val reg = registeredCommands.firstOrNull {
            it.name.equals(cmdName, ignoreCase = true) || it.aliases.contains(cmdName)
        }

        // Audit command entry
        AuditLogger.log(
            category = AuditCategory.TERMINAL,
            severity = AuditSeverity.INFO,
            action = "Terminal Exec: $cmdName",
            details = "Arguments: ${args.joinToString(" ")}",
            outcome = "PARSING"
        )

        // Permission Policy Verification
        if (reg?.requiredPermission != null) {
            val hasPerm = permissionManager.hasPermission(reg.requiredPermission)
            if (!hasPerm) {
                AuditLogger.log(
                    category = AuditCategory.PERMISSION,
                    severity = AuditSeverity.WARNING,
                    action = "Terminal Permission Denied",
                    details = "Command '$cmdName' requires ${reg.requiredPermission}",
                    outcome = "BLOCKED"
                )
                return@withContext CommandResult(
                    command = rawCommand,
                    returnCode = 126,
                    output = "PERMISSION DENIED: Command '$cmdName' requires Android permission:\n${reg.requiredPermission}\n\nPlease grant this permission in Diagnostics or Android Settings.",
                    error = "Permission ${reg.requiredPermission} missing.",
                    status = CommandStatus.PERMISSION_DENIED
                )
            }
        }

        return@withContext when (cmdName) {
            "help", "?" -> handleHelp(args.firstOrNull())
            "status" -> handleStatus()
            "battery" -> handleBattery()
            "torch" -> handleTorch(args.firstOrNull())
            "launch" -> handleLaunch(args.joinToString(" "))
            "alarm" -> handleAlarm(args)
            "notify" -> handleNotify(args)
            "location" -> handleLocation()
            "storage" -> handleStorage()
            "ping" -> handlePing(args.firstOrNull())
            "speak" -> handleSpeak(args.joinToString(" "))
            "termux" -> handleTermux()
            "shizuku" -> handleShizuku()
            "adb" -> handleAdb()
            "audit" -> handleAudit(args.firstOrNull()?.toIntOrNull() ?: 15)
            "clear" -> CommandResult(rawCommand, 0, "[CLEAR]")
            else -> CommandResult(
                command = rawCommand,
                returnCode = 127,
                output = "jarvis: command not found: $cmdName\nType 'help' for a list of available autonomous commands.",
                error = "Unknown command $cmdName",
                status = CommandStatus.FAILED
            )
        }
    }

    private fun handleHelp(arg: String?): CommandResult {
        if (!arg.isNullOrEmpty()) {
            val found = registeredCommands.firstOrNull { it.name.equals(arg, ignoreCase = true) }
            if (found != null) {
                val sb = StringBuilder()
                sb.append("COMMAND: ${found.name}\n")
                sb.append("USAGE:   ${found.usage}\n")
                sb.append("DETAILS: ${found.description}\n")
                if (found.requiredPermission != null) {
                    sb.append("REQUIRED PERMISSION: ${found.requiredPermission}\n")
                }
                return CommandResult("help $arg", 0, sb.toString())
            }
        }

        val sb = StringBuilder()
        sb.append("╔════════════════════════════════════════════════════════════════╗\n")
        sb.append("║                   JARVIS AUTONOMOUS TERMINAL                   ║\n")
        sb.append("╚════════════════════════════════════════════════════════════════╝\n")
        sb.append("Registered Commands (Permission-Enforced Handlers):\n\n")
        for (cmd in registeredCommands) {
            val pad = cmd.usage.padEnd(28, ' ')
            sb.append("  $pad - ${cmd.description}\n")
        }
        sb.append("\nNote: Sandboxed Android environment. Unregistered arbitrary bash execution is disabled for platform security.\n")
        return CommandResult("help", 0, sb.toString())
    }

    private fun handleStatus(): CommandResult {
        val spec = deviceIntegrations.getDeviceSpec()
        val battery = deviceIntegrations.getBatteryInfo()
        val sb = StringBuilder()
        sb.append("[JARVIS CORE TELEMETRY]\n")
        sb.append("OS Version:        Android ${spec.androidVersion} (API ${spec.sdkInt})\n")
        sb.append("Device Hardware:   ${spec.manufacturer} ${spec.model} (${spec.board})\n")
        sb.append("Battery:           ${battery.percentage}% [${battery.chargingType}, ${battery.health}, ${battery.temperatureCelsius}°C]\n")
        sb.append("Free Storage:      ${batteryInfoFormat(spec.freeStorageMb)} / ${batteryInfoFormat(spec.totalStorageMb)}\n")
        sb.append("Torch State:       ${if (deviceIntegrations.isFlashlightActive()) "ACTIVE (ON)" else "OFF"}\n")
        sb.append("TTS Voice Engine:  ${if (voiceEngine.isTtsReady.value) "ONLINE (${voiceEngine.currentLanguage.value.displayName})" else "INITIALIZING"}\n")
        sb.append("Audio Permission:  ${if (permissionManager.getAudioPermissionStatus()) "GRANTED" else "NOT_GRANTED"}\n")
        sb.append("Camera Permission: ${if (permissionManager.getCameraPermissionStatus()) "GRANTED" else "NOT_GRANTED"}\n")
        return CommandResult("status", 0, sb.toString())
    }

    private fun batteryInfoFormat(mb: Long): String {
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024.0) else "$mb MB"
    }

    private fun handleBattery(): CommandResult {
        val b = deviceIntegrations.getBatteryInfo()
        val sb = StringBuilder()
        sb.append("--- BATTERY SUBSYSTEM ---\n")
        sb.append("Charge Level:    ${b.percentage}%\n")
        sb.append("Status:          ${if (b.isCharging) "CHARGING" else "DISCHARGING"}\n")
        sb.append("Source:          ${b.chargingType}\n")
        sb.append("Health:          ${b.health}\n")
        sb.append("Temperature:     ${b.temperatureCelsius} °C\n")
        sb.append("Voltage:         ${b.voltageMv} mV\n")
        sb.append("Power Save Mode: ${if (b.isPowerSaveMode) "ENABLED" else "DISABLED"}\n")
        return CommandResult("battery", 0, sb.toString())
    }

    private fun handleTorch(mode: String?): CommandResult {
        val target = when (mode?.lowercase()) {
            "on", "1", "true" -> true
            "off", "0", "false" -> false
            else -> null // toggle
        }
        val res = deviceIntegrations.toggleTorch(target)
        return if (res.isSuccess) {
            val state = if (res.getOrNull() == true) "ON" else "OFF"
            CommandResult("torch", 0, "Hardware Flashlight toggled $state successfully.")
        } else {
            CommandResult("torch", 1, "Failed to toggle flashlight: ${res.exceptionOrNull()?.message}")
        }
    }

    private fun handleLaunch(appName: String): CommandResult {
        if (appName.isBlank()) {
            return CommandResult("launch", 1, "Usage: launch <app_name or package>")
        }
        val res = deviceIntegrations.launchApp(appName)
        return if (res.isSuccess) {
            CommandResult("launch $appName", 0, res.getOrNull() ?: "App launched.")
        } else {
            CommandResult("launch $appName", 1, "Launch failed: ${res.exceptionOrNull()?.message}")
        }
    }

    private fun handleAlarm(args: List<String>): CommandResult {
        if (args.size < 2) {
            return CommandResult("alarm", 1, "Usage: alarm <hour 0-23> <minute 0-59> [label]")
        }
        val hour = args[0].toIntOrNull()
        val min = args[1].toIntOrNull()
        if (hour == null || min == null || hour !in 0..23 || min !in 0..59) {
            return CommandResult("alarm", 1, "Error: Invalid time format. Hours must be 0-23, Minutes 0-59.")
        }
        val label = if (args.size > 2) args.drop(2).joinToString(" ") else "JARVIS Scheduled Alarm"
        val res = deviceIntegrations.setAlarm(hour, min, label)
        return if (res.isSuccess) {
            CommandResult("alarm", 0, res.getOrNull() ?: "Alarm set.")
        } else {
            CommandResult("alarm", 1, "Alarm error: ${res.exceptionOrNull()?.message}")
        }
    }

    private fun handleNotify(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult("notify", 1, "Usage: notify <title> <message>")
        }
        val title = args[0]
        val message = if (args.size > 1) args.drop(1).joinToString(" ") else "Notification from JARVIS"
        val res = deviceIntegrations.postNotification(title, message)
        return if (res.isSuccess) {
            CommandResult("notify", 0, "Notification successfully posted.")
        } else {
            CommandResult("notify", 1, "Failed to post notification: ${res.exceptionOrNull()?.message}")
        }
    }

    private fun handleLocation(): CommandResult {
        val res = deviceIntegrations.getLastKnownLocation()
        return if (res.isSuccess) {
            val loc = res.getOrNull()!!
            val out = "Latitude:  ${loc.latitude}\nLongitude: ${loc.longitude}\nAccuracy:  ±${loc.accuracyMeters}m\nProvider:  ${loc.provider}"
            CommandResult("location", 0, out)
        } else {
            CommandResult("location", 1, "Location error: ${res.exceptionOrNull()?.message}")
        }
    }

    private fun handleStorage(): CommandResult {
        val spec = deviceIntegrations.getDeviceSpec()
        val sb = StringBuilder()
        sb.append("--- INTERNAL STORAGE PARTITION ---\n")
        sb.append("Total Space:     ${batteryInfoFormat(spec.totalStorageMb)}\n")
        sb.append("Available Space: ${batteryInfoFormat(spec.freeStorageMb)}\n")
        val used = spec.totalStorageMb - spec.freeStorageMb
        val pct = if (spec.totalStorageMb > 0) (used * 100) / spec.totalStorageMb else 0
        sb.append("Used:            ${batteryInfoFormat(used)} ($pct%)\n")
        return CommandResult("storage", 0, sb.toString())
    }

    private fun handlePing(host: String?): CommandResult {
        val targetHost = host?.trim()?.ifEmpty { "8.8.8.8" } ?: "8.8.8.8"
        return try {
            val start = System.currentTimeMillis()
            val addr = InetAddress.getByName(targetHost)
            val reachable = addr.isReachable(3000)
            val elapsed = System.currentTimeMillis() - start
            if (reachable) {
                CommandResult("ping $targetHost", 0, "64 bytes from ${addr.hostAddress}: icmp_seq=1 time=${elapsed}ms (OK)")
            } else {
                CommandResult("ping $targetHost", 1, "Host $targetHost is unreachable (timed out after 3000ms).")
            }
        } catch (e: Exception) {
            CommandResult("ping $targetHost", 1, "Ping failed: ${e.message}")
        }
    }

    private fun handleSpeak(text: String): CommandResult {
        if (text.isBlank()) return CommandResult("speak", 1, "Usage: speak <text to say>")
        voiceEngine.speak(text)
        return CommandResult("speak", 0, "Spoken via voice engine in ${voiceEngine.currentLanguage.value.displayName}: \"$text\"")
    }

    private fun handleTermux(): CommandResult {
        val pm = context.packageManager
        val isInstalled = try {
            pm.getPackageInfo("com.termux", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        val sb = StringBuilder()
        sb.append("--- TERMUX BRIDGE STATUS ---\n")
        sb.append("Installed: ${if (isInstalled) "YES (Package: com.termux)" else "NOT DETECTED"}\n")
        if (isInstalled) {
            sb.append("Status: Termux environment detected. Integration requires Termux:Tasker or Termux:API addon to securely receive explicit intents.\n")
        } else {
            sb.append("Setup Guide: To connect Termux with JARVIS AI:\n")
            sb.append("1. Install Termux from F-Droid (preferred) or official release.\n")
            sb.append("2. Install 'termux-api' inside Termux (`pkg install termux-api`).\n")
            sb.append("3. JARVIS will safely exchange structured commands via Intent filters.\n")
        }
        return CommandResult("termux", 0, sb.toString())
    }

    private fun handleShizuku(): CommandResult {
        val pm = context.packageManager
        val isInstalled = try {
            pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        val sb = StringBuilder()
        sb.append("--- SHIZUKU PRIVILEGED API STATUS ---\n")
        sb.append("Installed: ${if (isInstalled) "YES (moe.shizuku.privileged.api)" else "NOT DETECTED"}\n")
        if (isInstalled) {
            sb.append("Status: Shizuku is installed. Elevated system actions must be granted via the Shizuku Manager app.\n")
        } else {
            sb.append("Setup Guide:\n")
            sb.append("1. Download Shizuku from GitHub or Google Play Store.\n")
            sb.append("2. Start Shizuku service via Wireless Debugging (Android 11+) or ADB.\n")
            sb.append("3. Authorize JARVIS AI in Shizuku's authorized apps list for system operations.\n")
        }
        return CommandResult("shizuku", 0, sb.toString())
    }

    private fun handleAdb(): CommandResult {
        val isDeveloper = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
        val isAdbEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0

        val sb = StringBuilder()
        sb.append("--- ANDROID DEBUG BRIDGE (ADB) STATUS ---\n")
        sb.append("Developer Options: ${if (isDeveloper) "ENABLED" else "DISABLED"}\n")
        sb.append("USB Debugging:     ${if (isAdbEnabled) "ENABLED" else "DISABLED"}\n")
        sb.append("Android Release:   Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        sb.append("\nPairing Instructions:\n")
        sb.append("1. Go to Settings > System > Developer Options.\n")
        sb.append("2. Enable 'Wireless Debugging'.\n")
        sb.append("3. Tap 'Pair device with pairing code' to view port and 6-digit key.\n")
        return CommandResult("adb", 0, sb.toString())
    }

    private fun handleAudit(limit: Int): CommandResult {
        val logs = AuditLogger.getRecent(limit)
        val sb = StringBuilder()
        sb.append("--- RECENT SECURITY & SYSTEM AUDIT LOGS (${logs.size}) ---\n")
        for (log in logs) {
            sb.append("[${log.formattedTime}] [${log.severity}] [${log.category}] ${log.action} -> ${log.outcome}\n")
            sb.append("  Details: ${log.details}\n")
        }
        return CommandResult("audit", 0, sb.toString())
    }
}

package com.example.model

enum class CommandStatus {
    SUCCESS,
    FAILED,
    IN_PROGRESS,
    PERMISSION_DENIED,
    CONFIRMATION_REQUIRED
}

data class CommandResult(
    val command: String,
    val returnCode: Int,
    val output: String,
    val error: String? = null,
    val status: CommandStatus = if (returnCode == 0) CommandStatus.SUCCESS else CommandStatus.FAILED,
    val timestamp: Long = System.currentTimeMillis()
)

data class RegisteredCommand(
    val name: String,
    val usage: String,
    val description: String,
    val requiredPermission: String? = null,
    val isDestructive: Boolean = false,
    val aliases: List<String> = emptyList()
)

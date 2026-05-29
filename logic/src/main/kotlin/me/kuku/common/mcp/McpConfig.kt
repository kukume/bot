package me.kuku.common.mcp

data class McpConfig(
    val servers: List<McpServerConfig> = emptyList()
)

data class McpServerConfig(
    val name: String,
    val type: String = "stdio",
    val command: List<String>? = null,
    val url: String? = null
)

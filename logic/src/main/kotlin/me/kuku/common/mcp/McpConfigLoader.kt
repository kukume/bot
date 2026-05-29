package me.kuku.common.mcp

import com.fasterxml.jackson.databind.ObjectMapper

object McpConfigLoader {

    private val objectMapper = ObjectMapper()

    fun load(): McpConfig? {
        val stream = javaClass.classLoader.getResourceAsStream("mcp.json")
            ?: return null
        return stream.use { objectMapper.readValue(it, McpConfig::class.java) }
    }
}

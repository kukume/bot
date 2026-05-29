package me.kuku.common.mcp

import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.ClientCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.SSE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

class McpClientManager {

    private val clients = mutableMapOf<String, Client>()

    suspend fun connectStdio(name: String, command: List<String>) {
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder(command).start()
        }
        val transport = StdioClientTransport(
            input = process.inputStream.asSource().buffered(),
            output = process.outputStream.asSink().buffered()
        )
        val client = Client(
            clientInfo = Implementation(name = "kuku-bot-mcp", version = "1.0.0"),
            options = ClientOptions(
                capabilities = ClientCapabilities()
            )
        )
        client.connect(transport)
        clients[name] = client
    }

    suspend fun connectSse(name: String, url: String) {
        val httpClient = HttpClient { install(SSE) }
        val transport = StreamableHttpClientTransport(client = httpClient, url = url)
        val client = Client(
            clientInfo = Implementation(name = "kuku-bot-mcp", version = "1.0.0"),
            options = ClientOptions(
                capabilities = ClientCapabilities()
            )
        )
        client.connect(transport)
        clients[name] = client
    }

    suspend fun listAllTools(): List<Tool> {
        return clients.flatMap { (_, client) ->
            client.listTools().tools
        }
    }

    suspend fun callTool(name: String, arguments: Map<String, Any?>): io.modelcontextprotocol.kotlin.sdk.types.CallToolResult {
        for ((_, client) in clients) {
            val tools = client.listTools().tools
            if (tools.any { it.name == name }) {
                return client.callTool(name = name, arguments = arguments)
            }
        }
        error("Tool '$name' not found in any MCP server")
    }

    suspend fun connectFromConfig(config: McpConfig) {
        for (server in config.servers) {
            when (server.type.lowercase()) {
                "stdio" -> server.command?.let { connectStdio(server.name, it) }
                "sse" -> server.url?.let { connectSse(server.name, it) }
            }
        }
    }

    fun isEmpty(): Boolean = clients.isEmpty()

    fun isNotEmpty(): Boolean = clients.isNotEmpty()

    suspend fun closeAll() {
        clients.values.forEach { it.close() }
        clients.clear()
    }
}

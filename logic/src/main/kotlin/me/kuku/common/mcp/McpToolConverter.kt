package me.kuku.common.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.openai.core.JsonValue
import com.openai.models.FunctionDefinition
import com.openai.models.FunctionParameters
import com.openai.models.chat.completions.ChatCompletionFunctionTool
import com.openai.models.chat.completions.ChatCompletionTool
import io.modelcontextprotocol.kotlin.sdk.types.Tool

object McpToolConverter {

    private val objectMapper = ObjectMapper()

    fun toOpenAiTools(tools: List<Tool>): List<ChatCompletionTool> {
        return tools.map { tool ->
            ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool.builder()
                    .function(
                        FunctionDefinition.builder()
                            .name(tool.name)
                            .description(tool.description ?: "")
                            .parameters(toolSchemaToFunctionParameters(tool.inputSchema))
                            .build()
                    )
                    .build()
            )
        }
    }

    private fun toolSchemaToFunctionParameters(schema: io.modelcontextprotocol.kotlin.sdk.types.ToolSchema): FunctionParameters {
        val schemaNode = objectMapper.createObjectNode().apply {
            put("type", "object")
            set<JsonNode>("properties", objectMapper.readTree(schema.properties.toString()))
            set<JsonNode>("required", objectMapper.valueToTree(schema.required))
        }
        val map = mutableMapOf<String, JsonValue>()
        schemaNode.properties().forEach { (k, v) ->
            map[k] = JsonValue.fromJsonNode(v)
        }
        return FunctionParameters.builder()
            .putAllAdditionalProperties(map)
            .build()
    }
}

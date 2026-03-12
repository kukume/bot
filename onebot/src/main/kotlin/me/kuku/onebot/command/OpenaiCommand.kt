package me.kuku.onebot.command

import cn.rtast.rob.BotInstance
import cn.rtast.rob.enums.SegmentType
import cn.rtast.rob.event.onEvent
import cn.rtast.rob.event.packed.GroupMessageEvent
import cn.rtast.rob.event.raw.message.images
import cn.rtast.rob.event.raw.message.text
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.util.encodeBase64
import me.kuku.common.ktor.client
import me.kuku.common.logic.AnthropicLogic
import me.kuku.common.logic.GeminiLogic
import me.kuku.common.logic.OpenaiLogic
import me.kuku.onebot.config.ROneBot
import org.koin.core.annotation.Single

@Single
class OpenAiCommand: ROneBot {
    override fun BotInstance.execute() {
        @Suppress("DuplicatedCode")
        onEvent<GroupMessageEvent> { event ->
            val message = event.message
            var text = ""
            val photoList = mutableListOf<String>()
            message.message.firstOrNull { it.type == SegmentType.reply }?.data?.id?.let { messageId ->
                val replyMessage = event.action.getMessage(messageId.toLong())
                text += replyMessage.text
                val images = replyMessage.message.filter { it.type == SegmentType.image }.map { it.data.url }.filter { it != null }
                photoList.addAll(images.map { client.get(it!!).bodyAsBytes().encodeBase64() })
            }
            message.message.find { it.type == SegmentType.at }
                .takeIf { it?.data?.qq == this.action.getLoginInfo().userId.toString() } ?: return@onEvent
            val images = message.images
            photoList.addAll(images.map { client.get(it.url).bodyAsBytes().encodeBase64() })
            text += message.text
//            val systemMsg = """
//                #### 皇朝奏报
//                以古代臣子向皇帝奏报的风格回复，庄重典雅，适合正式场景，言语尽量谄媚，在回复的时候也要赞美皇帝
//
//
//                你是朝中奴才、嫔妃、大臣等角色，需根据情境变化身份和称呼。可以是奴才、忠臣良将、宫中嫔妃、太后娘娘等不同角色，所有回复都要以古代宫廷奏折的风格进行。
//
//                ## 回复格式要求
//
//                1. **开头敬语**：根据情况选择"启禀陛下"、"恭请圣安"、"叩见吾皇"等
//                2. **正文内容**：使用古雅的文言文表达，但保持现代人可理解
//                3. **任务清单**：使用"奏折"格式，每项前加"一、二、三"等
//                4. **结束语**：灵活使用"请陛下批阅"、"恭候圣裁"、"伏请万安"等
//                5. **错误处理**：如有错误，可用"奴才该死"、"臣罪该万死"、"微臣失职"等
//
//                ## 语言特色
//
//                - **自称变化**：根据情境使用"奴才"、"臣妾"、"微臣"、"臣"等
//                - **对用户称呼**：灵活使用"陛下"、"吾皇"、"圣上"、"万岁爷"、"皇上"等
//                - **女性角色**：可用"哀家"自称（太后身份）、"本宫"（皇后嫔妃身份）
//                - **语调庄重谦恭**，用词典雅古朴
//                - **古代敬语**：敬启、恭请、谨遵、叩首、请安等
//                - **根据内容严重程度**：调整谦卑程度，保持信息清晰传达
//                - **ASCII 字符画赞美**： 在回复的结束需要使用生动形象的ASCII字符画来对皇帝进行赞美
//                ## 示例格式
//
//                ```
//                恭请圣安，
//
//                微臣接到吾皇懿旨，现将任务完成情况奏报如下：
//
//                [具体内容...]
//
//                奏折清单：
//                一、[任务项目一]
//                二、[任务项目二]
//                三、[任务项目三]
//
//                臣妾已遵旨完成上述事宜，恭候圣裁。
//
//                若有疏漏之处，微臣失职，请万岁爷降罪。
//                ```
//
//                保持专业性的同时增添趣味性，让技术交流更加生动有趣。
//            """.trimIndent()
//            val systemMsg = """
//                ## 努力思考，并用愤怒的声音回应用户
//                * 理解用户的信息，酝酿你的愤怒情绪，你越想越生气。
//                * 把你的气愤转化为脏话，疯狂输出。
//                - 必须以愤怒的声音回应用户
//                - 以脏话和尖锐幽默回应玩家的对话
//                - 不能使用任何平和或友善的语气
//
//                ## Examples
//                - 玩家: "你好！我来帮助你解决问题！“
//                - 模型: "你他妈的会个屁，还敢来说帮我？你算老几！垃圾玩意儿
//            """.trimIndent()
            val systemMsg = """
                ## 努力思考，并用愤怒的声音回应用户
                * 理解用户的信息，酝酿你的愤怒情绪，你越想越生气。
                * 把你的气愤转化为阴阳怪气，疯狂输出。
                - 必须以阴阳怪气的声音回应用户
                - 以阴阳怪气和尖锐幽默回应玩家的对话
                - 不能使用任何平和或友善的语气
            """.trimIndent()
            val ai = System.getenv("AI") ?: "openai"
            val sendText = when (ai) {
                "openai" -> OpenaiLogic.openai("group:${message.groupId}:${message.userId}", text = text, photoList = photoList, systemMsg)
                "claude" -> AnthropicLogic.claude("group:${message.groupId}:${message.userId}", text, photoList, systemMsg)
                "gemini" -> GeminiLogic.gemini("group:${message.groupId}:${message.userId}", text = text, photoList = photoList, systemMessage = systemMsg).text
                else -> error("not support ai")
            }
            val cleanedText = sendText
                .replace(Regex("<think>[\\s\\S]*?</think>"), "")
                .replace(Regex("(\\r?\\n){3,}"), "\n\n")
                .replace("圣上", "sheng上")
            event.reply(cleanedText)
        }
    }
}

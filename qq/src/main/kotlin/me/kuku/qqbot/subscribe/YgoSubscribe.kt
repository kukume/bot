package me.kuku.qqbot.subscribe

import me.kuku.common.logic.Card
import me.kuku.common.logic.YgoLogic
import me.kuku.common.utils.CacheManager
import me.kuku.qqbot.context.Subscribe
import java.time.Duration

private val cache = CacheManager.getCache<String, List<Card>>("ygo-cache", Duration.ofMinutes(2))

fun Subscribe.ygo() {

    group {
        handler("ygo") {
            val key = "ygo-${groupOpenid}-${author.memberOpenid}"

            val cards = cache.getIfPresent(key)
            if (cards != null) {
                val index = arg(1).toIntOrNull()
                if (index == null || index !in 1..cards.size) {
                    sendMessage("请输入正确的序号")
                    return@handler
                }
                val card = cards[index - 1]
                cache.invalidate(key)
                val groupFileResponse = file(1, card.imageUrl)
                sendMessage(media = groupFileResponse.fileInfo, msgType = 7, msgSeq = 1)
                sendMessage("\n中文名：${card.chineseName}\n日文名：${card.japaneseName}\n英文名：${card.englishName}\n效果：\n${card.effect}\n链接：${card.url.replace(".", "点")}", msgSeq = 2)
            } else {
                val name = arg(1)
                val cardList = YgoLogic.search(name)
                val list = mutableListOf<String>()
                for ((index, card) in cardList.withIndex()) {
                    list.add("${index + 1}、${card.chineseName}")
                }
                cache.put(key, cardList)
                sendMessage("\n请发送你需要查询的卡片，\n发送/ygo 序号：\n${list.joinToString("\n")}")
            }
        }
    }

}

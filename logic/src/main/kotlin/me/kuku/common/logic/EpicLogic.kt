package me.kuku.common.logic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.kuku.common.ktor.client
import me.kuku.common.ktor.cookieString
import me.kuku.common.ktor.referer
import me.kuku.common.utils.toJsonNode
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object EpicLogic {

    suspend fun epic(): List<EpicFreeGame> {
        val jsonNode = client.get("https://store-site-backend-static-ipv4.ak.epicgames.com/freeGamesPromotions?locale=zh-CN&country=US&allowCountries=US")
            .body<JsonNode>()
        val elements = jsonNode["data"]["Catalog"]["searchStore"]["elements"].filter { it["status"].asText() == "ACTIVE" }
        val list = mutableListOf<EpicFreeGame>()
        for (element in elements) {
            val promotion = element["promotions"]?.get("promotionalOffers")?.get(0)?.get("promotionalOffers")?.get(0)
                ?: element["promotions"]?.get("upcomingPromotionalOffers")?.get(0)?.get("promotionalOffers")?.get(0)  ?: continue
            val startDate = promotion["startDate"].asText().replace(".000Z", "")
            val startTimeStamp = LocalDateTime.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).toInstant(
                ZoneOffset.ofHours(0)).toEpochMilli()
            val nowTimeStamp = System.currentTimeMillis()
            val diff = nowTimeStamp - startTimeStamp
            if (diff > 0) {
                val title = element["title"].asText()
                val imageUrl = element["keyImages"][0]["url"].asText()
                val slug = element["productSlug"].takeIf { it !is NullNode }?.asText() ?: element["catalogNs"]["mappings"][0]["pageSlug"].asText()
                val html = client.get("https://store.epicgames.com/zh-CN/p/$slug") {
                    userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                    cookieString("EPIC_LOCALE_COOKIE=zh-CN; _epicSID=c9a415ce4e6c4f1fa3da0cb465a1679f;")
                    referer("https://store.epicgames.com/zh-CN/")
                }.bodyAsText()
                val queryJsonNode =
                    "window\\.__REACT_QUERY_INITIAL_QUERIES__\\s*=\\s*(\\{.*});".toRegex().find(html)?.value?.substring(41)?.dropLast(1)?.toJsonNode() ?: continue
                val queries = queryJsonNode["queries"]
                val mappings = queries.filter { it["queryKey"]?.get(0)?.asText() == "getCatalogOffer" }
                for (mapping in mappings) {
                    val catalogOffer = mapping["state"]["data"]?.get("Catalog")?.get("catalogOffer") ?: continue
                    val fmtPrice = catalogOffer["price"]["totalPrice"]["fmtPrice"]
                    val originalPrice = fmtPrice["originalPrice"].asText()
                    val discountPrice = fmtPrice["discountPrice"].asText()
                    if (discountPrice != "0") continue
                    val namespace = catalogOffer["namespace"].asText()
                    val id = catalogOffer["id"].asText()
                    val innerTitle = catalogOffer["title"].asText()
                    val description = catalogOffer["description"].asText()
                    val longDescription = catalogOffer["longDescription"].asText()
                    val url = "https://store.epicgames.com/purchase?highlightColor=0078f2&offers=1-$namespace-$id&showNavigation=true#/purchase/payment-methods"
                    val epicFreeGame = EpicFreeGame(title, innerTitle, description, longDescription, originalPrice, discountPrice, url, imageUrl, diff)
                    list.add(epicFreeGame)
                }
            }
        }
        return list
    }
}

data class EpicFreeGame(
    val title: String,
    val innerTitle: String,
    val description: String,
    val longDescription: String,
    val originalPrice: String,
    val discountPrice: String,
    val url: String,
    val imageUrl: String,
    val diff: Long
) {
    fun text() = "#Epic免费游戏推送\n游戏名称: $title\n游戏内部名称: $innerTitle\n游戏描述: $description\n原价: $originalPrice\n折扣价: $discountPrice\n订单地址：${url}"
}
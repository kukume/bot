package me.kuku.common.logic

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import me.kuku.common.ktor.client
import me.kuku.common.utils.toUrlEncode
import org.jsoup.Jsoup

object MusicLogic {

    suspend fun search(name: String): List<Song> {
        val html = client.get("https://music.kukuqaq.com/search?type=song&q=${name.toUrlEncode()}&sources=netease&sources=qq&sources=kugou&sources=kuwo&sources=migu&sources=qianqian&sources=soda").bodyAsText()
        val document = Jsoup.parse(html)
        val elements = document.select(".result-list li")
        val list = mutableListOf<Song>()
        for (item in elements) {
            val name = item.attr("data-name")
            val artist = item.attr("data-artist")
            val cover = item.attr("data-cover")
            val source = item.attr("data-source")
            val ass = item.select(".actions a")
            val url = "https://music.kukuqaq.com" + (ass.first()?.attr("href") ?: continue)
            list.add(Song(name, artist, cover, source, url))
        }
        return list
    }


}

data class Song(
    val name: String,
    val artist: String,
    val cover: String,
    val source: String,
    val url: String
)
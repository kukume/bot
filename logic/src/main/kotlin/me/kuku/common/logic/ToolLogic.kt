package me.kuku.common.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.StringValuesBuilder
import kotlinx.coroutines.runBlocking
import me.kuku.common.ktor.client
import me.kuku.common.ktor.referer
import me.kuku.common.ktor.setJsonBody
import me.kuku.common.utils.*
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.JPEGTranscoder
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLDecoder
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.hours

object ToolLogic {

    private val fishCache = CacheManager.getCache<String, String>("fishing", 1.hours)
    private const val FISH_KEY = "fishing"

    suspend fun fishing(): String {
        val cacheUrl = fishCache[FISH_KEY]
        if (cacheUrl != null) return cacheUrl
        val jsonNode = client.get("https://mp.weixin.qq.com/mp/appmsgalbum?action=getalbum&__biz=MzAxOTYyMzczNA%3D%3D&album_id=3743225907507462153&count=10&begin_msgid&begin_itemidx&uin&key&pass_ticket&wxtoken&devicetype&clientversion&__biz=MzAxOTYyMzczNA%3D%3D&appmsg_token&x5=0&f=json")
            .body<JsonNode>()
        val article = jsonNode["getalbum_resp"]["article_list"][0]
        val url = article["url"].asText()
        val response = client.get(url)
        val html = if (response.status == HttpStatusCode.MovedPermanently) {
            val location = response.headers["Location"]!!
            client.get(location) {
                referer(url)
                userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
            }.bodyAsText()
        } else response.bodyAsText()
        val document = Jsoup.parse(html)
        val list = document.getElementsByTag("img")
        val filterElement = mutableListOf<Element>()
        for (element in list) {
            if (element.hasAttr("data-src")) filterElement.add(element)
        }
        val imageUrl = filterElement.find { it.attr("data-w") == "540" }?.attr("data-src") ?: error("not found fishing image")
        fishCache[FISH_KEY] = imageUrl
        return imageUrl
    }

    private val rapidKey by lazy {
        System.getenv("RAPID_KEY")
    }

    suspend fun bin(bin: String, key: String = rapidKey): Bin {
        val jsonNode = client.post("https://bin-ip-checker.p.rapidapi.com/?bin=$bin") {
            setJsonBody("""{"bin":"$bin"}""")
            headers {
                append("x-rapidapi-key", key)
                append("x-rapidapi-host", "bin-ip-checker.p.rapidapi.com")
            }
        }.body<JsonNode>()
        if (jsonNode["code"]?.asInt() == 200) {
            val binNode = jsonNode["BIN"]
            val countryNode = binNode["country"]
            return Bin(binNode["number"].asText(),
                binNode["scheme"].asText(),
                binNode["type"].asText(),
                binNode["level"].asText(),
                countryNode["name"].asText(),
                countryNode["currency"].asText(),
                binNode["issuer"]["name"].asText()
            )
        } else {
            error(jsonNode["message"].asText())
        }
    }

    suspend fun dy(param: String): File {
        val urlArg = "https://v.douyin.com/[A-Za-z0-9_-]*/".toRegex().find(param)?.groupValues?.get(0) ?: error("not found douyin url")
        val locationResponse = client.get(urlArg) {
            userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
        }
        val htmlUrl = locationResponse.headers["Location"] ?: error("获取抖音视频失败")
        val html = client.get(htmlUrl) {
            userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
        }.bodyAsText()
        val id = RegexUtils.extract(html, "video_id=", "\"") ?: error("获取抖音视频失败")
        val response = client.get("https://m.douyin.com/aweme/v1/playwm/?video_id=$id&ratio=720p&line=0")
        val url = response.headers["Location"] ?: error("获取抖音视频失败")
        val videoFile = File("tmp${File.separator}${UUID.randomUUID()}.mp4")
        // 还有一层302
        segmentsDownload(videoFile, url)
        return videoFile
    }

    private fun StringValuesBuilder.currencyAppendCommon() {
        append("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
        append("accept-language", "zh-CN,zh;q=0.9,en;q=0.8")
        append("cache-control", "no-cache")
        append("pragma", "no-cache")
        append("priority", "u=0, i")
        append("upgrade-insecure-requests", "1")
        append("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
    }

    private val currency by lazy {
        runBlocking {
            val jsonNode =
                client.get("https://www.mastercard.com/settlement/currencyrate/settlement-currencies") {
                    headers {
                        currencyAppendCommon()
                    }
                }.body<JsonNode>()
            jsonNode["data"]["currencies"].associate {
                val alphaCd = it["alphaCd"].asText()
                val currNam = it["currNam"].asText()
                alphaCd to Currency(alphaCd, currNam)
            }
        }
    }

    private data class Currency(val alphaCd: String, val currNam: String)

    suspend fun rate(from: String, to: String, amt: Double): Rate {
        currency[from] ?: error("incorrect currency: $from")
        currency[to] ?: error("incorrect currency: $to")
        val jsonNode = client.get("https://www.mastercard.com/marketingservices/public/mccom-services/currency-conversions/conversion-rates?exchange_date=0000-00-00&transaction_currency=$from&cardholder_billing_currency=$to&bank_fee=0&transaction_amount=$amt") {
            headers {
                currencyAppendCommon()
            }
        }.body<JsonNode>()
        val data = jsonNode["data"]
        val transCurr = data["transCurr"].asText()
        val conversionRate = data["conversionRate"].asText()
        val crdhldBillCurr = data["crdhldBillCurr"].asText()
        val transAmt = data["transAmt"].asText()
        val crdhldBillAmt = data["crdhldBillAmt"].asText()
        return Rate(transCurr, conversionRate, crdhldBillCurr, transAmt, crdhldBillAmt)
    }

    private val currencyVisa by lazy {
        runBlocking {
            val html = client.get("https://www.visa.com.hk/zh_HK/support/consumer/travel-support/exchange-rate-calculator.html").bodyAsText()
            val extract = RegexUtils.extract(html, "<dm-calculator content='", "'></dm-calculator>") ?: error("unsuccess get currency")
            val json = Parser.unescapeEntities(extract, false)
            json.toJsonNode()["currencyList"].associate {
                val key = it["key"].asText()
                val value = it["value"].asText()
                key to Currency(key, value)
            }
        }
    }

    suspend fun rateVisa(from: String, to: String, amt: Double): Rate {
        currencyVisa[from] ?: error("incorrect currency: $from")
        currencyVisa[to] ?: error("incorrect currency: $to")
        val now = LocalDateTime.now()
        val year = now.year
        val month = now.monthValue.toString().padStart(2, '0')
        val day = now.dayOfMonth.toString().padStart(2, '0')
        val jsonNode = client.get("https://www.visa.com.hk/cmsapi/fx/rates?amount=$amt&fee=0&utcConvertedDate=$month%2F$day%2F$year&exchangedate=$month%2F$day%2F$year&fromCurr=$to&toCurr=$from").body<JsonNode>()
        val data = jsonNode["originalValues"]
        val fromCurrency = data["fromCurrency"].asText()
        val toCurrency = data["toCurrency"].asText()
        val fxRate = data["fxRateWithAdditionalFee"].asText()
        val fromAmount = data["fromAmount"].asText()
        val toAmount = data["toAmountWithAdditionalFee"].asText()
        return Rate(fromCurrency, fxRate, toCurrency, fromAmount, toAmount)
    }

    suspend fun renderMarkdown1(text: String): ByteArray {
        return client.submitForm("https://oiapi.net/API/MarkdownToImage", formParameters = parameters {
            append("content", text)
        }).bodyAsBytes()
    }

    suspend fun renderMarkdown2(text: String): ByteArray {
        val jsonNode =  client.post("https://markdown-to-image-serve.jcommon.top/api/generatePosterImage") {
            setJsonBody("""
            {
                "markdown": "$text"
            }
        """.trimIndent())
        }.body<JsonNode>()
        val url = jsonNode["url"].asText()
        return client.get(url).bodyAsBytes()
    }

    suspend fun watchWorld(): String {
        val jsonNode = client.get("https://zaobao.wpush.cn/api/zaobao/today").body<JsonNode>()
        return jsonNode["data"]["image"].asText()
    }

    suspend fun xianBao(): List<XianBao> {
        return client.get("https://new.xianbao.fun/plus/json/push.json?230406").body<List<XianBao>>()
    }

    suspend fun meiTuanIp(ip: String): String {
        val jsonNode1 = client.get("https://apimobile.meituan.com/locate/v2/ip/loc?rgeo=true&ip=$ip").body<JsonNode>()
        if (jsonNode1.has("error")) error(jsonNode1["error"]["message"].asText())
        val data1 = jsonNode1["data"]
        val lat = data1["lat"].asText()
        val lng = data1["lng"].asText()
        val jsonNode2 = client.get("https://apimobile.meituan.com/group/v1/city/latlng/$lat,$lng?tag=0").body<JsonNode>()
        val data2 = jsonNode2["data"]
        val country = data2["country"]?.asText() ?: ""
        val province = data2["province"]?.asText() ?: ""
        val city = data2["city"]?.asText() ?: ""
        val district = data2["district"]?.asText() ?: ""
        val areaName = data2["areaName"]?.asText() ?: ""
        val detail = data2["detail"]?.asText() ?: ""
        return "$country$province$city$district$areaName$detail"
    }

    suspend fun bgp(keyword: String): File {
        var response = client.get("https://bgp.tools/search?q=$keyword")
        while (response.status == HttpStatusCode.TemporaryRedirect) {
            response = client.get("https://bgp.tools" + response.headers["Location"])
        }
        val url = response.request.url.toString()
        val html = response.bodyAsText()
        val imageUrl = if (url.contains("prefix/")) {
            val key = url.substring(25).replace("/", "_")
            "https://bgp.tools/pathimg/rt-$key?${UUID.randomUUID()}&loggedin"
        } else if (url.contains("prefix-selector")) {
            val elements = Jsoup.parse(html).getElementsByClass("smallonmobile nowrap")
            val str = buildString {
                for (item in elements.chunked(3)) {
                    val asn = item[0].text()
                    val prefix = item[1].text()
                    appendLine("$asn $prefix")
                }
                removeSuffix("\n")
            }
            error("该ip有多个前缀\n$str\n你应该可直接搜索asn或者前缀")
        } else if (url.contains("as")) {
            val asn = url.substringAfterLast("/")
            val down = Jsoup.parse(html).getElementById("netpolicydropdown")?.selectFirst("option[selected]")?.attr("value") ?: error("not found")
            "https://bgp.tools/pathimg/$asn-$down?"
        } else error("not found")
        val transcoder = JPEGTranscoder()
        val uuid = UUID.randomUUID().toString()
        val svgPath = "tmp" + File.separator + uuid + ".svg"
        val jpgPath = "tmp" + File.separator + uuid + ".jpg"
        val svgText = client.get(imageUrl).bodyAsText().replaceFirst("transparent", "none")
        val svgFile = File(svgPath)
        svgFile.writeText(svgText)
        transcoder.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, 0.9f)

        FileInputStream(svgPath).use { input ->
            FileOutputStream(jpgPath).use { output ->
                val inputSvg = TranscoderInput(input)
                val outputJpg = TranscoderOutput(output)
                transcoder.transcode(inputSvg, outputJpg)
            }
        }
        svgFile.deleteOnExit()
        return File(jpgPath)
    }

    suspend fun whois(domain: String, full: Boolean = false): Whois {
        val jsonNode = client.get("https://whois.233333.best/api/?domain=$domain").body<JsonNode>()
        return if (jsonNode["code"].asInt() == 0) {
            val data = jsonNode["data"]["whoisData"].asText()
            if (full) error(data)
            val map = mutableMapOf<String, String>()
            for (line in data.split("\n")) {
                val split = line.split(": ")
                if (split.size == 2) {
                    val key = split[0].trim()
                    val value = split[1].trim()
                    val saveValue = map[key]
                    if (saveValue == null) map[key] = value
                    else map[key] = "$saveValue,$value"
                }
            }
            Whois(map["Domain Name"], map["Registry Domain ID"], map["Registrant"] ?: map["Registrant Organization"], map["Registrant Contact Email"] ?: map["Registrant Email"],
                map["Registrar"] ?: map["Sponsoring Registrar"],
                map["Domain Status"], map["Name Server"], map["Creation Date"] ?: map["Registration Time"], map["Registry Expiry Date"] ?: map["Expiration Time"])
        } else {
            error(jsonNode["msg"].asText())
        }
    }

}

data class Bin(
    val number: String,
    val scheme: String,
    val type: String,
    val level: String,
    val region: String,
    val currency: String,
    val bank: String
)

data class Rate(
    val transCurr: String,
    val conversionRate: String,
    val crdhldBillCurr: String,
    val transAmt: String,
    val crdhldBillAmt: String
)

class XianBao {
    var id: Int = 0
    var title: String = ""
    var content: String = ""
    @JsonProperty("content_html")
    var contentHtml: String = ""
    var datetime: String = ""
    @JsonProperty("shorttime")
    var shortTime: String = ""
    @JsonProperty("shijianchuo")
    var time: String = ""
    @JsonProperty("cateid")
    var cateId: String = ""
    @JsonProperty("catename")
    var cateName: String = ""
    var comments: String = ""
    @JsonProperty("louzhu")
    var louZhu: String = ""
    @JsonProperty("louzhuregtime")
    var regTime: String? = null
    var url: String = ""

    fun urlIncludeDomain() = "http://new.xianbao.fun/$url"
}

data class Whois(
    val domainName: String?,
    val domainId: String?,
    val registrant: String?,
    val registrantEmail: String?,
    val registrar: String?,
    val status: String?,
    val nameserver: String?,
    val creationDate: String?,
    val expiryDate: String?
)
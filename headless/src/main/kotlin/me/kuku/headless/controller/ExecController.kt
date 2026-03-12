package me.kuku.headless.controller

import io.ktor.http.encodeURLParameter
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.time.withTimeout
import me.kuku.headless.utils.PlaywrightUtils
import me.kuku.headless.utils.newPage
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.util.UUID

private val mutex = Mutex()

fun Application.exec() {

    val port = environment.config.config("ktor.deployment").property("port").getString()
    val okHttpClient = OkHttpClient()

    routing {

        route("exec") {
            get("kuGou") {
                val phone = call.request.queryParameters.getOrFail("phone")
                val time = call.request.queryParameters.getOrFail("time")
                val request = Request.Builder().url("https://staticssl.kugou.com/common/js/min/login/kguser.v2.min.js?appid=1058").build()
                val js = okHttpClient.newCall(request).execute().use { response ->
                    response.body.string()
                }
                call.respond(ThymeleafContent("exec/kuGou", mapOf("script" to js, "phone" to phone, "time" to time)))
            }

            post("kuGou") {
                val receiveParameters = call.receiveParameters()
                val phone = receiveParameters.getOrFail("phone")
                val time = receiveParameters.getOrFail("time")
                val page = PlaywrightUtils.browser().newPage()
                try {
                    withContext(Dispatchers.IO) {
                        page.navigate("http://localhost:$port/exec/kuGou?phone=$phone&time=$time")
                        val params = page.evaluate("document.getElementById(\"params\").innerText")?.toString()
                        val pk = page.evaluate("document.getElementById(\"pk\").innerText")?.toString()
                        call.respond(mapOf("params" to params, "pk" to pk))
                    }
                } finally {
                    page.context().browser().close()
                }
            }

            get("douYin") {
                val url = call.request.queryParameters.getOrFail("url")
                call.respondTemplate("/exec/douYin", mapOf("url" to url))
            }

            post("douYin") {
                val formData = call.receiveParameters()
                val url = formData.getOrFail("url")
                val page = PlaywrightUtils.browser().newPage {
                    setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36")
                }
                var data: String? = null
                var userAgent: String? = null
                page.onRequest {
                    val requestUrl = it.url()
                    if (requestUrl.startsWith(url)) {
                        data = requestUrl
                        userAgent = it.headers()["user-agent"]
                    }
                }
                try {
                    withContext(Dispatchers.IO) {
                        page.navigate("http://localhost:$port/exec/douYin?url=${url.encodeURLParameter()}")
                        withTimeout(1000 * 30) {
                            while (true) {
                                if (data != null && userAgent != null) {
                                    call.respond(mapOf("url" to data, "userAgent" to userAgent))
                                    break
                                }
                                delay(1000)
                            }
                        }
                    }
                } finally {
                    page.context().browser().close()
                }
            }

            get("douYinSign") {
                call.respondTemplate("/exec/douYinSign")
            }

            post("douYinSign") {
                val parameters = call.receiveParameters()
                val nonce = parameters.getOrFail("nonce")
                val page = PlaywrightUtils.browser().newPage()
                try {
                    page.navigate("http://localhost:$port/exec/douYinSign")
                    val data = page.evaluate("""window.byted_acrawler.sign("", "$nonce")""").toString()
                    call.respond(mapOf("sign" to data))
                } finally {
                    page.context().browser().close()
                }
            }

            route("netEaseMusic") {
                get {
                    call.respondTemplate("/exec/netEaseMusic")
                }
            }

            post("netEase/checkToken") {
                mutex.withLock {
                    val page = PlaywrightUtils.browser().newPage()
                    try {
                        withTimeout(Duration.ofSeconds(30)) {
                            val uuid = UUID.randomUUID().toString().replace("-", "")
                            page.navigate("http://localhost:$port/exec/netEaseMusic?uuid=$uuid")
                            var i = 0
                            while (true) {
                                if (i++ > 30) error("check token api timeout")
                                val token = page.evaluate("""document.getElementById('checkToken').innerHTML""").toString()
                                if (token.isEmpty()) {
                                    delay(500)
                                    continue
                                }
                                call.respond(mapOf("checkToken" to token))
                                break
                            }
                        }
                    } finally {
                        page.context().browser().close()
                    }
                }
            }
        }

    }

}
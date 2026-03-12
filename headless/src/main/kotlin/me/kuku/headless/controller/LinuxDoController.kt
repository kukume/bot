package me.kuku.headless.controller

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.kuku.headless.utils.PlaywrightUtils
import me.kuku.headless.utils.addCookie
import me.kuku.headless.utils.cookie
import kotlin.collections.mapOf

fun Application.linuxDo() {

    val mutes = Mutex()

    routing {

        route("/linuxdo") {
            get("topic/{id}") {
                val id = call.parameters.getOrFail("id")
                val cookie = call.request.headers["cookie"] ?: error("cookie不存在")
                mutes.withLock {
                    PlaywrightUtils.browser().use {
                        val page = it.newPage()
                        page.context().addCookie(cookie, ".linux.do")
                        page.navigate("https://linux.do/t/topic/${id}")
                        delay(3000)
                        call.respond(mapOf("cookie" to page.context().cookie()))
                    }
                }
            }
            get("index") {
                val cookie = call.request.headers["cookie"] ?: error("cookie不存在")
                mutes.withLock {
                    PlaywrightUtils.browser().use {
                        val page = it.newPage()
                        page.context().addCookie(cookie, ".linux.do")
                        page.navigate("https://linux.do")
                        delay(3000)
                        call.respond(mapOf("cookie" to page.context().cookie()))
                    }
                }
            }
        }
    }

}
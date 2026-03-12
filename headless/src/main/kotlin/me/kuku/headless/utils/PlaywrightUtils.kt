package me.kuku.headless.utils

import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.Cookie
import me.kuku.headless.utils.PlaywrightBrowser.*

object PlaywrightUtils {

    private val playwright: Playwright by lazy {
        Playwright.create().also {
            Runtime.getRuntime().addShutdownHook(Thread {
                PlaywrightUtils.playwright.close()
            })
        }
    }

    fun browser(playwrightBrowser: PlaywrightBrowser = Chromium, option: BrowserType.LaunchOptions.() -> Unit = {}): com.microsoft.playwright.Browser {
        val browserType = when (playwrightBrowser) {
            Chromium -> playwright.chromium()
            Firefox -> playwright.firefox()
            Webkit -> playwright.webkit()
        }
        val options = BrowserType.LaunchOptions().apply(option)
        return browserType.launch(options)
    }

}

enum class PlaywrightBrowser {
    Chromium, Firefox, Webkit
}

fun com.microsoft.playwright.Browser.newPage(option: com.microsoft.playwright.Browser.NewPageOptions.() -> Unit): com.microsoft.playwright.Page {
    return newPage(com.microsoft.playwright.Browser.NewPageOptions().apply(option))
}

fun BrowserContext.addCookie(cookie: String, domain: String) {
    val cookieList = mutableListOf<Cookie>()
    val arr1 = cookie.split("; ")
    for (single in arr1) {
        val arr2 = single.split("=")
        val key = arr2[0]
        if (key.isEmpty()) continue
        val value = single.replace("${key}=", "").replace(";", "").trim()
        cookieList.add(Cookie(key, value).also {
            it.domain = domain
            it.path = "/"
        })
    }
    this.addCookies(cookieList)
}

fun BrowserContext.cookie(): String {
    val cookies = this.cookies()
    val sb = StringBuilder()
    for (cookie in cookies) {
        sb.append(cookie.name).append("=").append(cookie.value).append("; ")
    }
    return sb.toString()
}
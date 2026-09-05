package me.kuku.common.logic

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * NetEase Music web checkToken via official Yidun Watchman JS, without a headless browser.
 *
 * Requires `node` on PATH. The script loads tool.min.js / watchman.min.js and
 * exchanges JSONP with ac.dun.163.com, same as music.163.com.
 */
object NetEaseWatchman {

    const val DEFAULT_PRODUCT_NUMBER = "YD00000558929251"
    const val DEFAULT_BUSINESS_ID = "bd5d2f973ef74cd2a61325a412ae54d9"

    private const val SCRIPT_RESOURCE = "/netease/get-check-token.mjs"

    fun nodeAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("node", "-v").redirectErrorStream(true).start()
            process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    fun checkToken(
        productNumber: String = DEFAULT_PRODUCT_NUMBER,
        businessId: String = DEFAULT_BUSINESS_ID,
        timeoutMs: Long = 25_000
    ): String {
        val script = extractedScript()
        val process = ProcessBuilder("node", script.absolutePath, productNumber, businessId)
            .start()
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val outThread = Thread.startVirtualThread {
            process.inputStream.bufferedReader().use { stdout.append(it.readText()) }
        }
        val errThread = Thread.startVirtualThread {
            process.errorStream.bufferedReader().use { stderr.append(it.readText()) }
        }
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForcibly()
            error("watchman getToken timed out after ${timeoutMs}ms")
        }
        outThread.join(1_000)
        errThread.join(1_000)
        val out = stdout.toString().trim()
        val err = stderr.toString().trim()
        if (process.exitValue() != 0) {
            error("watchman getToken failed (exit ${process.exitValue()}): ${err.ifEmpty { out }}")
        }
        require(out.isNotEmpty()) { "empty checkToken${if (err.isEmpty()) "" else ": $err"}" }
        return out
    }

    private fun extractedScript(): File {
        val tmp = File.createTempFile("netease-get-check-token", ".mjs")
        tmp.deleteOnExit()
        val stream = NetEaseWatchman::class.java.getResourceAsStream(SCRIPT_RESOURCE)
            ?: error("missing classpath resource $SCRIPT_RESOURCE")
        stream.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        }
        return tmp
    }
}

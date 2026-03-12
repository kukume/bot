package me.kuku.common.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kuku.common.ktor.client
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.text.substring

private val commandLog = LoggerFactory.getLogger("command")

suspend fun ffmpeg(command: String) {
    val runtime = Runtime.getRuntime()
    val process = withContext(Dispatchers.IO) {
        val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)
        if (isWindows) {
            runtime.exec(arrayOf("cmd", "/C", command))
        } else {
            runtime.exec(arrayOf("/bin/sh", "-c", command))
        }
    }
    Thread.startVirtualThread {
        BufferedReader(InputStreamReader(process.inputStream)).use { br ->
            while (true) {
                val line = br.readLine() ?: break
                commandLog.info("command: $line")
            }
        }
    }
    Thread.startVirtualThread {
        BufferedReader(InputStreamReader(process.errorStream)).use { br ->
            while (true) {
                val line = br.readLine() ?: break
                commandLog.error("command: $line")
            }
        }
    }
    withContext(Dispatchers.IO) {
        process.waitFor()
    }
}

fun qrcode(text: String): ByteArray {
    val side = 200
    val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF8", EncodeHintType.MARGIN to 0)
    val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, side, side, hints)
    val bos = ByteArrayOutputStream()
    MatrixToImageWriter.writeToStream(bitMatrix, "JPEG", bos)
    return bos.use {
        it.toByteArray()
    }
}

fun command(command: String): String {
    val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)
    val process = if (isWindows) {
        ProcessBuilder("cmd", "/c", command)
            .redirectErrorStream(true)
            .start()
    } else {
        ProcessBuilder("/bin/bash", "-c", command)
            .redirectErrorStream(true)
            .start()
    }
    val sb = StringBuilder()
    Thread.startVirtualThread {
        process.inputStream.bufferedReader().use { br ->
            while (true) {
                val s = br.readLine() ?: break
                commandLog.info("command: $s")
                sb.append(s)
            }
        }
    }
    Thread.startVirtualThread {
        process.errorStream.bufferedReader().use { br ->
            while (true) {
                val line = br.readLine() ?: break
                commandLog.error("command: $line")
            }
        }
    }
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        error("command error: $exitCode")
    }
    return sb.toString()
}

suspend fun segmentsDownload(file: File, url: String, block: HttpRequestBuilder.() -> Unit = {}) {
    FileOutputStream(file, true).use { fos ->
        var prefixLength = 0
        var suffixLength = 4000000

        var newUrl = url

        while (true) {
            val videoResponse = client.get(newUrl) {
                block()
                headers {
                    append("range", "bytes=$prefixLength-$suffixLength")
                }
            }
            val location = videoResponse.headers["Location"]
            if (location != null) {
                newUrl = location
                continue
            }
            videoResponse.body<InputStream>().use {
                it.copyTo(fos)
            }
            val contentRange = videoResponse.headers["Content-Range"]?.substring(6) ?: error("分段下载视频失败")
            val allLength = contentRange.split("/")[1].toInt()
            if (suffixLength >= allLength) break
            else {
                prefixLength = suffixLength + 1
                val tempLength = suffixLength + 4000000
                suffixLength = if (tempLength < allLength) tempLength else allLength
            }
        }
    }
}

package me.kuku.common.utils

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.client.config.RequestHttpChecksumConfig
import aws.smithy.kotlin.runtime.client.config.ResponseHttpChecksumConfig
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import me.kuku.common.ktor.client
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration.Companion.hours

private class S3Config {
    val region: String = System.getenv("S3_REGION") ?: error("Missing S3_REGION")
    val accessKeyId: String = System.getenv("S3_ACCESS_KEY_ID") ?: error("Missing S3_ACCESS_KEY_ID")
    val secretAccessKey: String = System.getenv("S3_SECRET_ACCESS_KEY") ?: error("Missing S3_SECRET_ACCESS_KEY")
    val bucket: String = System.getenv("S3_BUCKET") ?: error("Missing S3_BUCKET")
    val endpointUrl: String = System.getenv("S3_ENDPOINT_URL") ?: error("Missing S3_ENDPOINT_URL")
}

object S3Utils {

    private val s3Config by lazy {
        S3Config()
    }

    fun s3Client(): S3Client {
        return S3Client {
            region = s3Config.region
            credentialsProvider = StaticCredentialsProvider(Credentials(s3Config.accessKeyId, s3Config.secretAccessKey))
            endpointUrl = Url.parse(s3Config.endpointUrl)
            forcePathStyle = true
            enableAwsChunked = false
            requestChecksumCalculation = RequestHttpChecksumConfig.WHEN_REQUIRED
            responseChecksumValidation  = ResponseHttpChecksumConfig.WHEN_REQUIRED
        }
    }


    suspend fun putObject(key: String, file: File) {
        s3Client().use {
            it.putObject {
                bucket = s3Config.bucket
                this.key = key
                body = file.asByteStream()
            }
        }
    }

    suspend fun putObject(key: String, path: Path) {
        s3Client().use {
            it.putObject {
                bucket = s3Config.bucket
                this.key = key
                body = path.asByteStream()
            }
        }
    }

    suspend fun putObject(key: String, byteArray: ByteArray) {
        s3Client().use {
            it.putObject {
                bucket = s3Config.bucket
                this.key = key
                body = ByteStream.fromBytes(byteArray)
            }
        }
    }

    suspend fun presignedUrl(key: String): String {
        val unsignedRequest = GetObjectRequest {
                bucket = s3Config.bucket
                this.key = key
            }
        s3Client().use {
            val presignedRequest = it.presignGetObject(unsignedRequest, 1.hours)
            return presignedRequest.url.toString()
        }

    }

    suspend fun deleteObject(key: String) {
        s3Client().use {
            it.deleteObject {
                bucket = s3Config.bucket
                this.key = key
            }
        }
    }

    suspend fun presignedUrl(key: String, path: String): String {
        val presignedUrl = presignedUrl(key)
        val response = client.get(presignedUrl)
        if (response == HttpStatusCode.OK) return presignedUrl
        putObject(key, File(path))
        return presignedUrl(key)
    }

}
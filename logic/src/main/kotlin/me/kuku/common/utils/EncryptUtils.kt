package me.kuku.common.utils

import java.math.BigInteger
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun String.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String.toUrlDecode(): String {
    return URLDecoder.decode(this, "UTF-8")
}

fun String.toUrlEncode(): String {
    return URLEncoder.encode(this, "UTF-8")
}

fun String.rsaEncrypt(publicKeyStr: String): String {
    val keyFactory = KeyFactory.getInstance("RSA")
    val decodedKey = Base64.getDecoder().decode(publicKeyStr.toByteArray())
    val keySpec = X509EncodedKeySpec(decodedKey)
    val publicKey =  keyFactory.generatePublic(keySpec)
    val cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    val encryptedBytes = cipher.doFinal(this.toByteArray(Charsets.UTF_8))
    return Base64.getEncoder().encodeToString(encryptedBytes)
}

fun String.rsaEncrypt(publicKey: PublicKey): String {
    val cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    val encryptedBytes = cipher.doFinal(this.toByteArray(Charsets.UTF_8))
    return Base64.getEncoder().encodeToString(encryptedBytes)
}

fun publicKey(modulus: String, publicExponent: String): PublicKey {
    val bigIntModulus = BigInteger(modulus, 16)
    val bigIntPrivateExponent = BigInteger(publicExponent, 16)
    val keySpec = RSAPublicKeySpec(bigIntModulus, bigIntPrivateExponent)
    val keyFactory = KeyFactory.getInstance("RSA")
    return keyFactory.generatePublic(keySpec)
}

fun ByteArray.hex(): String {
    return this.joinToString("") { "%02x".format(it) }
}

fun String.aes(key: String, iv: String): ByteArray {
    val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
    val ivSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

    return cipher.doFinal(this.toByteArray(Charsets.UTF_8))
}

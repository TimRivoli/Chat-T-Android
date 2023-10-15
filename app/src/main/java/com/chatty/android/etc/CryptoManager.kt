package com.chatty.android.etc
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Log
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyGenerator
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

//Encrypted content conversations
//text = Base64.getEncoder().encodeToString(bytes)
//bytes = Base64.getDecoder().decode(text)
//
//Text string conversation
//bytes = text.toByteArray()
//text = String(decryptedBytes, Charsets.UTF_8)

object CryptoManager {
    private const val TAG = "CryptoManager"
    private const val keyName = "ChattyAndroidKey"
    private lateinit var keyStore: KeyStore

    fun initialize(store: KeyStore ) {
        this.keyStore = store
        this.keyStore.load(null)
    }

    private fun generateAesKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        val random = SecureRandom()
        keyGenerator.init(random)
        return keyGenerator.generateKey()
    }

    private fun keyExists(keyName: String): Boolean {
        return try {
            keyStore.getKey(keyName, null) != null
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Error checking for keyExists", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking for keyExists", e)
            false
        }
    }

    private fun generateRSAKeys() {
        Log.d(TAG, "Generating RSA keys")
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore"  )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder( keyName,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .build()
        keyPairGenerator.initialize(keyGenParameterSpec)
        keyPairGenerator.generateKeyPair()
    }

    private fun getPublicKey(): PublicKey {
        if (!keyExists(keyName)) {generateRSAKeys()}
        return keyStore.getCertificate(keyName).publicKey
    }

    private fun getPrivateKey(): PrivateKey {
        Log.d(TAG, "getPrivateKey")
        if (!keyExists(keyName)) {generateRSAKeys()}
        return keyStore.getKey(keyName, null) as PrivateKey
    }

    fun getPublicKeyString(): String {
        return Base64.getEncoder().encodeToString(getPublicKey().encoded)
    }

    private fun encryptWithAes(byteArray: ByteArray, key: SecretKey, iv: ByteArray): ByteArray {
        Log.d(TAG, "Encrypting with AES... Size: " + byteArray.size)
        try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            return cipher.doFinal(byteArray)
        } catch (ex: Exception) {
            Log.e(TAG, "AES encryption failure", ex)
            return byteArrayOf()
        }
    }

    private fun decryptWithAes(byteArray: ByteArray, key: SecretKey, iv: ByteArray): ByteArray {
        try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            return cipher.doFinal(byteArray)
        } catch (ex: Exception) {
            Log.e(TAG, "AES decryption failure", ex)
            return byteArrayOf()
        }
    }

    private fun encryptWithRSA(byteArray: ByteArray, publicKey: PublicKey): ByteArray {
        Log.d(TAG, "Encrypting with RSA... Size: " + byteArray.size)
        var result = byteArrayOf()
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        try {
            result = cipher.doFinal(byteArray)
        } catch (ex: Exception) {
            Log.e(TAG, "RSA encryption failure", ex)
        }
        return result
    }

    private fun decryptWtihRSA(encryptedBytes: ByteArray): ByteArray {
        Log.d(TAG, "Decrypting with RSA")
        var result = byteArrayOf()
        val privateKey = getPrivateKey()
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        try {
            result = cipher.doFinal(encryptedBytes)
        } catch (ex:Exception) {
            Log.e(TAG, "RSA decryption failure", ex)
        }
        return result
    }

    fun encryptStringAES(text:String, aesKeyString: String=""): String {
        val aesKey = SecretKeySpec(Base64.getDecoder().decode(aesKeyString), "AES")
        val random = SecureRandom()
        val iv = ByteArray(16)
        random.nextBytes(iv)
        val sourceBytes = text.toByteArray()
        val encryptedBytes = encryptWithAes(sourceBytes, aesKey,iv)
        val result = Base64.getEncoder().encodeToString(iv) + "|" + Base64.getEncoder().encodeToString(encryptedBytes)
        return result
    }

    fun decryptStringAES(text:String, aesKeyString: String=""): String {
        var result = ""
        val aesKey = SecretKeySpec(Base64.getDecoder().decode(aesKeyString), "AES")
        val x = text.split("|")
        if (x.size == 2){
            val iv = Base64.getDecoder().decode(x[0])
            val encryptedBytes = Base64.getDecoder().decode(x[1])
            val decryptedBytes = decryptWithAes(encryptedBytes, aesKey, iv)
            result = String(decryptedBytes, Charsets.UTF_8)
        } else {
            Log.e(TAG, "Malformed AES encrypted text.")
        }
        return result
    }

    fun encryptStringRSA(text:String, publicKeyString: String=""): String {
        Log.d(TAG, "encryptStringRSA")
        var result = ""
        Log.d(TAG, "Encrypting string length: " + text.length + " text:" + text.take(25) )
        var publicKey: PublicKey =  getPublicKey()
        if (publicKeyString !="") {
            val publicKeyByteArray = Base64.getDecoder().decode(publicKeyString)
            val keySpec = X509EncodedKeySpec(publicKeyByteArray)
            val keyFactory = KeyFactory.getInstance("RSA")
            publicKey =  keyFactory.generatePublic(keySpec)
        }
        val sourceBytes = text.toByteArray()
        if (text.length < 190) {
            Log.d(TAG, "Short string is directly encrypted")
            val encryptedBytes = encryptWithRSA(sourceBytes, publicKey)
            result = Base64.getEncoder().encodeToString(encryptedBytes)
        } else {
            Log.d(TAG, "Long string is encrypted with AES")
            val aesKey = generateAesKey()
            val encryptedAESKey = encryptWithRSA(aesKey.encoded, publicKey)
            Log.d(TAG, "Created and encrypted new AES key.  Len: " + encryptedAESKey.size)
            val random = SecureRandom()
            val iv = ByteArray(16)
            random.nextBytes(iv)
            val encryptedBytes = encryptWithAes(sourceBytes, aesKey, iv)
            result = Base64.getEncoder().encodeToString(encryptedAESKey)
            result += "|" + Base64.getEncoder().encodeToString(iv)
            result += "|" + Base64.getEncoder().encodeToString(encryptedBytes)
        }
        return result
    }

    fun decryptStringRSA(text: String): String {
        Log.d(TAG, "decryptStringRSA")
        var result = ""
        val x = text.split("|")
        if (x.size == 1) {
            Log.d(TAG, "Short string is directly decrypted")
            val decryptedBytes = decryptWtihRSA(Base64.getDecoder().decode(text))
            result = String(decryptedBytes, Charsets.UTF_8)
        } else if (x.size == 3) {
            Log.d(TAG, "Long string is decrypted with AES")
            val encryptedAESKey = Base64.getDecoder().decode(x[0])
            val decryptedAESKey = decryptWtihRSA(encryptedAESKey)
            val aesKey = SecretKeySpec(decryptedAESKey, "AES")
            val iv = Base64.getDecoder().decode(x[1])
            val encryptedBytes = Base64.getDecoder().decode(x[2])
            val decryptedBytes = decryptWithAes(encryptedBytes, aesKey, iv)
            result = String(decryptedBytes, Charsets.UTF_8)
        } else {
            Log.e(TAG, "Decrypt string, malformed input array")
        }
        return result
    }

    fun exportNewAESKey(): String{
        val aesKey = generateAesKey()
        return Base64.getEncoder().encodeToString(aesKey.encoded)
    }


    fun Test(text: String) {
        Log.d(TAG, "Testing encryption...")
        val t1 = encryptStringRSA(text)
        Log.d(TAG, "Encrypted: $t1")
        val t2 = decryptStringRSA(t1)
        Log.d(TAG, "Output: $t2")
        Log.d(TAG, "Result: " + (text==t2).toString())
    }
}
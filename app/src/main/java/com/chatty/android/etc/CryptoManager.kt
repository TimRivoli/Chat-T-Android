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

    private fun bytesToText(b: ByteArray): String { return String(b, Charsets.UTF_8) }
    private fun bytesFromText(text: String): ByteArray { return text.toByteArray(Charsets.UTF_8) }
    private fun bytesToBase64Text(b: ByteArray): String { return String(Base64.getEncoder().encode(b), Charsets.UTF_8) }
    private fun bytesFromBase64Text(text: String): ByteArray {return Base64.getDecoder().decode(text) }
    //private fun bytesToIntString(b: ByteArray): String { val intList = b.map { it.toInt() and 0xFF}; return intList.joinToString(":") { it.toString() } }
    //private fun bytesFromIntString(intString:String): ByteArray { val l = intString.split(":").map { it.toInt() }; return ByteArray(l.size) { index -> l[index].toByte() } }

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

    private fun publicKeyFromString(publicKeyString: String):PublicKey? {
        try {
            val publicKeyByteArray = bytesFromBase64Text(publicKeyString)
            val keySpec = X509EncodedKeySpec(publicKeyByteArray)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePublic(keySpec)
        }
        catch (ex: Exception) {
            Log.e(TAG, "Unable to create public key from given string", ex)
            return null
        }
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

    fun getPublicKeyString(): String { return bytesToBase64Text(getPublicKey().encoded)  }

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
        val aesKey = SecretKeySpec(bytesFromBase64Text(aesKeyString), "AES")
        val random = SecureRandom()
        val iv = ByteArray(16)
        random.nextBytes(iv)
        val sourceBytes = text.toByteArray()
        val encryptedBytes = encryptWithAes(sourceBytes, aesKey,iv)
        val result = bytesToBase64Text(iv) + "|" + bytesToBase64Text(encryptedBytes)
        return result
    }

    fun decryptStringAES(text:String, aesKeyString: String=""): String {
        var result = ""
        val aesKey = SecretKeySpec(bytesFromBase64Text(aesKeyString), "AES")
        val x = text.split("|")
        if (x.size == 2){
            val iv = bytesFromBase64Text(x[0])
            val encryptedBytes = bytesFromBase64Text(x[1])
            val decryptedBytes = decryptWithAes(encryptedBytes, aesKey, iv)
            result =  bytesToText(decryptedBytes)
        } else {
            Log.e(TAG, "Malformed AES encrypted text.")
        }
        return result
    }

    fun encryptStringRSA(text:String, publicKeyString: String=""): String {
        var result = ""
        //Log.d(TAG, "Encrypting string length: " + text.length + " text:" + text.take(25) )
        var publicKey: PublicKey =  getPublicKey()
        if (publicKeyString !="") {
            val x =  publicKeyFromString(publicKeyString)
            if (x ==null) {
                Log.e(TAG, "Unable to generate public key from given input")
                return ""
            } else {publicKey = x}
        }
        val sourceBytes = bytesFromText(text)
        if (text.length < 190) {
            Log.d(TAG, "Short string is directly encrypted with RSA")
            val encryptedBytes = encryptWithRSA(sourceBytes, publicKey)
            result = bytesToBase64Text(encryptedBytes)
        } else {
            Log.d(TAG, "Long string is encrypted with AES")
            val aesKey = generateAesKey()
            val encryptedAESKey = encryptWithRSA(aesKey.encoded, publicKey)
            Log.d(TAG, "Created and encrypted new AES key.  Len: " + encryptedAESKey.size)
            val random = SecureRandom()
            val iv = ByteArray(16)
            random.nextBytes(iv)
            val encryptedBytes = encryptWithAes(sourceBytes, aesKey, iv)
            result = bytesToBase64Text(encryptedAESKey)
            result += "|" + bytesToBase64Text(iv)
            result += "|" + bytesToBase64Text(encryptedBytes)
        }
        return result
    }

    fun decryptStringRSA(text: String): String {
        var result = ""
        val x = text.split("|")
        if (x.size == 1) {
            Log.d(TAG, "Short string is directly decrypted from RSA")
            val decryptedBytes = decryptWtihRSA(bytesFromBase64Text(text))
            result = bytesToText(decryptedBytes)
        } else if (x.size == 3) {
            Log.d(TAG, "Long string is decrypted with AES")
            val encryptedAESKey = bytesFromBase64Text(x[0])
            val decryptedAESKey = decryptWtihRSA(encryptedAESKey)
            val aesKey = SecretKeySpec(decryptedAESKey, "AES")
            val iv = bytesFromBase64Text(x[1])
            val encryptedBytes = bytesFromBase64Text(x[2])
            val decryptedBytes = decryptWithAes(encryptedBytes, aesKey, iv)
            result = bytesToText(decryptedBytes)
        } else {
            Log.e(TAG, "Decrypt string, malformed input array")
        }
        return result
    }

    fun exportNewAESKey(): String{ return bytesToBase64Text(generateAesKey().encoded) }

    fun Test(text: String) {
        val pkByte = getPublicKey().encoded
        val s1 = bytesToBase64Text(pkByte)
        val A = bytesToBase64Text(pkByte)
        val B = bytesFromBase64Text(A)
        val s2 = bytesToBase64Text(B)
        Log.d(TAG, "Key s1: $s1")
        Log.d(TAG, "Key s2: $s2")

        val publicKeyString = getPublicKeyString()
        Log.d(TAG, "Key: $publicKeyString")
        Log.d(TAG, "Testing encryption...")
        val t1 = encryptStringRSA(text, publicKeyString)
        Log.d(TAG, "Testing Encrypted: $t1")
        val t2 = decryptStringRSA(t1)
        Log.d(TAG, "Testing Output: $t2")
        Log.d(TAG, "Testing Result: " + (text==t2).toString())
    }
}
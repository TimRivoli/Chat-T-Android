# Chatty Android CryptoManager

## Overview

The `CryptoManager` is a utility class for handling encryption and decryption operations within the Chatty Android application. It provides methods for both AES and RSA encryption schemes.

### AES Encryption

AES (Advanced Encryption Standard) is a symmetric encryption algorithm used to secure sensitive data. The `CryptoManager` includes functions for generating AES keys, encrypting and decrypting data using AES, and exporting a new AES key.

### RSA Encryption

RSA is an asymmetric encryption algorithm that uses a pair of keys: a public key for encryption and a private key for decryption. The `CryptoManager` facilitates the generation of RSA key pairs, encryption and decryption using RSA, and handling long strings through a combination of AES and RSA.

## Usage

```java
// Initialize the CryptoManager with a KeyStore
CryptoManager.initialize(keyStore);

// Generate and store RSA keys if not already present
CryptoManager.generateRSAKeys();

// Encrypt and decrypt with AES
String encryptedAES = CryptoManager.encryptStringAES(text, aesKeyString);
String decryptedAES = CryptoManager.decryptStringAES(encryptedAES, aesKeyString);

// Encrypt and decrypt with RSA
String publicKeyString = CryptoManager.getPublicKeyString();
String encryptedRSA = CryptoManager.encryptStringRSA(text, publicKeyString);
String decryptedRSA = CryptoManager.decryptStringRSA(encryptedRSA);

// Export a new AES key
String newAESKey = CryptoManager.exportNewAESKey();

// Testing encryption
CryptoManager.Test(text);
```

## Methods

### `initialize(store: KeyStore)`

Initialize the `CryptoManager` with a `KeyStore`.

### `generateRSAKeys()`

Generate and store RSA key pair in the Android Keystore.

### `getPublicKeyString()`

Get the Base64 encoded string representation of the public key.

### `encryptStringAES(text: String, aesKeyString: String = ""): String`

Encrypt a text using AES encryption. Returns the encrypted text as a Base64 encoded string.

### `decryptStringAES(text: String, aesKeyString: String = ""): String`

Decrypt an AES encrypted text. Returns the original text.

### `encryptStringRSA(text: String, publicKeyString: String = ""): String`

Encrypt a text using RSA encryption. Returns the encrypted text as a Base64 encoded string.

### `decryptStringRSA(text: String): String`

Decrypt an RSA encrypted text. Returns the original text.

### `exportNewAESKey(): String`

Generate a new AES key and export it as a Base64 encoded string.

### `Test(text: String)`

A testing method to verify encryption and decryption operations.

## Notes

- For AES encryption, you can optionally provide an existing AES key as a Base64 encoded string. If not provided, a new AES key will be generated.

- For RSA encryption, you can optionally provide a public key as a Base64 encoded string. If not provided, the default public key from the keystore will be used.

- The `Test` method demonstrates the encryption and decryption process for testing purposes. It logs the results to assist with verification.




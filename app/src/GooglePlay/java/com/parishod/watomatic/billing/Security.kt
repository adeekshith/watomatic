package com.parishod.watomatic.billing

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Security-related methods for Google Play Billing.
 * For a secure implementation, all of this code should be implemented on
 * a server that communicates with the application on the device.
 */
object Security {
    private const val TAG = "BillingSecurity"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    /**
     * Verifies that the data was signed with the given signature
     *
     * @param base64PublicKey the base64-encoded public key to use for verifying.
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature the signature for the data, signed with the private key
     * @return true if the signature is valid, false otherwise
     */
    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if (TextUtils.isEmpty(signedData) || TextUtils.isEmpty(base64PublicKey) || TextUtils.isEmpty(signature)) {
            Log.e(TAG, "Purchase verification failed: missing data.")
            return false
        }

        val key = generatePublicKey(base64PublicKey)
        return verify(key, signedData, signature)
    }

    /**
     * Generates a PublicKey instance from a string containing the Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     */
    private fun generatePublicKey(encodedPublicKey: String): PublicKey {
        return try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalArgumentException("RSA algorithm not supported", e)
        } catch (e: InvalidKeySpecException) {
            throw IllegalArgumentException("Invalid key specification: $encodedPublicKey", e)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Base64 decoding failed", e)
        }
    }

    /**
     * Verifies that the signature from the server matches the computed signature on the data.
     * Returns true if the data is correctly signed.
     *
     * @param publicKey public key associated with the developer account
     * @param signedData signed data from server
     * @param signature server signature
     * @return true if the data and signature match
     */
    private fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        return try {
            val signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGORITHM)
            signatureAlgorithm.initVerify(publicKey)
            signatureAlgorithm.update(signedData.toByteArray())
            
            val decodedSignature = Base64.decode(signature, Base64.DEFAULT)
            signatureAlgorithm.verify(decodedSignature)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "NoSuchAlgorithmException", e)
            false
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Invalid key specification", e)
            false
        } catch (e: SignatureException) {
            Log.e(TAG, "Signature exception", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Base64 decoding failed", e)
            false
        }
    }
}

package com.javasteam.steam.crypto;

import com.javasteam.utils.common.ArrayUtils;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.zip.CRC32;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/**
 * Crypto class for establishing secure connections with the Steam network and encrypting/decrypting
 * messages
 */
@Slf4j
public class Crypto {
  private static final String UNIVERSE_KEY =
      "MIGdMA0GCSqGSIb3DQEBAQUAA4GLADCBhwKBgQDf7BrWLBBmLBc1OhSwfFkRf53T"
          + "2Ct64+AVzRkeRuh7h3SiGEYxqQMUeYKO6UWiSRKpI2hzic9pobFhRr3Bvr/WARvY"
          + "gdTckPv+T1JzZsuVcNfFjrocejN1oWI0Rrtgt4Bo+hOneoo3S57G9F1fOpn5nsQ6"
          + "6WOiu4gZKODnFMBCiQIBEQ==";

  private static final int BLOCK_SIZE = 16; // Block size for AES
  private static final int HMAC_HASH_SIZE = 13; // Size of the HMAC hash
  private static final int PREFIX_SIZE = 3; // Size of the prefix

  /**
   * Encrypt or decrypt data using AES/ECB/NoPadding
   *
   * @param data the data to encrypt or decrypt
   * @param key the key to use
   * @param mode the mode to use
   * @return the encrypted or decrypted data
   */
  public static byte[] cipherECB(byte[] data, SecretKeySpec key, int mode) {
    try {
      Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
      cipher.init(mode, key);
      return cipher.doFinal(data);
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Encrypt or decrypt data using AES/CBC/PKCS5Padding
   *
   * @param data the data to encrypt or decrypt
   * @param iv the IV to use
   * @param key the key to use
   * @param mode the mode to use
   * @return the encrypted or decrypted data
   */
  public static byte[] cipherCBC(byte[] data, IvParameterSpec iv, SecretKeySpec key, int mode) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(mode, key, iv);
      return cipher.doFinal(data);
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | BadPaddingException
        | IllegalBlockSizeException
        | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Decrypt message using AES/ECB/NoPadding, AES/CBC/PKCS5Padding and HMAC SHA-1
   *
   * @param ciphertext the data to decrypt
   * @param decryptionKey the key to use
   * @param hmacSecret the HMAC to use
   * @return the decrypted message
   */
  public static byte[] decryptMessage(byte[] ciphertext, byte[] decryptionKey, byte[] hmacSecret) {
    SecretKeySpec secretKeySpec = new SecretKeySpec(decryptionKey, "AES");

    byte[] iv =
        cipherECB(
            ArrayUtils.subarray(ciphertext, 0, BLOCK_SIZE), secretKeySpec, Cipher.DECRYPT_MODE);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

    byte[] message =
        cipherCBC(
            ArrayUtils.subarray(ciphertext, BLOCK_SIZE, ciphertext.length - BLOCK_SIZE),
            ivParameterSpec,
            secretKeySpec,
            Cipher.DECRYPT_MODE);

    byte[] hmacHash =
        hmacSha1(
            ArrayUtils.concat(
                ArrayUtils.subarray(iv, iv.length - PREFIX_SIZE, PREFIX_SIZE), message),
            hmacSecret);

    if (!ArrayUtils.equals(hmacHash, iv, 0, HMAC_HASH_SIZE)) {
      throw new RuntimeException("Failed to verify HMAC");
    }

    return message;
  }

  /**
   * Encrypt message using AES/ECB/NoPadding, AES/CBC/PKCS5Padding and HMAC SHA-1
   *
   * @param message the data to encrypt
   * @param encryptionKey the key to use
   * @param hmacSecret the HMAC to use
   * @return the encrypted message
   */
  public static byte[] encryptMessage(byte[] message, byte[] encryptionKey, byte[] hmacSecret) {
    byte[] prefix = getRandomBytes(PREFIX_SIZE);
    byte[] hmacHash = hmacSha1(ArrayUtils.concat(prefix, message), hmacSecret);
    byte[] iv = ArrayUtils.concat(ArrayUtils.subarray(hmacHash, HMAC_HASH_SIZE), prefix);

    SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, "AES");
    byte[] encryptedIv = cipherECB(iv, secretKeySpec, Cipher.ENCRYPT_MODE);

    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    byte[] ciphertext = cipherCBC(message, ivParameterSpec, secretKeySpec, Cipher.ENCRYPT_MODE);

    return ArrayUtils.concat(encryptedIv, ciphertext);
  }

  /**
   * Calculate HMAC SHA-1
   *
   * @param data the data to calculate the HMAC of
   * @param hmacSecret the HMAC to use
   * @return the calculated HMAC
   */
  public static byte[] hmacSha1(byte[] data, byte[] hmacSecret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      SecretKeySpec hmacKeySpec = new SecretKeySpec(hmacSecret, "HmacSHA1");
      mac.init(hmacKeySpec);
      return mac.doFinal(data);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException("Failed to calculate HMAC", e);
    }
  }

  /**
   * Generate random bytes
   *
   * @param size the size of the random bytes
   * @return the generated random bytes
   */
  public static byte[] getRandomBytes(int size) {
    try {
      byte[] randomBytes = new byte[size];
      SecureRandom.getInstanceStrong().nextBytes(randomBytes);
      return randomBytes;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to generate random bytes", e);
    }
  }

  /**
   * Encrypt session key using RSA/ECB/OAEPWithSHA-1AndMGF1Padding and HMAC SHA-1
   *
   * @param hmacSecret the HMAC to use
   * @return the encrypted session key
   */
  public static byte[] encryptSessionKey(byte[] sessionKey, byte[] hmacSecret) {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      byte[] keyBytes = java.util.Base64.getDecoder().decode(Crypto.UNIVERSE_KEY);
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
      PublicKey publicKey = keyFactory.generatePublic(keySpec);

      Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);

      return cipher.doFinal(ArrayUtils.concat(sessionKey, hmacSecret));
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeySpecException
        | InvalidKeyException
        | BadPaddingException
        | IllegalBlockSizeException e) {
      throw new RuntimeException("Failed to encrypt session key", e);
    }
  }

  /**
   * Generate a 32 byte session key
   *
   * @return the generated session key
   */
  public static byte[] generateSessionKey() {
    byte[] sessionKey = getRandomBytes(32);
    log.debug("Generated session key: {}", Arrays.toString(sessionKey));
    return sessionKey;
  }

  /**
   * Calculate the CRC32 of the given data
   *
   * @param data the data to calculate the CRC32 of
   * @return the CRC32 of the data
   */
  public static int calculateCRC32(byte[] data) {
    CRC32 crc = new CRC32();
    crc.update(data);
    return (int) crc.getValue();
  }
}

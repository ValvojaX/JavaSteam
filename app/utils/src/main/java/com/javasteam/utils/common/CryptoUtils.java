package com.javasteam.utils.common;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.zip.CRC32;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Utility class for common crypto operations. */
public class CryptoUtils {
  public static final String AES_ECB_NO_PADDING = "AES/ECB/NoPadding";
  public static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
  public static final String RSA = "RSA";
  public static final String AES = "AES";
  public static final String HMAC_SHA1 = "HmacSHA1";
  public static final String PBKDF2_WITH_HMAC_SHA256 = "PBKDF2WithHmacSHA256";

  /**
   * Encrypt data using the given algorithm, data, key, and IV
   *
   * @param algorithm the algorithm to use
   * @param data the data to encrypt
   * @param key the key to use
   * @return the encrypted data
   */
  public static byte[] encrypt(String algorithm, byte[] data, Key key) {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      cipher.init(Cipher.ENCRYPT_MODE, key);
      return cipher.doFinal(data);
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new RuntimeException("Failed to encrypt data", e);
    }
  }

  /**
   * Encrypt data using the given algorithm, data, key, and IV
   *
   * @param algorithm the algorithm to use
   * @param data the data to encrypt
   * @param key the key to use
   * @param iv the IV to use
   * @return the encrypted data
   */
  public static byte[] encrypt(String algorithm, byte[] data, SecretKey key, IvParameterSpec iv) {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      cipher.init(Cipher.ENCRYPT_MODE, key, iv);
      return cipher.doFinal(data);
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | IllegalBlockSizeException
        | BadPaddingException
        | InvalidAlgorithmParameterException e) {
      throw new RuntimeException("Failed to encrypt data", e);
    }
  }

  /**
   * Decrypt data using the given algorithm, data, and key
   *
   * @param algorithm the algorithm to use
   * @param data the data to decrypt
   * @param key the key to use
   * @return the decrypted data
   */
  public static byte[] decrypt(String algorithm, byte[] data, SecretKeySpec key) {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      cipher.init(Cipher.DECRYPT_MODE, key);
      return cipher.doFinal(data);
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new RuntimeException("Failed to decrypt data", e);
    }
  }

  /**
   * Decrypt data using the given algorithm, data, key, and IV
   *
   * @param algorithm the algorithm to use
   * @param data the data to decrypt
   * @param key the key to use
   * @param iv the IV to use
   * @return the decrypted data
   */
  public static byte[] decrypt(String algorithm, byte[] data, SecretKey key, IvParameterSpec iv) {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      cipher.init(Cipher.DECRYPT_MODE, key, iv);
      return cipher.doFinal(data);
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | IllegalBlockSizeException
        | BadPaddingException
        | InvalidAlgorithmParameterException e) {
      throw new RuntimeException("Failed to decrypt data", e);
    }
  }

  /**
   * Encrypt data using AES/ECB/NoPadding
   *
   * @param data the data to encrypt
   * @param key the key to use
   * @return the encrypted data
   */
  public static byte[] encryptECB(byte[] data, SecretKeySpec key) {
    return encrypt(AES_ECB_NO_PADDING, data, key);
  }

  /**
   * Decrypt data using AES/ECB/NoPadding
   *
   * @param data the data to decrypt
   * @param key the key to use
   * @return the decrypted data
   */
  public static byte[] decryptECB(byte[] data, SecretKeySpec key) {
    return decrypt(AES_ECB_NO_PADDING, data, key);
  }

  /**
   * Encrypt data using AES/CBC/PKCS5Padding
   *
   * @param data the data to encrypt
   * @param key the key to use
   * @param iv the IV to use
   * @return the encrypted data
   */
  public static byte[] encryptCBC(byte[] data, SecretKey key, IvParameterSpec iv) {
    return encrypt(AES_CBC_PKCS5_PADDING, data, key, iv);
  }

  /**
   * Decrypt data using AES/CBC/PKCS5Padding
   *
   * @param data the data to decrypt
   * @param key the key to use
   * @param iv the IV to use
   * @return the decrypted data
   */
  public static byte[] decryptCBC(byte[] data, SecretKey key, IvParameterSpec iv) {
    return decrypt(AES_CBC_PKCS5_PADDING, data, key, iv);
  }

  /**
   * Encrypt the given data using the given RSA public key
   *
   * @param data the data to encrypt
   * @param rsaPublicKey the RSA public key
   * @return the encrypted data
   */
  public static byte[] encryptRSA(byte[] data, RSAPublicKey rsaPublicKey) {
    return encrypt(RSA, data, rsaPublicKey);
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
      Mac mac = Mac.getInstance(HMAC_SHA1);
      SecretKeySpec hmacKeySpec = new SecretKeySpec(hmacSecret, HMAC_SHA1);
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

  /**
   * Generate a key from the given password and salt
   *
   * @param password the password
   * @param salt the salt
   * @return the generated key
   */
  public static SecretKeySpec getKeyFromPassword(String password, byte[] salt) {
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_WITH_HMAC_SHA256);
      KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
      return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), AES);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException("Failed to generate key from password", e);
    }
  }
}

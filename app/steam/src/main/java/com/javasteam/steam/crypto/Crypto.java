package com.javasteam.steam.crypto;

import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.common.CryptoUtils;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

/** Steam network specific crypto operations */
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
        CryptoUtils.decryptECB(ArrayUtils.subarray(ciphertext, 0, BLOCK_SIZE), secretKeySpec);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

    byte[] message =
        CryptoUtils.decryptCBC(
            ArrayUtils.subarray(ciphertext, BLOCK_SIZE, ciphertext.length - BLOCK_SIZE),
            secretKeySpec,
            ivParameterSpec);

    byte[] hmacHash =
        CryptoUtils.hmacSha1(
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
    byte[] prefix = CryptoUtils.getRandomBytes(PREFIX_SIZE);
    byte[] hmacHash = CryptoUtils.hmacSha1(ArrayUtils.concat(prefix, message), hmacSecret);
    byte[] iv = ArrayUtils.concat(ArrayUtils.subarray(hmacHash, HMAC_HASH_SIZE), prefix);

    SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, "AES");
    byte[] encryptedIv = CryptoUtils.encryptECB(iv, secretKeySpec);

    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    byte[] ciphertext = CryptoUtils.encryptCBC(message, secretKeySpec, ivParameterSpec);

    return ArrayUtils.concat(encryptedIv, ciphertext);
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
   * Create an RSA public key from the given modulus and exponent
   *
   * @param modulus the modulus
   * @param exponent the exponent
   * @return the RSA public key
   */
  public static RSAPublicKey createRSAPublicKey(BigInteger modulus, BigInteger exponent) {
    try {
      RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return (RSAPublicKey) keyFactory.generatePublic(rsaPublicKeySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException("Failed to create RSA public key", e);
    }
  }

  /**
   * Generate a random machine ID
   *
   * @return the generated machine ID
   * @see <a
   *     href="https://github.com/SteamRE/SteamKit/blob/master/SteamKit2/SteamKit2/Util/HardwareUtils.cs#L321">SteamKit2/Util/HardwareUtils.cs</a>
   */
  public static byte[] generateMachineID() {
    byte[] valueBytes1 = ByteBuffer.allocate(Long.BYTES).putDouble(Math.random()).array();
    byte[] valueBytes2 = ByteBuffer.allocate(Long.BYTES).putDouble(Math.random()).array();
    byte[] valueBytes3 = ByteBuffer.allocate(Long.BYTES).putDouble(Math.random()).array();

    String hexValueHash1 = DigestUtils.sha1Hex(valueBytes1) + "\n";
    String hexValueHash2 = DigestUtils.sha1Hex(valueBytes2) + "\n";
    String hexValueHash3 = DigestUtils.sha1Hex(valueBytes3) + "\n";

    ByteBuffer buffer = ByteBuffer.allocate(155);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    buffer.put((byte) 0);
    buffer.put("MessageObject\n".getBytes());

    buffer.put((byte) 1);
    buffer.put("BB3\n".getBytes());
    buffer.put(hexValueHash1.getBytes());

    buffer.put((byte) 1);
    buffer.put("FF2\n".getBytes());
    buffer.put(hexValueHash2.getBytes());

    buffer.put((byte) 1);
    buffer.put("3B3\n".getBytes());
    buffer.put(hexValueHash3.getBytes());

    buffer.put((byte) 8);
    buffer.put((byte) 8);

    return buffer.flip().array();
  }
}

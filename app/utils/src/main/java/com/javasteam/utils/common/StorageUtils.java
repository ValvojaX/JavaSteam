package com.javasteam.utils.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/** Utility class for common storage operations. */
public class StorageUtils {
  public static final int ENCRYPTION_BLOCK_SIZE = 16;

  /**
   * Get the home directory of the current user
   *
   * @return the home directory
   */
  public static String getHomeDirectory() {
    return System.getProperty("user.home");
  }

  /**
   * Create directories
   *
   * @param filePath to create directories
   * @return the path to the created directories
   */
  public static Path createDirectoriesToFile(String filePath) {
    try {
      Path path = Path.of(filePath);
      Files.createDirectories(path.getParent());
      return path;
    } catch (IOException e) {
      throw new RuntimeException("Failed to create directories", e);
    }
  }

  /**
   * Save data to a file
   *
   * @param filePath to the file to save
   * @param data to save to the file
   */
  public static void saveFile(String filePath, byte[] data) {
    try {
      Path path = createDirectoriesToFile(filePath);
      Files.write(path, data);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save file", e);
    }
  }

  /**
   * Read data from a file
   *
   * @param path to the file to read
   * @return the data read from the file
   */
  public static byte[] readFile(String path) {
    try {
      return Files.readAllBytes(Path.of(path));
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file", e);
    }
  }

  /**
   * Save encrypted data to a file
   *
   * @param path to the file to save
   * @param password to use for encryption
   * @param data to save to the file
   */
  public static void saveEncryptedFile(String path, String password, byte[] data) {
    byte[] salt = CryptoUtils.getRandomBytes(ENCRYPTION_BLOCK_SIZE);
    byte[] iv = CryptoUtils.getRandomBytes(ENCRYPTION_BLOCK_SIZE);

    SecretKey secretKey = CryptoUtils.getKeyFromPassword(password, salt);
    byte[] encryptedData = CryptoUtils.encryptCBC(data, secretKey, new IvParameterSpec(iv));

    byte[] encryptedDataWithSaltAndIv = ArrayUtils.concat(salt, iv, encryptedData);
    saveFile(path, encryptedDataWithSaltAndIv);
  }

  /**
   * Read encrypted data from a file
   *
   * @param path to the file to read
   * @param password to use for decryption
   * @return the decrypted data read from the file
   */
  public static String readEncryptedFile(String path, String password) {
    byte[] encryptedDataWithSaltAndIv = readFile(path);
    byte[] salt = ArrayUtils.subarray(encryptedDataWithSaltAndIv, 0, ENCRYPTION_BLOCK_SIZE);
    byte[] iv =
        ArrayUtils.subarray(
            encryptedDataWithSaltAndIv, ENCRYPTION_BLOCK_SIZE, ENCRYPTION_BLOCK_SIZE);
    byte[] encryptedData =
        ArrayUtils.subarray(
            encryptedDataWithSaltAndIv,
            2 * ENCRYPTION_BLOCK_SIZE,
            encryptedDataWithSaltAndIv.length - 2 * ENCRYPTION_BLOCK_SIZE);

    SecretKey secretKey = CryptoUtils.getKeyFromPassword(password, salt);
    byte[] data = CryptoUtils.decryptCBC(encryptedData, secretKey, new IvParameterSpec(iv));

    return new String(data);
  }
}

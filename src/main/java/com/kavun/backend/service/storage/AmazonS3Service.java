package com.kavun.backend.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * This interface provides operations available to Amazon S3.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public interface AmazonS3Service {

  /**
   * It stores the given file name in S3 and returns the key under which the file has been stored.
   *
   * @param uploadedFile The multipart file uploaded by the user
   * @param username The username for which to upload this file
   * @return The URL of the uploaded image
   * @throws IOException if any error comes up dealing with i/o operations
   * @throws InterruptedException if there are any interruptions
   */
  String storeProfileImage(final MultipartFile uploadedFile, final String username)
      throws IOException, InterruptedException;

  /**
   * It stores the given file name in S3 and returns the key under which the file has been stored.
   *
   * @param file The multipart file uploaded by the user
   * @param path The folder within which this file will be placed
   * @param fileName The file name eg. fileName.png
   * @return The profile image key
   * @throws IOException If something goes wrong with file handling
   * @throws InterruptedException if there are any interruptions
   */
  String storeFile(MultipartFile file, String path, String fileName)
      throws IOException, InterruptedException;

  /**
   * Return all files under the path given.
   *
   * @param path the path
   * @return list of files
   * @throws S3Exception if the path is not found
   */
  List<String> getFiles(String path);

  /**
   * Return a file for the path given.
   *
   * @param path the path
   * @return the file
   * @throws S3Exception if the path is not found
   * @throws IOException if any error comes up dealing with i/o operations
   */
  InputStream getFile(String path) throws IOException;

  /**
   * Pre-signed URLs allow formation of a signed URL for an Amazon S3 resource.
   *
   * @param key the key of the user as a path to image file
   * @return the pre-signed url
   */
  String generatePreSignedUrl(String key);

  /**
   * Rename the currentKey with the new key.
   *
   * @param currentKey the current key
   * @param newKey the new key
   * @return the updated key
   */
  String renameFile(String currentKey, String newKey);

  /**
   * Deletes the folder hosted on amazon s3 assigned to user.
   *
   * @param key the key of the user as a path to image file
   */
  void delete(String key);
}

package com.kavun.backend.service.storage.impl;

import com.kavun.config.properties.AwsProperties;
import com.kavun.constant.EnvConstants;
import com.kavun.constant.StorageConstants;
import com.kavun.exception.InvalidFileFormatException;
import com.kavun.exception.StorageException;
import com.kavun.shared.util.core.FileUtils;
import com.kavun.shared.util.core.ValidationUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * This class provides working/production-ready operations available to Amazon S3.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile({EnvConstants.PRODUCTION, EnvConstants.INTEGRATION_TEST_CI, EnvConstants.INTEGRATION_TEST})
public class AmazonS3ServiceImpl extends AbstractAmazonS3Service {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final AwsProperties properties;

  @Override
  @CircuitBreaker(name = "s3Storage", fallbackMethod = "storeFileFallback")
  public String storeFile(MultipartFile file, String path, String fileName)
      throws IOException, InterruptedException {
    ValidationUtils.validateInputs(file, path, fileName);

    if (file.isEmpty()) {
      LOG.debug(StorageConstants.MULTI_PART_FILE_IS_EMPTY);
      throw new InvalidFileFormatException(StorageConstants.MULTI_PART_FILE_IS_EMPTY);
    }
    File image = multipartToFile(file);
    if (Objects.nonNull(ImageIO.read(image))) {
      LOG.debug("MultipartFile is an image and a resize will be done accordingly.");
      FileUtils.resize600(image);
    }
    String imageUrl = storeFileToS3(image, path, fileName, s3Client, properties);
    if (imageUrl != null && image.exists() && Files.deleteIfExists(image.toPath())) {
      LOG.debug("Image successfully deleted!");
    }
    return imageUrl;
  }

  @Override
  @CircuitBreaker(name = "s3Storage", fallbackMethod = "getFilesFallback")
  public List<String> getFiles(String path) {
    Objects.requireNonNull(path, StorageConstants.PATH_CANNOT_BE_NULL);

    List<String> files = new ArrayList<>();
    ListObjectsV2Request request =
        ListObjectsV2Request.builder().bucket(properties.getS3BucketName()).prefix(path).build();

    ListObjectsV2Response response = s3Client.listObjectsV2(request);
    response.contents().forEach(object -> files.add(object.key()));

    return files;
  }

  @Override
  @CircuitBreaker(name = "s3Storage", fallbackMethod = "getFileFallback")
  public InputStream getFile(String path) throws IOException {
    Objects.requireNonNull(path, StorageConstants.PATH_CANNOT_BE_NULL);

    GetObjectRequest request =
        GetObjectRequest.builder().bucket(properties.getS3BucketName()).key(path).build();

    return s3Client.getObject(request);
  }

  @Override
  @CircuitBreaker(name = "s3Storage", fallbackMethod = "generatePreSignedUrlFallback")
  public String generatePreSignedUrl(String key) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(properties.getS3BucketName()).key(key).build();

    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofDays(StorageConstants.PRE_SIGNED_URL_DAYS_TO_EXPIRE))
            .getObjectRequest(getObjectRequest)
            .build();

    return s3Presigner.presignGetObject(presignRequest).url().toString();
  }

  @Override
  @CircuitBreaker(name = "s3Storage", fallbackMethod = "renameFileFallback")
  public String renameFile(String currentKey, String newKey) {
    ValidationUtils.validateInputs(currentKey, newKey);

    String updatedNewKeyPath = newKey.replace("\\", "/");
    String bucketName = properties.getS3BucketName();

    CopyObjectRequest copyRequest =
        CopyObjectRequest.builder()
            .sourceBucket(bucketName)
            .sourceKey(currentKey)
            .destinationBucket(bucketName)
            .destinationKey(updatedNewKeyPath)
            .build();

    s3Client.copyObject(copyRequest);
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(currentKey).build());

    return updatedNewKeyPath;
  }

  @Override
  @CircuitBreaker(name = "s3Storage", fallbackMethod = "deleteFallback")
  public void delete(String key) {
    ValidationUtils.validateInputs(key);

    String bucketName = properties.getS3BucketName();

    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());

    LOG.debug("Object successfully deleted from bucket {} and key {}", bucketName, key);
  }

  // =========================================================================
  // CIRCUIT BREAKER FALLBACKS
  // =========================================================================

  private String storeFileFallback(MultipartFile file, String path, String fileName, Throwable t) {
    LOG.error("S3 storeFile circuit breaker fallback triggered for path {}: {}", path, t.getMessage(), t);
    throw new StorageException("S3 storage is currently unavailable. Please try again later.", t);
  }

  private List<String> getFilesFallback(String path, Throwable t) {
    LOG.error("S3 getFiles circuit breaker fallback triggered for path {}: {}", path, t.getMessage(), t);
    throw new StorageException("S3 storage is currently unavailable. Please try again later.", t);
  }

  private InputStream getFileFallback(String path, Throwable t) {
    LOG.error("S3 getFile circuit breaker fallback triggered for path {}: {}", path, t.getMessage(), t);
    throw new StorageException("S3 storage is currently unavailable. Please try again later.", t);
  }

  private String generatePreSignedUrlFallback(String key, Throwable t) {
    LOG.error("S3 generatePreSignedUrl circuit breaker fallback triggered for key {}: {}", key, t.getMessage(), t);
    throw new StorageException("S3 storage is currently unavailable. Please try again later.", t);
  }

  private String renameFileFallback(String currentKey, String newKey, Throwable t) {
    LOG.error("S3 renameFile circuit breaker fallback triggered for key {}: {}", currentKey, t.getMessage(), t);
    throw new StorageException("S3 storage is currently unavailable. Please try again later.", t);
  }

  private void deleteFallback(String key, Throwable t) {
    LOG.error("S3 delete circuit breaker fallback triggered for key {}: {}", key, t.getMessage(), t);
    throw new StorageException("S3 storage is currently unavailable. Please try again later.", t);
  }
}

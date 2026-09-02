package com.turing.app.api.document.storage;

import com.turing.app.api.document.config.StorageProperties;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import java.io.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true")
@EnableConfigurationProperties(StorageProperties.class)
public class MinioObjectStorage implements ObjectStorage {
  private final MinioClient client;
  private final String bucket;

  public MinioObjectStorage(StorageProperties properties) {
    this.client =
        MinioClient.builder()
            .endpoint(properties.endpoint())
            .credentials(properties.accessKey(), properties.secretKey())
            .build();
    this.bucket = properties.bucket();
  }

  @PostConstruct
  void ensureBucket() {
    try {
      if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
        client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    } catch (Exception exception) {
      throw new StorageException("Document bucket could not be initialized.", exception);
    }
  }

  public void put(String key, byte[] content, String contentType) {
    try (InputStream input = new ByteArrayInputStream(content)) {
      client.putObject(
          PutObjectArgs.builder().bucket(bucket).object(key).contentType(contentType).stream(
                  input, (long) content.length, -1L)
              .build());
    } catch (Exception exception) {
      throw new StorageException("Object could not be stored.", exception);
    }
  }

  public byte[] get(String key) {
    try (InputStream input =
        client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
      return input.readAllBytes();
    } catch (Exception exception) {
      throw new StorageException("Object could not be read.", exception);
    }
  }

  public void delete(String key) {
    try {
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
    } catch (Exception exception) {
      throw new StorageException("Object could not be deleted.", exception);
    }
  }
}

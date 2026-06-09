package com.sealhackathon.api.storage;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.net.URI;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "minio")
public class MinioObjectStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public MinioObjectStorageService(StorageProperties properties) {
        StorageProperties.Minio cfg = properties.getMinio();
        this.bucket = cfg.getBucket();
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(cfg.getEndpoint()))
                .region(Region.of(cfg.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey())))
                .forcePathStyle(true)
                .build();
    }

    @PostConstruct
    void ensureBucketOnStartup() {
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException ex) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {
                // race với minio-init container
            }
        }
    }

    @Override
    public void put(String key, InputStream stream, String contentType, long sizeBytes) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromInputStream(stream, sizeBytes));
    }

    @Override
    public StoredObject get(String key) {
        try {
            var response = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
            String contentType = response.response().contentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            long size = response.response().contentLength() != null ? response.response().contentLength() : 0L;
            return new StoredObject(response, contentType, size);
        } catch (NoSuchKeyException ex) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy file");
        }
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        }
    }
}

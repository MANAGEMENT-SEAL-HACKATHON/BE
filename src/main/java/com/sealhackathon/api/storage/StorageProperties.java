package com.sealhackathon.api.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** minio | local */
    private String type = "local";

    private int submissionSlideMaxMb = 25;

    private String localDir = "uploads/submissions";

    private Minio minio = new Minio();

    @Getter
    @Setter
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket = "seal-submissions";
        private String region = "us-east-1";
    }
}

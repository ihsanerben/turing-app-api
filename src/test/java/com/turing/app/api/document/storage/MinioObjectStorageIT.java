package com.turing.app.api.document.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.turing.app.api.document.config.StorageProperties;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MinioObjectStorageIT {
    private static final String ACCESS_KEY = "turing-minio";
    private static final String SECRET_KEY = "turing-minio-secret";

    @Container
    static GenericContainer<?> minio = new GenericContainer<>("minio/minio:latest")
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server", "/data")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    @Test
    void storesReadsAndDeletesPrivateObject() {
        StorageProperties properties = new StorageProperties(
                true,
                "http://" + minio.getHost() + ":" + minio.getMappedPort(9000),
                ACCESS_KEY,
                SECRET_KEY,
                "turing-documents-test"
        );
        MinioObjectStorage storage = new MinioObjectStorage(properties);
        storage.ensureBucket();
        byte[] content = "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII);

        storage.put("applications/app/requirement/file", content, "application/pdf");
        assertThat(storage.get("applications/app/requirement/file")).isEqualTo(content);

        storage.delete("applications/app/requirement/file");
        assertThatThrownBy(() -> storage.get("applications/app/requirement/file"))
                .isInstanceOf(StorageException.class);
    }
}

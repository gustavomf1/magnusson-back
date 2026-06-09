package com.magnossao.config;

import org.springframework.stereotype.Service;
import com.magnossao.service.StorageService;
import com.magnossao.service.StorageService.PresignedUploadResult;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Service
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final StorageProperties props;

    public S3StorageService(StorageProperties props) {
        this.props = props;

        StaticCredentialsProvider creds = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
        );

        S3ClientBuilder clientBuilder = S3Client.builder()
            .credentialsProvider(creds)
            .region(Region.of(props.getRegion()));

        Builder presignerBuilder = S3Presigner.builder()
            .credentialsProvider(creds)
            .region(Region.of(props.getRegion()));

        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            clientBuilder.endpointOverride(URI.create(props.getEndpoint())).forcePathStyle(true);
        }
        presignerBuilder.serviceConfiguration(
            S3Configuration.builder().pathStyleAccessEnabled(true).build());
        if (props.getPresignEndpoint() != null && !props.getPresignEndpoint().isBlank()) {
            presignerBuilder.endpointOverride(URI.create(props.getPresignEndpoint()));
        }

        this.s3Client = clientBuilder.build();
        this.presigner = presignerBuilder.build();
    }

    @Override
    public PresignedUploadResult gerarUrlUpload(String chave, String contentType) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(props.getBucket())
            .key(chave)
            .contentType(contentType)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(5))
            .putObjectRequest(putRequest)
            .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        String urlPublica = props.getPublicBaseUrl() + "/" + chave;
        return new PresignedUploadResult(presigned.url().toString(), chave, urlPublica);
    }

    @Override
    public void deletar(String chave) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(props.getBucket())
            .key(chave)
            .build());
    }
}

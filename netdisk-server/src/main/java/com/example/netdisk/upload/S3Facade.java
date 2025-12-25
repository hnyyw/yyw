package com.example.netdisk.upload;

import com.example.netdisk.config.NetdiskProperties;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;

@Component
@RequiredArgsConstructor
public class S3Facade {
  private final S3Client s3;
  private final S3Presigner presigner;
  private final NetdiskProperties props;

  public String createMultipartUpload(String key) {
    CreateMultipartUploadResponse resp =
        s3.createMultipartUpload(
            CreateMultipartUploadRequest.builder().bucket(props.getS3().getBucket()).key(key).build());
    return resp.uploadId();
  }

  public String presignUploadPartUrl(String key, String s3UploadId, int partNumber, long expireSeconds) {
    UploadPartRequest req =
        UploadPartRequest.builder()
            .bucket(props.getS3().getBucket())
            .key(key)
            .uploadId(s3UploadId)
            .partNumber(partNumber)
            .build();
    PresignedUploadPartRequest presigned =
        presigner.presignUploadPart(b -> b.signatureDuration(Duration.ofSeconds(expireSeconds)).uploadPartRequest(req));
    return presigned.url().toString();
  }

  public List<Part> listUploadedParts(String key, String s3UploadId) {
    ListPartsResponse resp =
        s3.listParts(
            ListPartsRequest.builder()
                .bucket(props.getS3().getBucket())
                .key(key)
                .uploadId(s3UploadId)
                .build());
    return resp.parts();
  }

  public void completeMultipartUpload(String key, String s3UploadId, List<CompletedPart> parts) {
    CompletedMultipartUpload cmu = CompletedMultipartUpload.builder().parts(parts).build();
    s3.completeMultipartUpload(
        CompleteMultipartUploadRequest.builder()
            .bucket(props.getS3().getBucket())
            .key(key)
            .uploadId(s3UploadId)
            .multipartUpload(cmu)
            .build());
  }

  public void abortMultipartUpload(String key, String s3UploadId) {
    s3.abortMultipartUpload(
        AbortMultipartUploadRequest.builder()
            .bucket(props.getS3().getBucket())
            .key(key)
            .uploadId(s3UploadId)
            .build());
  }

  public String presignDownloadUrl(String key, long expireSeconds) {
    GetObjectRequest req = GetObjectRequest.builder().bucket(props.getS3().getBucket()).key(key).build();
    PresignedGetObjectRequest presigned =
        presigner.presignGetObject(b -> b.signatureDuration(Duration.ofSeconds(expireSeconds)).getObjectRequest(req));
    return presigned.url().toString();
  }
}


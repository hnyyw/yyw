package com.example.netdisk.config;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
public class S3Config {
  private final NetdiskProperties props;

  @Bean
  public S3Client s3Client() {
    var s3Props = props.getS3();
    var creds = AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey());
    var builder =
        S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .region(Region.of(s3Props.getRegion()))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3Props.isPathStyleAccess()).build());
    if (s3Props.getEndpoint() != null && !s3Props.getEndpoint().isBlank()) {
      builder = builder.endpointOverride(URI.create(s3Props.getEndpoint()));
    }
    return builder.build();
  }

  @Bean
  public S3Presigner s3Presigner() {
    var s3Props = props.getS3();
    var creds = AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey());
    var builder =
        S3Presigner.builder()
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .region(Region.of(s3Props.getRegion()));
    if (s3Props.getEndpoint() != null && !s3Props.getEndpoint().isBlank()) {
      builder = builder.endpointOverride(URI.create(s3Props.getEndpoint()));
    }
    return builder.build();
  }
}

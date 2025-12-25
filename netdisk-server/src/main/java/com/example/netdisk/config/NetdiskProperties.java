package com.example.netdisk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "netdisk")
public class NetdiskProperties {
  private Jwt jwt = new Jwt();
  private Presign presign = new Presign();
  private Upload upload = new Upload();
  private S3 s3 = new S3();

  @Data
  public static class Jwt {
    private String issuer;
    private String secret;
    private long accessTokenTtlSeconds = 7200;
  }

  @Data
  public static class Presign {
    private long uploadPartExpireSeconds = 900;
    private long downloadExpireSeconds = 300;
    private long shareTokenTtlSeconds = 7200;
  }

  @Data
  public static class Upload {
    private long partSizeDefaultBytes = 16L * 1024 * 1024;
    private long partSizeMinBytes = 8L * 1024 * 1024;
    private long partSizeMaxBytes = 64L * 1024 * 1024;
    private long sessionTtlMinutes = 60;
  }

  @Data
  public static class S3 {
    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private boolean pathStyleAccess = true;
  }
}

package com.example.netdisk.download;

import com.example.netdisk.auth.AuthUtil;
import com.example.netdisk.common.ApiResponse;
import com.example.netdisk.common.BizException;
import com.example.netdisk.config.NetdiskProperties;
import com.example.netdisk.dao.BlobRepository;
import com.example.netdisk.dao.EntryRepository;
import com.example.netdisk.domain.entity.Entry;
import com.example.netdisk.upload.S3Facade;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DownloadController {
  private final EntryRepository entryRepository;
  private final BlobRepository blobRepository;
  private final S3Facade s3;
  private final NetdiskProperties props;

  @PostMapping("/files/{entryId}/download-link")
  public ApiResponse<Map<String, Object>> downloadLink(@PathVariable("entryId") long entryId) {
    long userId = AuthUtil.requirePrincipal().userId();
    Entry e =
        entryRepository.findByIdAndUserId(entryId, userId).orElseThrow(() -> BizException.notFound("文件不存在"));
    if (e.getIsDeleted() != null && e.getIsDeleted() == 1) {
      throw BizException.conflict("文件在回收站中");
    }
    if (e.getType() == null || e.getType() != 2 || e.getBlobId() == null) {
      throw BizException.badRequest("不是文件，无法下载");
    }
    var blob = blobRepository.findById(e.getBlobId()).orElseThrow(() -> BizException.notFound("内容不存在"));
    String url = s3.presignDownloadUrl(blob.getStorageKey(), props.getPresign().getDownloadExpireSeconds());
    return ApiResponse.ok(Map.of("url", url, "expire_in", props.getPresign().getDownloadExpireSeconds()));
  }
}


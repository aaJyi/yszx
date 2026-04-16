package com.android.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.android.config.AliyunOssProperties;
import com.android.service.IAliyunOssService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AliyunOssServiceImpl implements IAliyunOssService {

    private final AliyunOssProperties props;

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        validateConfig();

        String ext = getExtension(file.getOriginalFilename());
        String datePath = LocalDate.now().toString().replace("-", "/");
        String objectName = (StringUtils.hasText(folder) ? folder.trim() : "uploads")
                + "/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        OSS ossClient = null;
        try (InputStream in = file.getInputStream()) {
            ossClient = new OSSClientBuilder().build(
                    props.getEndpoint(),
                    props.getAccessKeyId(),
                    props.getAccessKeySecret()
            );

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (StringUtils.hasText(file.getContentType())) {
                metadata.setContentType(file.getContentType());
            }

            ossClient.putObject(props.getBucketName(), objectName, in, metadata);
            return buildPublicUrl(objectName);
        } catch (Exception e) {
            throw new RuntimeException("上传 OSS 失败: " + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(props.getEndpoint())
                || !StringUtils.hasText(props.getAccessKeyId())
                || !StringUtils.hasText(props.getAccessKeySecret())
                || !StringUtils.hasText(props.getBucketName())) {
            throw new RuntimeException("OSS 配置不完整，请先在 application.yml 配置 aliyun.oss");
        }
    }

    private String buildPublicUrl(String objectName) {
        if (StringUtils.hasText(props.getPublicUrlPrefix())) {
            return props.getPublicUrlPrefix().replaceAll("/+$", "") + "/" + objectName;
        }
        String endpoint = props.getEndpoint().replace("https://", "").replace("http://", "");
        return "https://" + props.getBucketName() + "." + endpoint + "/" + objectName;
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}


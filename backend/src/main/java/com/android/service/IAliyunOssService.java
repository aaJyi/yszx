package com.android.service;

import org.springframework.web.multipart.MultipartFile;

public interface IAliyunOssService {
    String uploadImage(MultipartFile file, String folder);
}


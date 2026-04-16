package com.android.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 原始健康数据元信息（不含二进制）
 */
@Data
@Builder
public class RawHealthDataMetaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String dataType;
    private String formatType;
    private String fileName;
    private Long fileSize;
    private LocalDateTime uploadTime;
}

package com.android.dto;

import lombok.Data;

import java.util.List;

@Data
public class ClubDivisionImgManifestVO {
    /** 浏览器访问前缀，如 /club-division-img */
    private String baseUrl;
    /** 排序后的文件名：优先 community_network.png，其次 dashboard.png 等 */
    private List<String> files;
    /** 最新图片修改时间（毫秒），用于前端强制刷新 */
    private Long updatedAtMs;
}

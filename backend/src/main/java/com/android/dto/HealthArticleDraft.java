package com.android.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 从外部资讯源拉取的单条条目（入库前）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthArticleDraft {
    private String title;
    private String summary;
    private String coverUrl;
    private String articleUrl;

    /**
     * 是否允许写入知识库：排除「无 RSS 时的搜索聚合页」等占位条目（与 Python CLI 只输出真实 RSS 条一致）。
     */
    public boolean isPersistableKnowledgeEntry() {
        if (articleUrl == null || articleUrl.isBlank()) {
            return false;
        }
        String u = articleUrl.trim().toLowerCase();
        if (u.contains("news.google.com/search")) {
            return false;
        }
        String t = title != null ? title.trim() : "";
        if (t.startsWith("在 Google 新闻中搜索")) {
            return false;
        }
        return true;
    }
}

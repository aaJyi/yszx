package com.android.service;

import com.android.dto.AdminKnowledgeArticleRowVO;
import com.android.dto.HealthArticleDraft;
import com.android.dto.PageResultVO;
import com.android.dto.SocialArticleItemVO;

import java.util.List;

public interface IHealthKnowledgeArticleService {

    /** 写入知识库，返回本次新增条数（已存在 URL 则跳过） */
    int upsertFromDrafts(String keyword, List<HealthArticleDraft> drafts);

    /** 按用户推断疾病关键词匹配知识库；无疾病时返回近期通用资讯 */
    List<SocialArticleItemVO> listFeedForUser(Long userId);

    PageResultVO<AdminKnowledgeArticleRowVO> pageAdmin(long pageNum, long pageSize, String keywordFilter);

    /** 供定时任务：用户库中去重后的疾病名，作补充关键词 */
    List<String> listDistinctUserDiseaseKeywords(int limit);
}

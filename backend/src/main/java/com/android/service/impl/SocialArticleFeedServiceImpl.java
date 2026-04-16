package com.android.service.impl;

import com.android.dto.HealthArticleDraft;
import com.android.dto.SocialArticleItemVO;
import com.android.entity.UserInferredDisease;
import com.android.mapper.UserInferredDiseaseMapper;
import com.android.service.IHealthArticleFetchService;
import com.android.service.IHealthKnowledgeArticleService;
import com.android.service.ISocialArticleFeedService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialArticleFeedServiceImpl implements ISocialArticleFeedService {

    private final UserInferredDiseaseMapper userInferredDiseaseMapper;
    private final IHealthArticleFetchService healthArticleFetchService;
    private final IHealthKnowledgeArticleService healthKnowledgeArticleService;

    @Value("${health.news.max-per-keyword:5}")
    private int maxPerKeyword;

    @Override
    public List<SocialArticleItemVO> listArticles(Long userId) {
        return healthKnowledgeArticleService.listFeedForUser(userId);
    }

    /**
     * 立即按当前推断疾病拉取并写入全局知识库（与定时任务共用去重逻辑）；不再写入 social_toutiao_article。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshArticles(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        List<UserInferredDisease> diseases = userInferredDiseaseMapper.selectList(
                new LambdaQueryWrapper<UserInferredDisease>()
                        .eq(UserInferredDisease::getUserId, userId)
                        .orderByDesc(UserInferredDisease::getCreatedAt));
        if (diseases.isEmpty()) {
            return 0;
        }

        int total = 0;
        int cap = Math.max(1, Math.min(maxPerKeyword, 20));

        for (UserInferredDisease d : diseases) {
            String kw = d.getDiseaseName();
            if (kw == null || kw.trim().isEmpty()) {
                continue;
            }
            List<HealthArticleDraft> drafts = healthArticleFetchService.fetchByKeyword(kw.trim(), cap);
            total += healthKnowledgeArticleService.upsertFromDrafts(kw.trim(), drafts);
        }
        return total;
    }
}

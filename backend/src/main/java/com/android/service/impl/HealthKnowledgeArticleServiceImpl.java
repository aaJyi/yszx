package com.android.service.impl;

import com.android.dto.AdminKnowledgeArticleRowVO;
import com.android.dto.HealthArticleDraft;
import com.android.dto.PageResultVO;
import com.android.dto.SocialArticleItemVO;
import com.android.entity.HealthKnowledgeArticle;
import com.android.entity.UserInferredDisease;
import com.android.mapper.HealthKnowledgeArticleMapper;
import com.android.mapper.UserInferredDiseaseMapper;
import com.android.service.IHealthKnowledgeArticleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthKnowledgeArticleServiceImpl implements IHealthKnowledgeArticleService {

    private final HealthKnowledgeArticleMapper healthKnowledgeArticleMapper;
    private final UserInferredDiseaseMapper userInferredDiseaseMapper;

    @Value("${health.knowledge.feed-limit-matched:100}")
    private int feedLimitMatched;

    @Value("${health.knowledge.feed-limit-general:30}")
    private int feedLimitGeneral;

    @Override
    public int upsertFromDrafts(String keyword, List<HealthArticleDraft> drafts) {
        if (keyword == null || keyword.trim().isEmpty() || drafts == null || drafts.isEmpty()) {
            return 0;
        }
        String kw = keyword.trim();
        int n = 0;
        LocalDateTime now = LocalDateTime.now();
        for (HealthArticleDraft d : drafts) {
            if (d == null || !StringUtils.hasText(d.getArticleUrl()) || !d.isPersistableKnowledgeEntry()) {
                continue;
            }
            String url = d.getArticleUrl().trim();
            String hash = sha256Hex(url);
            Long exists = healthKnowledgeArticleMapper.selectCount(
                    new LambdaQueryWrapper<HealthKnowledgeArticle>()
                            .eq(HealthKnowledgeArticle::getUrlHash, hash));
            if (exists != null && exists > 0) {
                continue;
            }
            HealthKnowledgeArticle row = new HealthKnowledgeArticle();
            row.setKeyword(kw);
            row.setTitle(StringUtils.hasText(d.getTitle()) ? d.getTitle().trim() : "无标题");
            row.setSummary(d.getSummary());
            row.setCoverUrl(d.getCoverUrl());
            row.setArticleUrl(url);
            row.setUrlHash(hash);
            row.setFetchedAt(now);
            healthKnowledgeArticleMapper.insert(row);
            n++;
        }
        return n;
    }

    @Override
    public List<SocialArticleItemVO> listFeedForUser(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<UserInferredDisease> diseases = userInferredDiseaseMapper.selectList(
                new LambdaQueryWrapper<UserInferredDisease>()
                        .eq(UserInferredDisease::getUserId, userId)
                        .orderByDesc(UserInferredDisease::getCreatedAt));
        Set<String> names = diseases.stream()
                .map(UserInferredDisease::getDiseaseName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<HealthKnowledgeArticle> rows;
        if (names.isEmpty()) {
            rows = healthKnowledgeArticleMapper.selectList(
                    new LambdaQueryWrapper<HealthKnowledgeArticle>()
                            .orderByDesc(HealthKnowledgeArticle::getFetchedAt)
                            .last("LIMIT " + Math.max(1, feedLimitGeneral)));
        } else {
            rows = healthKnowledgeArticleMapper.selectList(
                    new LambdaQueryWrapper<HealthKnowledgeArticle>()
                            .in(HealthKnowledgeArticle::getKeyword, names)
                            .orderByDesc(HealthKnowledgeArticle::getFetchedAt)
                            .last("LIMIT " + Math.max(1, feedLimitMatched)));
            if (rows.isEmpty()) {
                rows = healthKnowledgeArticleMapper.selectList(
                        new LambdaQueryWrapper<HealthKnowledgeArticle>()
                                .orderByDesc(HealthKnowledgeArticle::getFetchedAt)
                                .last("LIMIT " + Math.max(1, feedLimitGeneral)));
            }
        }
        return rows.stream().map(this::toFeedVo).collect(Collectors.toList());
    }

    @Override
    public PageResultVO<AdminKnowledgeArticleRowVO> pageAdmin(long pageNum, long pageSize, String keywordFilter) {
        long pn = Math.max(1, pageNum);
        long ps = Math.min(100, Math.max(1, pageSize));
        Page<HealthKnowledgeArticle> page = new Page<>(pn, ps);
        LambdaQueryWrapper<HealthKnowledgeArticle> q = new LambdaQueryWrapper<HealthKnowledgeArticle>()
                .orderByDesc(HealthKnowledgeArticle::getFetchedAt);
        if (StringUtils.hasText(keywordFilter)) {
            q.like(HealthKnowledgeArticle::getKeyword, keywordFilter.trim());
        }
        Page<HealthKnowledgeArticle> out = healthKnowledgeArticleMapper.selectPage(page, q);
        List<AdminKnowledgeArticleRowVO> records = out.getRecords().stream()
                .map(this::toAdminVo)
                .collect(Collectors.toList());
        return new PageResultVO<>(records, out.getTotal(), out.getCurrent(), out.getSize());
    }

    @Override
    public List<String> listDistinctUserDiseaseKeywords(int limit) {
        int cap = Math.max(1, Math.min(limit, 200));
        List<UserInferredDisease> rows = userInferredDiseaseMapper.selectList(
                new LambdaQueryWrapper<UserInferredDisease>()
                        .select(UserInferredDisease::getDiseaseName)
                        .groupBy(UserInferredDisease::getDiseaseName)
                        .last("LIMIT " + cap));
        return rows.stream()
                .map(UserInferredDisease::getDiseaseName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private SocialArticleItemVO toFeedVo(HealthKnowledgeArticle e) {
        SocialArticleItemVO vo = new SocialArticleItemVO();
        vo.setId(e.getId());
        vo.setKeyword(e.getKeyword());
        vo.setTitle(e.getTitle());
        vo.setSummary(e.getSummary());
        vo.setCoverUrl(e.getCoverUrl());
        vo.setArticleUrl(e.getArticleUrl());
        return vo;
    }

    private AdminKnowledgeArticleRowVO toAdminVo(HealthKnowledgeArticle e) {
        AdminKnowledgeArticleRowVO vo = new AdminKnowledgeArticleRowVO();
        vo.setId(e.getId());
        vo.setKeyword(e.getKeyword());
        vo.setTitle(e.getTitle());
        vo.setSummary(e.getSummary());
        vo.setCoverUrl(e.getCoverUrl());
        vo.setArticleUrl(e.getArticleUrl());
        vo.setFetchedAt(e.getFetchedAt());
        return vo;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

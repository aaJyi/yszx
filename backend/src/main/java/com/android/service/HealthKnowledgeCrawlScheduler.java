package com.android.service;

import com.android.dto.HealthArticleDraft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 后台常驻：仅按 {@code health_crawl_keyword} 表中已启用的关键词周期性拉取资讯写入知识库（URL 去重）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "health.knowledge.crawler-enabled", havingValue = "true", matchIfMissing = true)
public class HealthKnowledgeCrawlScheduler {

    private final IHealthArticleFetchService healthArticleFetchService;
    private final IHealthKnowledgeArticleService healthKnowledgeArticleService;
    private final IHealthCrawlKeywordService healthCrawlKeywordService;

    @Value("${health.knowledge.max-per-keyword:8}")
    private int maxPerKeyword;

    @Value("${health.knowledge.pause-ms-between-keywords:1500}")
    private long pauseMsBetweenKeywords;

    @Scheduled(
            initialDelayString = "${health.knowledge.crawl-initial-delay-ms:120000}",
            fixedDelayString = "${health.knowledge.crawl-interval-ms:600000}")
    public void crawl() {
        Set<String> keywords = new LinkedHashSet<>();
        try {
            for (String k : healthCrawlKeywordService.resolveSeedKeywordsForCrawl()) {
                if (StringUtils.hasText(k)) {
                    keywords.add(k.trim());
                }
            }
        } catch (Exception e) {
            log.warn("读取爬取关键词表失败: {}", e.getMessage());
        }

        if (keywords.isEmpty()) {
            log.warn("知识库定时抓取未执行：health_crawl_keyword 表中没有「已启用」的关键词，请在管理端配置或执行建表 SQL 中的种子数据");
        }

        int cap = Math.max(1, Math.min(maxPerKeyword, 20));
        int totalNew = 0;
        for (String kw : keywords) {
            try {
                List<HealthArticleDraft> drafts = healthArticleFetchService.fetchByKeyword(kw, cap);
                if (drafts.isEmpty()) {
                    log.info("知识库抓取 keyword={} Python 返回 0 条：多为 RSS 标题/摘要中不含该词，或 8095 服务未启动/请求失败（见 healthArticleFetch 的 WARN）", kw);
                }
                int n = healthKnowledgeArticleService.upsertFromDrafts(kw, drafts);
                totalNew += n;
                if (!drafts.isEmpty() && n == 0) {
                    log.info("知识库抓取 keyword={} 拉取 {} 条但均为库中已有 URL（去重跳过）", kw, drafts.size());
                } else {
                    log.debug("知识库抓取 keyword={} 新增={}", kw, n);
                }
            } catch (Exception e) {
                log.warn("知识库抓取失败 keyword={}: {}", kw, e.getMessage());
            }
            if (pauseMsBetweenKeywords > 0) {
                try {
                    Thread.sleep(pauseMsBetweenKeywords);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("知识库定时抓取完成，本轮累计新增 {} 条（其余为已存在 URL 跳过）", totalNew);
    }
}

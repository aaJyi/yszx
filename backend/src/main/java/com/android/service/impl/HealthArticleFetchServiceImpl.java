package com.android.service.impl;

import com.android.dto.HealthArticleDraft;
import com.android.service.IHealthArticleFetchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 健康资讯拉取：仅调用本机 Python 服务（{@code backend/scripts} 下 RSS 聚合，与 CLI 一致），
 * 不在此实现任何外网 RSS/HTML 爬取，避免与 Python 环境行为不一致及网络策略问题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthArticleFetchServiceImpl implements IHealthArticleFetchService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${health.news.python-service.enabled:true}")
    private boolean pythonServiceEnabled;

    @Value("${health.news.python-service.base-url:http://127.0.0.1:8095}")
    private String pythonServiceBaseUrl;

    @Override
    public List<HealthArticleDraft> fetchByKeyword(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (!pythonServiceEnabled || !StringUtils.hasText(pythonServiceBaseUrl)) {
            log.warn("健康资讯 Python 服务未启用或未配置 health.news.python-service.base-url，跳过拉取");
            return Collections.emptyList();
        }
        String k = keyword.trim();
        int cap = Math.max(1, Math.min(limit, 30));

        String base = pythonServiceBaseUrl.trim().replaceAll("/+$", "");
        // 手写 query，避免 UriComponentsBuilder 与 RestTemplate 组合时对中文二次编码（%E9 -> %25E9）
        String url = base + "/articles?keyword=" + URLEncoder.encode(k, StandardCharsets.UTF_8) + "&limit=" + cap;

        try {
            // Python 返回 application/json 数组：勿用 getForEntity(..., String/byte[])，否则 Jackson 会当 JSON 反序列化
            URI uri = URI.create(url);
            byte[] raw = restTemplate.execute(uri, HttpMethod.GET, null, clientHttpResponse -> {
                if (!clientHttpResponse.getStatusCode().is2xxSuccessful()) {
                    return null;
                }
                return StreamUtils.copyToByteArray(clientHttpResponse.getBody());
            });
            if (raw == null || raw.length == 0) {
                return Collections.emptyList();
            }
            String body = new String(raw, StandardCharsets.UTF_8);
            return parseJsonArray(body);
        } catch (Exception e) {
            log.warn("Python 资讯服务调用失败 keyword={} url={} msg={}", k, url, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<HealthArticleDraft> parseJsonArray(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray()) {
                return Collections.emptyList();
            }
            List<HealthArticleDraft> out = new ArrayList<>();
            for (JsonNode n : root) {
                if (n == null || !n.isObject()) {
                    continue;
                }
                String title = text(n, "title");
                String summary = text(n, "summary");
                String coverUrl = text(n, "coverUrl");
                String articleUrl = text(n, "articleUrl");
                if (!StringUtils.hasText(articleUrl)) {
                    continue;
                }
                if (!StringUtils.hasText(title)) {
                    title = "无标题";
                }
                out.add(new HealthArticleDraft(title.trim(), summary, coverUrl, articleUrl.trim()));
            }
            return out;
        } catch (Exception e) {
            log.warn("解析 Python 资讯 JSON 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        return v.isTextual() ? v.asText() : v.toString();
    }
}

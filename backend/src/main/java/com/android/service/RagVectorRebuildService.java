package com.android.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过 HTTP 调用 ai-doctor-rag 重建向量（避免本地路径依赖）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagVectorRebuildService {

    private final RestTemplate restTemplate;

    @Value("${ai-doctor-rag.rebuild-url:}")
    private String rebuildUrl;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Async
    public void rebuildAsync(String dataset) {
        if (!StringUtils.hasText(rebuildUrl)) {
            log.warn("[RAG向量] 未配置 ai-doctor-rag.rebuild-url，跳过重建");
            return;
        }
        DbInfo db = parseDatasourceUrl(datasourceUrl);
        if (db == null) {
            log.warn("[RAG向量] 无法解析 spring.datasource.url={}", datasourceUrl);
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("dataset", dataset);
            body.put("dbHost", db.host);
            body.put("dbPort", db.port);
            body.put("dbName", db.database);
            body.put("dbUser", datasourceUsername);
            body.put("dbPassword", datasourcePassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            log.info("[RAG向量] 请求重建: {} dataset={}", rebuildUrl, dataset);
            restTemplate.postForEntity(rebuildUrl.trim(), entity, String.class);
            log.info("[RAG向量] 已提交重建请求");
        } catch (Exception e) {
            log.error("[RAG向量] 执行失败", e);
        }
    }

    private DbInfo parseDatasourceUrl(String jdbcUrl) {
        if (!StringUtils.hasText(jdbcUrl)) {
            return null;
        }
        Pattern p = Pattern.compile("^jdbc:mysql://([^:/?]+):(\\d+)/([^?]+).*$");
        Matcher m = p.matcher(jdbcUrl.trim());
        if (!m.matches()) {
            return null;
        }
        return new DbInfo(m.group(1), Integer.parseInt(m.group(2)), m.group(3));
    }

    private static class DbInfo {
        private final String host;
        private final int port;
        private final String database;

        private DbInfo(String host, int port, String database) {
            this.host = host;
            this.port = port;
            this.database = database;
        }
    }
}

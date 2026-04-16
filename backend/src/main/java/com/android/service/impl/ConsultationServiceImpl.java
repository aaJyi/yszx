package com.android.service.impl;

import com.android.service.IConsultationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * 在线问诊服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-21
 */
@Slf4j
@Service
public class ConsultationServiceImpl implements IConsultationService {

    @Autowired
    private RestTemplate restTemplate;

    /**
     * WebClient for streaming support
     */
    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();
    /**
     * 智能体服务基础URL
     */
    @Value("${ai.agent.base.url:}")
    private String agentBaseUrl;

    @Override
    public String chat(Long userId, String question, Object history) {
        try {
            log.info("开始调用智能体对话接口，用户ID：{}，问题：{}", userId, question);

            // 构建请求数据
            // 根据用户需求：智能体会自己查询健康档案，不需要在请求体中包含健康档案数据
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("userId", userId);
            requestData.put("question", question);
            
            // 添加对话历史
            if (history != null) {
                requestData.put("history", history);
                log.info("包含对话历史上下文");
            }
            
            // 不包含用户健康数据，智能体会根据userId自行查询健康档案
            log.info("请求体中不包含健康档案数据，智能体将根据userId自行查询");

            // TODO: 智能体开发程序员 - 后续更新 ai.agent.base.url、determineChatUrl、请求/响应格式
            // 3. 确定智能体对话接口URL
            String chatEndpoint = determineChatUrl();

            // 4. 调用智能体对话接口
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestData, headers);

            // 记录请求详情（用于调试）
            try {
                // 使用配置好的ObjectMapper，支持LocalDateTime序列化
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                String requestJson = mapper.writeValueAsString(requestData);
                log.info("调用智能体对话接口：{}", chatEndpoint);
                log.info("请求方法：POST");
                log.info("请求头：Content-Type: application/json");
                log.info("请求体大小：{} 字符", requestJson.length());
                // 只记录请求体的前500字符，避免日志过长
                if (requestJson.length() > 500) {
                    log.info("请求体JSON（前500字符）：{}...", requestJson.substring(0, 500));
                } else {
                    log.info("请求体JSON：{}", requestJson);
                }
            } catch (Exception e) {
                log.warn("无法序列化请求数据为JSON：{}", e.getMessage());
                log.warn("序列化异常详情：", e);
            }

            // 记录请求开始时间
            long requestStartTime = System.currentTimeMillis();
            log.info("开始发送请求到智能体服务，时间戳：{}", requestStartTime);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    chatEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            // 记录请求完成时间
            long requestEndTime = System.currentTimeMillis();
            long requestDuration = requestEndTime - requestStartTime;
            log.info("收到智能体服务响应，耗时：{} 毫秒（{} 秒）", requestDuration, requestDuration / 1000.0);

            // 5. 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

                // 尝试多种可能的响应格式
                String answer = null;
                if (responseBody.containsKey("answer")) {
                    answer = String.valueOf(responseBody.get("answer"));
                } else if (responseBody.containsKey("data")) {
                    Object dataObj = responseBody.get("data");
                    if (dataObj instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        if (data.containsKey("answer")) {
                            answer = String.valueOf(data.get("answer"));
                        }
                    } else if (dataObj instanceof String) {
                        answer = (String) dataObj;
                    }
                } else if (responseBody.containsKey("message")) {
                    answer = String.valueOf(responseBody.get("message"));
                } else if (responseBody.containsKey("content")) {
                    answer = String.valueOf(responseBody.get("content"));
                }

                if (answer != null && !answer.trim().isEmpty()) {
                    log.info("智能体对话接口调用成功，返回回答长度：{}", answer.length());
                    log.info("回答内容: {}", answer);
                    return answer;
                } else {
                    log.warn("智能体对话接口返回的响应格式不符合预期，响应体：{}", responseBody);
                    log.warn("尝试从响应中提取所有可能的字段：{}", responseBody.keySet());
                    return "抱歉，我暂时无法理解您的问题，请换一种方式提问。";
                }
            } else {
                log.error("智能体对话接口调用失败，状态码：{}，响应：{}", response.getStatusCode(), response.getBody());
                return "抱歉，服务暂时不可用，请稍后再试。";
            }

        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 检查是否是超时异常
            if (e.getCause() instanceof java.net.SocketTimeoutException) {
                log.error("智能体服务响应超时（Read timed out），可能原因：1) 智能体服务处理时间过长；2) 响应体传输时间过长；3) 网络延迟。用户ID：{}，问题长度：{}字符", 
                        userId, question != null ? question.length() : 0, e);
                log.error("建议：1) 检查智能体服务性能；2) 检查网络连接；3) 考虑增加超时时间或实现异步处理。");
                return "抱歉，AI服务响应超时，可能是处理的数据量较大或网络传输较慢。请稍后再试或联系管理员。";
            } else {
                log.error("无法连接到智能体服务，请检查网络连接或服务地址配置。用户ID：{}，问题：{}", userId, question, e);
                return "抱歉，无法连接到AI服务，请检查网络连接或稍后再试。";
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("智能体对话接口返回客户端错误，状态码：{}，响应：{}", e.getStatusCode(), e.getResponseBodyAsString());
            return "抱歉，请求格式有误，请稍后再试。";
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("智能体对话接口返回服务器错误，状态码：{}，响应：{}", e.getStatusCode(), e.getResponseBodyAsString());
            return "抱歉，AI服务暂时繁忙，请稍后再试。";
        } catch (Exception e) {
            log.error("调用智能体对话接口时发生异常", e);
            return "抱歉，发生了未知错误，请稍后再试。";
        }
    }

    /**
     * 确定智能体对话接口的完整URL
     */
    private String determineChatUrl() {
        // 基础URL，拼接chat路径
        if (agentBaseUrl != null && !agentBaseUrl.trim().isEmpty()) {
            String baseUrl = agentBaseUrl.endsWith("/") ? agentBaseUrl.substring(0, agentBaseUrl.length() - 1) : agentBaseUrl;
            return baseUrl + "/chat";
        }

        // 默认使用本地或常见的智能体服务地址
        // 这里可以根据实际情况修改为智能体的实际服务地址
        log.warn("未配置智能体对话接口URL，使用默认地址。请在application.yml中配置 ai.agent.chat.url 或 ai.agent.base.url");
        return "http://localhost:8000/chat"; // 默认地址，需要根据实际情况修改
    }

    @Override
    public void chatStream(Long userId, String question, Object history, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                // TODO: 智能体开发程序员 - 后续更新流式接口 URL、请求/响应格式
                log.info("开始调用智能体对话接口（流式），用户ID：{}，问题：{}", userId, question);

                // 构建请求数据
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("userId", userId);
                requestData.put("question", question);
                
                // 添加对话历史
                if (history != null) {
                    requestData.put("history", history);
                    log.info("包含对话历史上下文");
                }

                // 确定智能体对话接口URL（流式接口）
                String chatEndpoint = determineChatUrl();
                // 如果是流式接口，可能需要添加/stream后缀
                if (!chatEndpoint.endsWith("/stream")) {
                    chatEndpoint = chatEndpoint.endsWith("/") 
                        ? chatEndpoint + "stream" 
                        : chatEndpoint + "/stream";
                }

                log.info("调用智能体流式对话接口：{}", chatEndpoint);

                // 使用WebClient调用智能体的流式接口
                // 智能体可能返回SSE格式或纯文本流
                Flux<String> responseStream = webClient.post()
                    .uri(chatEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                    .bodyValue(requestData)
                    .retrieve()
                    .bodyToFlux(String.class);

                // 处理流式响应
                responseStream.subscribe(
                    chunk -> {
                        try {
                            if (chunk != null && !chunk.trim().isEmpty()) {
                                // 尝试解析JSON格式的数据
                                String data = chunk;
                                
                                // 如果是SSE格式，提取data部分
                                if (chunk.startsWith("data: ")) {
                                    data = chunk.substring(6);
                                }
                                
                                // 发送数据到前端
                                emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(data));
                            }
                        } catch (IOException e) {
                            log.error("发送SSE事件失败", e);
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("接收智能体流式响应失败", error);
                        try {
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\":\"接收AI响应失败: " + error.getMessage() + "\"}"));
                        } catch (IOException e) {
                            log.error("发送错误消息失败", e);
                        }
                        emitter.completeWithError(error);
                    },
                    () -> {
                        log.info("智能体流式响应完成");
                        emitter.complete();
                    }
                );

            } catch (Exception e) {
                log.error("调用智能体流式接口失败", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"" + e.getMessage() + "\"}"));
                } catch (IOException ex) {
                    log.error("发送错误消息失败", ex);
                }
                emitter.completeWithError(e);
            }
        });
    }
}

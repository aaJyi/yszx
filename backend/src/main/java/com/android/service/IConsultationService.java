package com.android.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <p>
 * 在线问诊服务接口
 * </p>
 *
 * @author sjt
 * @since 2026-01-21
 */
public interface IConsultationService {

    /**
     * AI对话
     * 调用智能体对话接口，获取AI回答
     * 支持对话历史上下文，智能体可以根据历史对话提供更准确的回答
     *
     * @param userId 用户ID
     * @param question 用户提问
     * @param history 对话历史（可选），格式为List<Map<String, String>>，每个Map包含role和content
     * @return AI回答
     */
    String chat(Long userId, String question, Object history);

    /**
     * AI对话（流式输出）
     * 调用智能体对话接口，以流式方式返回AI回答
     * 支持对话历史上下文，智能体可以根据历史对话提供更准确的回答
     *
     * @param userId 用户ID
     * @param question 用户提问
     * @param history 对话历史（可选），格式为List<Map<String, String>>，每个Map包含role和content
     * @param emitter SSE发射器，用于流式输出
     */
    void chatStream(Long userId, String question, Object history, SseEmitter emitter);
}

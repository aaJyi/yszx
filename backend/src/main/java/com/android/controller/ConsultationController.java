package com.android.controller;

import com.android.common.Result;
import com.android.service.IConsultationService;
import com.android.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 在线问诊控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-21
 */
@Slf4j
@RestController
@RequestMapping("/consultation")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "在线问诊", description = "AI在线问诊相关接口")
public class ConsultationController {

    private final IConsultationService consultationService;

    /**
     * AI对话接口（流式输出）
     * TODO: 智能体开发程序员 - 后续更新智能体流式接口配置
     * 接收用户提问，调用智能体对话接口，以流式方式返回AI回答
     * 支持对话历史上下文，智能体可以根据历史对话提供更准确的回答
     *
     * @param requestBody 请求体，包含question、userId（可选）、history（可选，对话历史）
     * @return SSE流式响应
     */
    @Operation(summary = "AI对话（流式）", description = "接收用户提问，调用智能体对话接口，以流式方式返回AI回答。支持对话历史上下文。")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> requestBody) {
        SseEmitter emitter = new SseEmitter(900000L); // 15分钟超时
        
        try {
            // 从请求体中获取参数
            String finalQuestion = null;
            Long finalUserId = null;
            Object historyObj = null;

            if (requestBody != null) {
                if (requestBody.containsKey("question")) {
                    finalQuestion = (String) requestBody.get("question");
                }
                if (requestBody.containsKey("userId")) {
                    Object userIdObj = requestBody.get("userId");
                    if (userIdObj instanceof Number) {
                        finalUserId = ((Number) userIdObj).longValue();
                    } else if (userIdObj instanceof String) {
                        try {
                            finalUserId = Long.parseLong((String) userIdObj);
                        } catch (NumberFormatException e) {
                            log.warn("userId格式错误：{}", userIdObj);
                        }
                    }
                }
                if (requestBody.containsKey("history")) {
                    historyObj = requestBody.get("history");
                }
            }

            // 如果userId为空，尝试从UserContext获取
            if (finalUserId == null) {
                finalUserId = UserContext.getUserId();
            }

            if (finalQuestion == null || finalQuestion.trim().isEmpty()) {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\":\"提问内容不能为空\"}"));
                emitter.complete();
                return emitter;
            }

            if (finalUserId == null) {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\":\"用户ID不能为空，请先登录\"}"));
                emitter.complete();
                return emitter;
            }

            log.info("用户{}发起AI问诊（流式），问题：{}", finalUserId, finalQuestion);

            // 异步调用服务层处理流式对话
            consultationService.chatStream(finalUserId, finalQuestion, historyObj, emitter);

        } catch (Exception e) {
            log.error("AI对话流式输出失败", e);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\":\"" + e.getMessage() + "\"}"));
            } catch (Exception ex) {
                log.error("发送错误消息失败", ex);
            }
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * AI对话接口（兼容旧版本，非流式）
     * 接收用户提问，调用智能体对话接口，返回AI回答
     * 支持对话历史上下文，智能体可以根据历史对话提供更准确的回答
     *
     * @param requestBody 请求体，包含question、userId（可选）、history（可选，对话历史）
     * @return AI回答
     */
    @Operation(summary = "AI对话", description = "接收用户提问，调用智能体对话接口，返回AI回答。支持对话历史上下文。")
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> requestBody) {
        try {
            // 从请求体中获取参数
            String finalQuestion = null;
            Long finalUserId = null;
            Object historyObj = null;

            if (requestBody != null) {
                if (requestBody.containsKey("question")) {
                    finalQuestion = (String) requestBody.get("question");
                }
                if (requestBody.containsKey("userId")) {
                    Object userIdObj = requestBody.get("userId");
                    if (userIdObj instanceof Number) {
                        finalUserId = ((Number) userIdObj).longValue();
                    } else if (userIdObj instanceof String) {
                        try {
                            finalUserId = Long.parseLong((String) userIdObj);
                        } catch (NumberFormatException e) {
                            log.warn("userId格式错误：{}", userIdObj);
                        }
                    }
                }
                if (requestBody.containsKey("history")) {
                    historyObj = requestBody.get("history");
                }
            }

            // 如果userId为空，尝试从UserContext获取
            if (finalUserId == null) {
                finalUserId = UserContext.getUserId();
            }

            if (finalQuestion == null || finalQuestion.trim().isEmpty()) {
                return Result.badRequest("提问内容不能为空");
            }

            if (finalUserId == null) {
                return Result.badRequest("用户ID不能为空，请先登录");
            }

            log.info("用户{}发起AI问诊，问题：{}", finalUserId, finalQuestion);

            // 调用服务层处理对话（支持对话历史）
            String answer = consultationService.chat(finalUserId, finalQuestion, historyObj);

            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("question", finalQuestion);
            result.put("userId", finalUserId);
            log.info("回答内容: ", answer);
            log.info("用户{}的AI问诊完成，问题：{}，回答长度：{}", finalUserId, finalQuestion, answer != null ? answer.length() : 0);

            return Result.success("对话成功", result);

        } catch (RuntimeException e) {
            log.error("AI对话失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("AI对话时发生异常", e);
            return Result.error("对话失败: " + e.getMessage());
        }
    }
}

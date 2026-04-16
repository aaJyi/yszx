package com.android.controller;

import com.android.common.Result;
import com.android.service.IConsultationService;
import com.android.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 首页对话控制器
 * 专门处理首页下方的对话提问
 * </p>
 *
 * @author sjt
 * @since 2026-01-23
 */
@Slf4j
@RestController
@RequestMapping("/home-chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "首页对话", description = "首页对话提问相关接口")
public class HomeChatController {

    private final IConsultationService consultationService;

    /**
     * 首页对话提问接口
     * TODO: 智能体开发程序员 - 后续更新 ConsultationServiceImpl 中智能体调用配置
     * 接收用户提问，调用智能体对话接口，返回AI回答
     * 支持对话历史上下文，智能体可以根据历史对话提供更准确的回答
     *
     * @param requestBody 请求体，包含question、userId（可选）、history（可选，对话历史）
     * @return AI回答
     */
    @Operation(summary = "首页对话提问", description = "接收首页用户提问，调用智能体对话接口，返回AI回答。支持对话历史上下文。")
    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestBody Map<String, Object> requestBody) {
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

            log.info("首页用户{}发起对话提问，问题：{}", finalUserId, finalQuestion);

            // 调用服务层处理对话
            String answer = consultationService.chat(finalUserId, finalQuestion, historyObj);

            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("question", finalQuestion);
            result.put("userId", finalUserId);

            log.info("首页用户{}的对话提问完成，问题：{}，回答长度：{}", finalUserId, finalQuestion, answer != null ? answer.length() : 0);
            
            // 记录回答内容用于调试
            if (answer != null) {
                log.info("返回给前端的回答内容：{}", answer);
            } else {
                log.warn("回答内容为null，可能导致前端无法显示");
            }

            return Result.success("对话成功", result);

        } catch (RuntimeException e) {
            log.error("首页对话提问失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("首页对话提问时发生异常", e);
            return Result.error("对话失败: " + e.getMessage());
        }
    }
}

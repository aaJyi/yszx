package com.android.controller;

import com.android.common.Result;
import com.android.service.IConsultationService;
import com.android.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * 心理咨询对话控制器
 * TODO: 智能体开发程序员 - 后续可在此增加心理咨询专属 prompt 或上下文
 * 转发到智能体对话服务，供心理咨询页面调用
 */
@Slf4j
@RestController
@RequestMapping("/psychological-chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PsychologicalConsultationController {

    private final IConsultationService consultationService;

    /**
     * 心理咨询对话（非流式）
     * 兼容前端 {message} 参数，转发为 {question} 调用智能体
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> requestBody) {
        String message = requestBody != null && requestBody.containsKey("message") 
                ? String.valueOf(requestBody.get("message")) : null;
        Long userId = UserContext.getUserId();
        if (message == null || message.trim().isEmpty()) {
            return Result.badRequest("消息内容不能为空");
        }
        if (userId == null) {
            return Result.badRequest("请先登录");
        }
        String answer = consultationService.chat(userId, message.trim(), null);
        Map<String, Object> result = new HashMap<>();
        result.put("content", answer);
        result.put("message", message);
        return Result.success("对话成功", result);
    }

    /**
     * 心理咨询对话（流式）
     * 接收 {message}，转发为 {question} 调用智能体流式接口
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> requestBody) {
        SseEmitter emitter = new SseEmitter(900000L);
        String message = requestBody != null && requestBody.containsKey("message") 
                ? String.valueOf(requestBody.get("message")) : null;
        Long userId = UserContext.getUserId();
        if (message == null || message.trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"消息内容不能为空\"}"));
            } catch (Exception e) {
                log.error("发送错误失败", e);
            }
            emitter.complete();
            return emitter;
        }
        if (userId == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"请先登录\"}"));
            } catch (Exception e) {
                log.error("发送错误失败", e);
            }
            emitter.complete();
            return emitter;
        }
        consultationService.chatStream(userId, message.trim(), null, emitter);
        return emitter;
    }
}

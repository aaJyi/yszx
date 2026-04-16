package com.android.controller;

import com.android.common.Result;
import com.android.entity.EmotionMonitoring;
import com.android.service.IEmotionMonitoringService;
import com.android.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 情绪监测控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-27
 */
@Slf4j
@RestController
@RequestMapping("/emotion-monitoring")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "情绪监测", description = "情绪监测相关接口")
public class EmotionMonitoringController {

    private final IEmotionMonitoringService emotionMonitoringService;

    /**
     * 记录今日情绪
     *
     * @param requestBody 请求体，包含 todayEmotion（今日情绪分数）
     * @return 保存结果
     */
    @Operation(summary = "记录今日情绪", description = "记录用户的今日情绪，自动计算记录天数、平均情绪和平稳情绪")
    @PostMapping("/record")
    public Result<EmotionMonitoring> recordTodayEmotion(@RequestBody Map<String, Object> requestBody) {
        try {
            // 从请求体获取参数
            Integer todayEmotion = null;
            if (requestBody != null && requestBody.containsKey("todayEmotion")) {
                Object emotionObj = requestBody.get("todayEmotion");
                if (emotionObj instanceof Number) {
                    todayEmotion = ((Number) emotionObj).intValue();
                } else if (emotionObj instanceof String) {
                    todayEmotion = Integer.parseInt((String) emotionObj);
                }
            }
            
            // 从ThreadLocal获取用户ID
            Long userIdFromContext = UserContext.getUserId();
            Integer userId = userIdFromContext != null ? userIdFromContext.intValue() : null;
            
            // 如果请求体中有userId，优先使用
            if (requestBody != null && requestBody.containsKey("userId")) {
                Object userIdObj = requestBody.get("userId");
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).intValue();
                } else if (userIdObj instanceof String) {
                    userId = Integer.parseInt((String) userIdObj);
                }
            }
            
            if (userId == null) {
                return Result.badRequest("用户ID不能为空");
            }
            
            if (todayEmotion == null) {
                return Result.badRequest("今日情绪分数不能为空");
            }
            
            // 验证情绪分数
            if (todayEmotion != 100 && todayEmotion != 80 && todayEmotion != 60 && 
                todayEmotion != 40 && todayEmotion != 20) {
                return Result.badRequest("今日情绪分数必须是 100（非常开心）、80（开心）、60（平静）、40（低落）或 20（很难过）");
            }
            
            EmotionMonitoring record = emotionMonitoringService.recordTodayEmotion(userId, todayEmotion);
            
            log.info("记录今日情绪成功，用户ID：{}，今日情绪：{}", userId, todayEmotion);
            
            return Result.success("记录成功", record);
            
        } catch (Exception e) {
            log.error("记录今日情绪失败", e);
            return Result.error("记录今日情绪失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的情绪监测数据
     *
     * @param userId 用户ID（可选，如果不传则从ThreadLocal获取）
     * @return 情绪监测数据
     */
    @Operation(summary = "获取情绪监测数据", description = "获取用户的情绪监测数据，包括今日情绪、记录天数、平均情绪、平稳情绪等")
    @GetMapping("/data")
    public Result<Map<String, Object>> getEmotionMonitoringData(
            @RequestParam(value = "userId", required = false) Integer userId) {
        try {
            // 如果参数中没有userId，从ThreadLocal获取
            if (userId == null) {
                Long userIdFromContext = UserContext.getUserId();
                if (userIdFromContext != null) {
                    userId = userIdFromContext.intValue();
                }
            }
            
            if (userId == null) {
                return Result.badRequest("用户ID不能为空");
            }
            
            Map<String, Object> data = emotionMonitoringService.getEmotionMonitoringData(userId);
            
            log.info("查询情绪监测数据成功，用户ID：{}", userId);
            
            return Result.success("查询成功", data);
            
        } catch (Exception e) {
            log.error("查询情绪监测数据失败", e);
            return Result.error("查询情绪监测数据失败: " + e.getMessage());
        }
    }
}

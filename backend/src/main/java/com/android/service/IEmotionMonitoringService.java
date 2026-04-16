package com.android.service;

import com.android.entity.EmotionMonitoring;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 情绪监测服务接口
 * </p>
 *
 * @author sjt
 * @since 2026-01-27
 */
public interface IEmotionMonitoringService extends IService<EmotionMonitoring> {
    
    /**
     * 记录今日情绪
     * 根据用户ID和今日情绪分数，计算并保存情绪记录
     * 
     * @param userId 用户ID
     * @param todayEmotion 今日情绪分数（100-非常开心 80-开心 60-平静 40-低落 20-很难过）
     * @return 保存后的情绪监测记录
     */
    EmotionMonitoring recordTodayEmotion(Integer userId, Integer todayEmotion);
    
    /**
     * 获取用户的情绪监测数据
     * 包括今日情绪、记录天数、平均情绪、平稳情绪等
     * 
     * @param userId 用户ID
     * @return 情绪监测数据（包含统计信息）
     */
    Map<String, Object> getEmotionMonitoringData(Integer userId);
}

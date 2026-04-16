package com.android.service.impl;

import com.android.entity.EmotionMonitoring;
import com.android.mapper.EmotionMonitoringMapper;
import com.android.service.IEmotionMonitoringService;
import com.android.util.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 情绪监测服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionMonitoringServiceImpl extends ServiceImpl<EmotionMonitoringMapper, EmotionMonitoring> implements IEmotionMonitoringService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmotionMonitoring recordTodayEmotion(Integer userId, Integer todayEmotion) {
        try {
            // 验证情绪分数
            if (todayEmotion == null || (todayEmotion != 100 && todayEmotion != 80 && 
                todayEmotion != 60 && todayEmotion != 40 && todayEmotion != 20)) {
                throw new RuntimeException("今日情绪分数必须是 100、80、60、40 或 20");
            }
            
            LocalDate today = LocalDate.now();
            
            // 查询今天是否已有记录
            LambdaQueryWrapper<EmotionMonitoring> todayQuery = new LambdaQueryWrapper<>();
            todayQuery.eq(EmotionMonitoring::getUserId, userId)
                     .eq(EmotionMonitoring::getRecordDate, today);
            
            EmotionMonitoring todayRecord = this.getOne(todayQuery);
            
            if (todayRecord != null) {
                // 今天已有记录，这是修改操作
                // 获取之前的今日情绪（用于重新计算平均情绪）
                Integer oldTodayEmotion = todayRecord.getTodayEmotion();
                int recordDays = todayRecord.getRecordDays() != null ? todayRecord.getRecordDays() : 1;
                BigDecimal currentAverageEmotion = todayRecord.getAverageEmotion() != null ? 
                                                   todayRecord.getAverageEmotion() : BigDecimal.ZERO;
                
                // 修改当天的记录时，记录天数不变
                int newRecordDays = recordDays;
                
                // 重新计算平均情绪：需要先减去旧的今日情绪，再加上新的今日情绪
                // 新平均 = (旧平均 * 记录天数 - 旧今日情绪 + 新今日情绪) / 记录天数
                BigDecimal newAverageEmotion;
                if (recordDays == 1) {
                    // 如果只有一条记录，平均情绪就是新的今日情绪
                    newAverageEmotion = new BigDecimal(todayEmotion);
                } else {
                    // 从总和中减去旧的今日情绪，加上新的今日情绪
                    BigDecimal totalSum = currentAverageEmotion.multiply(new BigDecimal(recordDays));
                    BigDecimal oldEmotionDecimal = new BigDecimal(oldTodayEmotion != null ? oldTodayEmotion : 0);
                    BigDecimal newEmotionDecimal = new BigDecimal(todayEmotion);
                    BigDecimal newTotalSum = totalSum.subtract(oldEmotionDecimal).add(newEmotionDecimal);
                    newAverageEmotion = newTotalSum.divide(new BigDecimal(recordDays), 2, RoundingMode.HALF_UP);
                }
                
                // 平稳情绪 = 今日情绪分数 - 平均情绪分数，如果 >= 0 为 1，否则为 0
                int stableEmotion = (todayEmotion - newAverageEmotion.intValue()) >= 0 ? 1 : 0;
                
                // 更新今天的记录
                todayRecord.setTodayEmotion(todayEmotion);
                todayRecord.setRecordDays(newRecordDays);
                todayRecord.setAverageEmotion(newAverageEmotion);
                todayRecord.setStableEmotion(stableEmotion);
                
                boolean updated = this.updateById(todayRecord);
                if (!updated) {
                    throw new RuntimeException("更新情绪记录失败");
                }
                
                log.info("更新情绪记录成功，用户ID：{}，今日情绪：{}（从{}修改），记录天数：{}，平均情绪：{}，平稳情绪：{}", 
                        userId, todayEmotion, oldTodayEmotion, newRecordDays, newAverageEmotion, stableEmotion);
                
                return todayRecord;
            } else {
                // 今天没有记录，创建新记录
                // 查询用户最近一条记录（用于获取当前记录天数和平均情绪）
                LambdaQueryWrapper<EmotionMonitoring> lastQuery = new LambdaQueryWrapper<>();
                lastQuery.eq(EmotionMonitoring::getUserId, userId)
                        .orderByDesc(EmotionMonitoring::getRecordDate)
                        .last("LIMIT 1");
                
                EmotionMonitoring lastRecord = this.getOne(lastQuery);
                
                // 计算新的记录天数和平均情绪
                int currentRecordDays = 0;
                BigDecimal currentAverageEmotion = BigDecimal.ZERO;
                
                if (lastRecord != null) {
                    currentRecordDays = lastRecord.getRecordDays() != null ? lastRecord.getRecordDays() : 0;
                    currentAverageEmotion = lastRecord.getAverageEmotion() != null ? lastRecord.getAverageEmotion() : BigDecimal.ZERO;
                }
                
                // 记录天数 = 当前天数 + 1
                int newRecordDays = currentRecordDays + 1;
                
                // 平均情绪计算：平均情绪 = (当前平均情绪 * (记录天数-1) + 今日情绪) / 记录天数
                BigDecimal newAverageEmotion;
                if (newRecordDays == 1) {
                    // 第一次记录，平均情绪就是今日情绪
                    newAverageEmotion = new BigDecimal(todayEmotion);
                } else {
                    // 加权平均：新平均 = (旧平均 * (旧天数) + 今日情绪) / 新天数
                    BigDecimal oldAverageTimesOldDays = currentAverageEmotion.multiply(new BigDecimal(currentRecordDays));
                    BigDecimal todayEmotionDecimal = new BigDecimal(todayEmotion);
                    BigDecimal sum = oldAverageTimesOldDays.add(todayEmotionDecimal);
                    newAverageEmotion = sum.divide(new BigDecimal(newRecordDays), 2, RoundingMode.HALF_UP);
                }
                
                // 平稳情绪 = 今日情绪分数 - 平均情绪分数，如果 >= 0 为 1，否则为 0
                int stableEmotion = (todayEmotion - newAverageEmotion.intValue()) >= 0 ? 1 : 0;
                
                // 创建新记录
                EmotionMonitoring newRecord = new EmotionMonitoring();
                newRecord.setUserId(userId);
                newRecord.setTodayEmotion(todayEmotion);
                newRecord.setRecordDays(newRecordDays);
                newRecord.setAverageEmotion(newAverageEmotion);
                newRecord.setStableEmotion(stableEmotion);
                newRecord.setRecordDate(today);
                
                boolean saved = this.save(newRecord);
                if (!saved) {
                    throw new RuntimeException("保存情绪记录失败");
                }
                
                log.info("保存情绪记录成功，用户ID：{}，今日情绪：{}，记录天数：{}，平均情绪：{}，平稳情绪：{}", 
                        userId, todayEmotion, newRecordDays, newAverageEmotion, stableEmotion);
                
                return newRecord;
            }
            
        } catch (Exception e) {
            log.error("记录今日情绪失败，用户ID：{}，今日情绪：{}", userId, todayEmotion, e);
            throw new RuntimeException("记录今日情绪失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getEmotionMonitoringData(Integer userId) {
        try {
            LocalDate today = LocalDate.now();
            
            // 查询今天的记录
            LambdaQueryWrapper<EmotionMonitoring> todayQuery = new LambdaQueryWrapper<>();
            todayQuery.eq(EmotionMonitoring::getUserId, userId)
                     .eq(EmotionMonitoring::getRecordDate, today);
            
            EmotionMonitoring todayRecord = this.getOne(todayQuery);
            
            // 查询用户最近一条记录（用于获取统计数据）
            LambdaQueryWrapper<EmotionMonitoring> lastQuery = new LambdaQueryWrapper<>();
            lastQuery.eq(EmotionMonitoring::getUserId, userId)
                    .orderByDesc(EmotionMonitoring::getRecordDate)
                    .last("LIMIT 1");
            
            EmotionMonitoring lastRecord = this.getOne(lastQuery);
            
            Map<String, Object> result = new HashMap<>();
            
            if (lastRecord != null) {
                result.put("todayEmotion", todayRecord != null ? todayRecord.getTodayEmotion() : null);
                result.put("recordDays", lastRecord.getRecordDays() != null ? lastRecord.getRecordDays() : 0);
                result.put("averageEmotion", lastRecord.getAverageEmotion() != null ? 
                          lastRecord.getAverageEmotion().intValue() : 0);
                result.put("stableEmotion", lastRecord.getStableEmotion() != null ? lastRecord.getStableEmotion() : 0);
                result.put("emotionTrend", lastRecord.getStableEmotion() != null && lastRecord.getStableEmotion() == 1 ? 
                          "上升情绪" : "下降情绪");
            } else {
                // 没有记录，返回默认值
                result.put("todayEmotion", null);
                result.put("recordDays", 0);
                result.put("averageEmotion", 0);
                result.put("stableEmotion", 0);
                result.put("emotionTrend", "平稳情绪");
            }
            
            log.info("查询情绪监测数据成功，用户ID：{}", userId);
            
            return result;
            
        } catch (Exception e) {
            log.error("查询情绪监测数据失败，用户ID：{}", userId, e);
            throw new RuntimeException("查询情绪监测数据失败: " + e.getMessage());
        }
    }
}

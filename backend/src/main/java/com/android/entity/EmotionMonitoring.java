package com.android.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 情绪监测表实体类
 * </p>
 *
 * @author sjt
 * @since 2026-01-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("emotion_monitoring")
public class EmotionMonitoring implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 今日情绪（100-非常开心 80-开心 60-平静 40-低落 20-很难过）
     */
    private Integer todayEmotion;

    /**
     * 记录天数（默认为0）
     */
    private Integer recordDays;

    /**
     * 平均情绪（默认为0）
     */
    private BigDecimal averageEmotion;

    /**
     * 平稳情绪（0-下降 1-上升）
     */
    private Integer stableEmotion;

    /**
     * 记录日期
     */
    private LocalDate recordDate;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

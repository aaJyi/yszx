package com.android.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端控制台：四张核心表汇总
 */
@Data
public class AdminDashboardOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** t_user 用户总数 */
    private long userTotal;

    /** raw_health_data 原始数据条数 */
    private long rawHealthDataTotal;

    /** family_member 家人档案总条数 */
    private long familyMemberTotal;

    /**
     * 产品普及率：至少添加过 1 位家人的用户占注册用户比例（%）
     */
    private BigDecimal familyPenetrationPercent;

    /** emotion_monitoring 情绪记录总条数 */
    private long emotionRecordTotal;

    /**
     * 情绪功能日常使用率：有过情绪记录的去重用户数占注册用户比例（%）
     */
    private BigDecimal emotionUsagePercent;

    /** 各分值区间记录数占比（基于有有效分值的记录） */
    private List<EmotionLevelShareVO> emotionLevelShares = new ArrayList<>();
}

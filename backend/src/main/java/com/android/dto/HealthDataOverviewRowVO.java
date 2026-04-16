package com.android.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端：用户健康数据汇总行（各类型取最近一条）
 */
@Data
@Builder
public class HealthDataOverviewRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 与 raw_health_data.user_id 一致，用于拉取明细 */
    private Long userId;
    /** 展示用：openid */
    private String openid;
    /** 展示姓名：取用户昵称 */
    private String userName;

    private HealthDataCellVO report;
    private HealthDataCellVO medicalRecord;
    private HealthDataCellVO skin;
    private HealthDataCellVO lab;
    private HealthDataCellVO meal;
}

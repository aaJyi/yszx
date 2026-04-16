package com.android.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 小程序「我的」页：三项统计（健康记录 / 家人管理 / 健康报告）
 */
@Data
@Builder
public class MiniProfileStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** raw_health_data 中当前用户全部条数 */
    private long healthRecordCount;
    /** family_member 中 owner_user_id = 当前用户 */
    private long familyMemberCount;
    /** raw_health_data 中 REPORT、MEDICAL_RECORD（体检/拍报告类）条数，仅当前用户本人 */
    private long healthReportCount;
}

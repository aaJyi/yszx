package com.android.service.impl;

import com.android.dto.MiniProfileStatsVO;
import com.android.entity.FamilyMember;
import com.android.entity.RawHealthData;
import com.android.service.IFamilyMemberService;
import com.android.service.IMiniProfileStatsService;
import com.android.service.IRawHealthDataService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 健康记录：本人 raw_health_data 总条数；家人管理：本人添加的家人条数；
 * 健康报告：本人 REPORT + MEDICAL_RECORD 条数（与「体检报告/就诊」原始资料口径一致）。
 */
@Service
@RequiredArgsConstructor
public class MiniProfileStatsServiceImpl implements IMiniProfileStatsService {

    private final IRawHealthDataService rawHealthDataService;
    private final IFamilyMemberService familyMemberService;

    @Override
    public MiniProfileStatsVO getProfileStats(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        long rawTotal = rawHealthDataService.count(
                Wrappers.<RawHealthData>lambdaQuery().eq(RawHealthData::getUserId, userId));

        long familyTotal = familyMemberService.count(
                Wrappers.<FamilyMember>lambdaQuery().eq(FamilyMember::getOwnerUserId, userId.intValue()));

        long reportTotal = rawHealthDataService.count(
                Wrappers.<RawHealthData>lambdaQuery()
                        .eq(RawHealthData::getUserId, userId)
                        .and(w -> w.eq(RawHealthData::getDataType, "REPORT")
                                .or()
                                .eq(RawHealthData::getDataType, "MEDICAL_RECORD")));

        return MiniProfileStatsVO.builder()
                .healthRecordCount(rawTotal)
                .familyMemberCount(familyTotal)
                .healthReportCount(reportTotal)
                .build();
    }
}

package com.android.service.impl;

import com.android.dto.AdminDashboardOverviewVO;
import com.android.dto.EmotionBucketRawCounts;
import com.android.dto.EmotionLevelShareVO;
import com.android.mapper.AdminDashboardMapper;
import com.android.service.IAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements IAdminDashboardService {

    private final AdminDashboardMapper adminDashboardMapper;

    @Override
    public AdminDashboardOverviewVO getOverview() {
        AdminDashboardOverviewVO vo = new AdminDashboardOverviewVO();

        long userTotal = safeLong(adminDashboardMapper.countUsers());
        vo.setUserTotal(userTotal);
        vo.setRawHealthDataTotal(safeLong(adminDashboardMapper.countRawHealthData()));
        vo.setFamilyMemberTotal(safeLong(adminDashboardMapper.countFamilyMembers()));

        long distinctOwners = safeLong(adminDashboardMapper.countDistinctFamilyOwners());
        vo.setFamilyPenetrationPercent(percent(distinctOwners, userTotal));

        long emotionRecords = safeLong(adminDashboardMapper.countEmotionRecords());
        long distinctEmotionUsers = safeLong(adminDashboardMapper.countDistinctEmotionUsers());
        vo.setEmotionRecordTotal(emotionRecords);
        vo.setEmotionUsagePercent(percent(distinctEmotionUsers, userTotal));

        EmotionBucketRawCounts buckets = adminDashboardMapper.selectEmotionBucketCounts();
        long vh = safeLong(buckets.getVeryHappy());
        long h = safeLong(buckets.getHappy());
        long c = safeLong(buckets.getCalm());
        long l = safeLong(buckets.getLow());
        long vs = safeLong(buckets.getVerySad());
        long scoredTotal = vh + h + c + l + vs;

        List<EmotionLevelShareVO> shares = Arrays.asList(
                share("100–80", "非常开心", vh, scoredTotal),
                share("80–60", "开心", h, scoredTotal),
                share("60–40", "平静", c, scoredTotal),
                share("40–20", "低落", l, scoredTotal),
                share("20–0", "很难过", vs, scoredTotal)
        );
        vo.setEmotionLevelShares(shares);
        return vo;
    }

    private static EmotionLevelShareVO share(String rangeLabel, String name, long count, long total) {
        return new EmotionLevelShareVO(
                rangeLabel,
                name,
                count,
                EmotionLevelShareVO.pct(count, total)
        );
    }

    private static long safeLong(Long v) {
        return v == null ? 0L : v;
    }

    private static BigDecimal percent(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}

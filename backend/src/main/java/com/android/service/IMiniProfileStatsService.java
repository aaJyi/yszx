package com.android.service;

import com.android.dto.MiniProfileStatsVO;

/**
 * 小程序个人中心统计
 */
public interface IMiniProfileStatsService {

    MiniProfileStatsVO getProfileStats(Long userId);
}

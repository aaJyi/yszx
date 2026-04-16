package com.android.service;

import com.android.dto.AdminDashboardOverviewVO;

/**
 * 管理端控制台统计
 */
public interface IAdminDashboardService {

    AdminDashboardOverviewVO getOverview();
}

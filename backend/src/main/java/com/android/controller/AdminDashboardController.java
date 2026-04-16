package com.android.controller;

import com.android.common.Result;
import com.android.dto.AdminDashboardOverviewVO;
import com.android.dto.AdminInferredDiseaseRowVO;
import com.android.service.IAdminDashboardService;
import com.android.service.IUserInferredDiseaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端控制台统计（PC 管理后台）
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final IAdminDashboardService adminDashboardService;
    private final IUserInferredDiseaseService userInferredDiseaseService;

    @GetMapping("/overview")
    public Result<AdminDashboardOverviewVO> overview() {
        return Result.success(adminDashboardService.getOverview());
    }

    @GetMapping("/inferred-diseases")
    public Result<List<AdminInferredDiseaseRowVO>> inferredDiseases(
            @RequestParam(required = false, defaultValue = "200") Integer limit) {
        return Result.success(userInferredDiseaseService.listRecentForAdmin(limit));
    }
}

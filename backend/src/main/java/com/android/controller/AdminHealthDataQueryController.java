package com.android.controller;

import com.android.common.Result;
import com.android.dto.HealthDataOverviewRowVO;
import com.android.dto.PageResultVO;
import com.android.dto.RawHealthDataMetaVO;
import com.android.service.IAdminHealthDataQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：健康原始数据汇总
 */
@RestController
@RequestMapping("/admin/health-data")
@RequiredArgsConstructor
public class AdminHealthDataQueryController {

    private final IAdminHealthDataQueryService adminHealthDataQueryService;

    @GetMapping("/overview")
    public Result<PageResultVO<HealthDataOverviewRowVO>> overview(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(adminHealthDataQueryService.pageOverview(pageNum, pageSize));
    }

    @GetMapping("/records")
    public Result<List<RawHealthDataMetaVO>> records(
            @RequestParam("userId") Long userId,
            @RequestParam("dataType") String dataType) {
        return Result.success(adminHealthDataQueryService.listRecordsByUserAndType(userId, dataType));
    }
}

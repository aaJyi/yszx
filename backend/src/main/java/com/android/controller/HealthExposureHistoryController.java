package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthExposureHistory;
import com.android.service.IHealthExposureHistoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 暴露史表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-exposure-history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "暴露史", description = "暴露史相关接口")
public class HealthExposureHistoryController {

    private final IHealthExposureHistoryService healthExposureHistoryService;

    /**
     * 根据健康档案ID更新暴露史
     *
     * @param archiveId 档案ID
     * @param healthExposureHistory 暴露史对象
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新暴露史", description = "根据健康档案ID更新暴露史信息")
    @PutMapping("/archive/{archiveId}")
    public Result<HealthExposureHistory> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody HealthExposureHistory healthExposureHistory) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 查询是否存在该archive_id的记录
            LambdaQueryWrapper<HealthExposureHistory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthExposureHistory::getArchiveId, archiveId);
            HealthExposureHistory existing = healthExposureHistoryService.getOne(queryWrapper);

            if (existing == null) {
                // 如果不存在，则创建新记录
                healthExposureHistory.setArchiveId(archiveId);
                boolean result = healthExposureHistoryService.save(healthExposureHistory);
                if (result) {
                    log.info("创建暴露史成功，档案ID：{}", archiveId);
                    return Result.success("创建成功", healthExposureHistory);
                } else {
                    return Result.error("创建失败");
                }
            } else {
                // 如果存在，则更新记录
                healthExposureHistory.setExposureId(existing.getExposureId());
                healthExposureHistory.setArchiveId(archiveId);
                boolean result = healthExposureHistoryService.updateById(healthExposureHistory);
                if (result) {
                    log.info("更新暴露史成功，档案ID：{}", archiveId);
                    return Result.success("更新成功", healthExposureHistory);
                } else {
                    return Result.error("更新失败");
                }
            }
        } catch (Exception e) {
            log.error("更新暴露史失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

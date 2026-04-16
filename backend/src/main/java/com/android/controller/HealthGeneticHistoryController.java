package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthGeneticHistory;
import com.android.service.IHealthGeneticHistoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 遗传病史表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-genetic-history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "遗传病史", description = "遗传病史相关接口")
public class HealthGeneticHistoryController {

    private final IHealthGeneticHistoryService healthGeneticHistoryService;

    /**
     * 根据健康档案ID更新遗传病史
     *
     * @param archiveId 档案ID
     * @param healthGeneticHistory 遗传病史对象
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新遗传病史", description = "根据健康档案ID更新遗传病史信息")
    @PutMapping("/archive/{archiveId}")
    public Result<HealthGeneticHistory> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody HealthGeneticHistory healthGeneticHistory) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 查询是否存在该archive_id的记录
            LambdaQueryWrapper<HealthGeneticHistory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthGeneticHistory::getArchiveId, archiveId);
            HealthGeneticHistory existing = healthGeneticHistoryService.getOne(queryWrapper);

            if (existing == null) {
                // 如果不存在，则创建新记录
                healthGeneticHistory.setArchiveId(archiveId);
                boolean result = healthGeneticHistoryService.save(healthGeneticHistory);
                if (result) {
                    log.info("创建遗传病史成功，档案ID：{}", archiveId);
                    return Result.success("创建成功", healthGeneticHistory);
                } else {
                    return Result.error("创建失败");
                }
            } else {
                // 如果存在，则更新记录
                healthGeneticHistory.setGeneticId(existing.getGeneticId());
                healthGeneticHistory.setArchiveId(archiveId);
                boolean result = healthGeneticHistoryService.updateById(healthGeneticHistory);
                if (result) {
                    log.info("更新遗传病史成功，档案ID：{}", archiveId);
                    return Result.success("更新成功", healthGeneticHistory);
                } else {
                    return Result.error("更新失败");
                }
            }
        } catch (Exception e) {
            log.error("更新遗传病史失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

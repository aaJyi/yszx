package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthAllergyHistory;
import com.android.service.IHealthAllergyHistoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 过敏史表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-allergy-history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "过敏史", description = "过敏史相关接口")
public class HealthAllergyHistoryController {

    private final IHealthAllergyHistoryService healthAllergyHistoryService;

    /**
     * 根据健康档案ID更新过敏史
     *
     * @param archiveId 档案ID
     * @param healthAllergyHistory 过敏史对象
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新过敏史", description = "根据健康档案ID更新过敏史信息")
    @PutMapping("/archive/{archiveId}")
    public Result<HealthAllergyHistory> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody HealthAllergyHistory healthAllergyHistory) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 查询是否存在该archive_id的记录
            LambdaQueryWrapper<HealthAllergyHistory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthAllergyHistory::getArchiveId, archiveId);
            HealthAllergyHistory existing = healthAllergyHistoryService.getOne(queryWrapper);

            if (existing == null) {
                // 如果不存在，则创建新记录
                healthAllergyHistory.setArchiveId(archiveId);
                boolean result = healthAllergyHistoryService.save(healthAllergyHistory);
                if (result) {
                    log.info("创建过敏史成功，档案ID：{}", archiveId);
                    return Result.success("创建成功", healthAllergyHistory);
                } else {
                    return Result.error("创建失败");
                }
            } else {
                // 如果存在，则更新记录
                healthAllergyHistory.setAllergyId(existing.getAllergyId());
                healthAllergyHistory.setArchiveId(archiveId);
                boolean result = healthAllergyHistoryService.updateById(healthAllergyHistory);
                if (result) {
                    log.info("更新过敏史成功，档案ID：{}", archiveId);
                    return Result.success("更新成功", healthAllergyHistory);
                } else {
                    return Result.error("更新失败");
                }
            }
        } catch (Exception e) {
            log.error("更新过敏史失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

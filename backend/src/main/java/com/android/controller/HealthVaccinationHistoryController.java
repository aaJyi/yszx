package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthVaccinationHistory;
import com.android.service.IHealthVaccinationHistoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 预防接种史表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-vaccination-history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "预防接种史", description = "预防接种史相关接口")
public class HealthVaccinationHistoryController {

    private final IHealthVaccinationHistoryService healthVaccinationHistoryService;

    /**
     * 根据健康档案ID更新预防接种史列表
     * 先删除该档案ID的所有接种史，然后插入新的接种史列表
     *
     * @param archiveId 档案ID
     * @param vaccinationHistories 预防接种史列表
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新预防接种史列表", description = "根据健康档案ID更新预防接种史列表，先删除旧数据再插入新数据")
    @PutMapping("/archive/{archiveId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<List<HealthVaccinationHistory>> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody List<HealthVaccinationHistory> vaccinationHistories) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 删除该archive_id的所有接种史
            LambdaQueryWrapper<HealthVaccinationHistory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthVaccinationHistory::getArchiveId, archiveId);
            healthVaccinationHistoryService.remove(queryWrapper);

            // 插入新的接种史列表
            if (vaccinationHistories != null && !vaccinationHistories.isEmpty()) {
                for (HealthVaccinationHistory vaccinationHistory : vaccinationHistories) {
                    vaccinationHistory.setArchiveId(archiveId);
                    vaccinationHistory.setVaccinationId(null); // 确保ID为null，让数据库自动生成
                }
                boolean result = healthVaccinationHistoryService.saveBatch(vaccinationHistories);
                if (result) {
                    log.info("更新预防接种史列表成功，档案ID：{}，接种史数量：{}", archiveId, vaccinationHistories.size());
                    return Result.success("更新成功", vaccinationHistories);
                } else {
                    return Result.error("更新失败");
                }
            } else {
                log.info("删除所有预防接种史，档案ID：{}", archiveId);
                return Result.success("更新成功（已清空所有接种史）", List.of());
            }
        } catch (Exception e) {
            log.error("更新预防接种史列表失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

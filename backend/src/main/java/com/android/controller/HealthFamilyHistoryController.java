package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthFamilyHistory;
import com.android.service.IHealthFamilyHistoryService;
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
 * 家族史表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-family-history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "家族史", description = "家族史相关接口")
public class HealthFamilyHistoryController {

    private final IHealthFamilyHistoryService healthFamilyHistoryService;

    /**
     * 根据健康档案ID更新家族史列表
     * 先删除该档案ID的所有家族史，然后插入新的家族史列表
     *
     * @param archiveId 档案ID
     * @param familyHistories 家族史列表
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新家族史列表", description = "根据健康档案ID更新家族史列表，先删除旧数据再插入新数据")
    @PutMapping("/archive/{archiveId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<List<HealthFamilyHistory>> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody List<HealthFamilyHistory> familyHistories) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 删除该archive_id的所有家族史
            LambdaQueryWrapper<HealthFamilyHistory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthFamilyHistory::getArchiveId, archiveId);
            healthFamilyHistoryService.remove(queryWrapper);

            // 插入新的家族史列表
            if (familyHistories != null && !familyHistories.isEmpty()) {
                for (HealthFamilyHistory familyHistory : familyHistories) {
                    familyHistory.setArchiveId(archiveId);
                    familyHistory.setFamilyId(null); // 确保ID为null，让数据库自动生成
                }
                boolean result = healthFamilyHistoryService.saveBatch(familyHistories);
                if (result) {
                    log.info("更新家族史列表成功，档案ID：{}，家族史数量：{}", archiveId, familyHistories.size());
                    return Result.success("更新成功", familyHistories);
                } else {
                    return Result.error("更新失败");
                }
            } else {
                log.info("删除所有家族史，档案ID：{}", archiveId);
                return Result.success("更新成功（已清空所有家族史）", List.of());
            }
        } catch (Exception e) {
            log.error("更新家族史列表失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

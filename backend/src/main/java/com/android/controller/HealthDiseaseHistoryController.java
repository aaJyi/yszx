package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthDiseaseHistory;
import com.android.service.IHealthDiseaseHistoryService;
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
 * 既往史表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-disease-history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "既往史", description = "既往史相关接口")
public class HealthDiseaseHistoryController {

    private final IHealthDiseaseHistoryService healthDiseaseHistoryService;

    /**
     * 根据健康档案ID更新既往史列表
     * 先删除该档案ID的所有既往史，然后插入新的既往史列表
     *
     * @param archiveId 档案ID
     * @param diseaseHistories 既往史列表
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新既往史列表", description = "根据健康档案ID更新既往史列表，先删除旧数据再插入新数据")
    @PutMapping("/archive/{archiveId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<List<HealthDiseaseHistory>> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody List<HealthDiseaseHistory> diseaseHistories) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 删除该archive_id的所有既往史
            LambdaQueryWrapper<HealthDiseaseHistory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthDiseaseHistory::getArchiveId, archiveId);
            healthDiseaseHistoryService.remove(queryWrapper);

            // 插入新的既往史列表
            if (diseaseHistories != null && !diseaseHistories.isEmpty()) {
                for (HealthDiseaseHistory diseaseHistory : diseaseHistories) {
                    diseaseHistory.setArchiveId(archiveId);
                    diseaseHistory.setDiseaseId(null); // 确保ID为null，让数据库自动生成
                }
                boolean result = healthDiseaseHistoryService.saveBatch(diseaseHistories);
                if (result) {
                    log.info("更新既往史列表成功，档案ID：{}，既往史数量：{}", archiveId, diseaseHistories.size());
                    return Result.success("更新成功", diseaseHistories);
                } else {
                    return Result.error("更新失败");
                }
            } else {
                log.info("删除所有既往史，档案ID：{}", archiveId);
                return Result.success("更新成功（已清空所有既往史）", List.of());
            }
        } catch (Exception e) {
            log.error("更新既往史列表失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

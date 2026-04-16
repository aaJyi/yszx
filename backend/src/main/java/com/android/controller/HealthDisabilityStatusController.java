package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthDisabilityStatus;
import com.android.service.IHealthDisabilityStatusService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 残疾情况表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-disability-status")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "残疾情况", description = "残疾情况相关接口")
public class HealthDisabilityStatusController {

    private final IHealthDisabilityStatusService healthDisabilityStatusService;

    /**
     * 根据健康档案ID更新残疾情况
     *
     * @param archiveId 档案ID
     * @param healthDisabilityStatus 残疾情况对象
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新残疾情况", description = "根据健康档案ID更新残疾情况信息")
    @PutMapping("/archive/{archiveId}")
    public Result<HealthDisabilityStatus> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody HealthDisabilityStatus healthDisabilityStatus) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 查询是否存在该archive_id的记录
            LambdaQueryWrapper<HealthDisabilityStatus> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthDisabilityStatus::getArchiveId, archiveId);
            HealthDisabilityStatus existing = healthDisabilityStatusService.getOne(queryWrapper);

            if (existing == null) {
                // 如果不存在，则创建新记录
                healthDisabilityStatus.setArchiveId(archiveId);
                boolean result = healthDisabilityStatusService.save(healthDisabilityStatus);
                if (result) {
                    log.info("创建残疾情况成功，档案ID：{}", archiveId);
                    return Result.success("创建成功", healthDisabilityStatus);
                } else {
                    return Result.error("创建失败");
                }
            } else {
                // 如果存在，则更新记录
                healthDisabilityStatus.setDisabilityId(existing.getDisabilityId());
                healthDisabilityStatus.setArchiveId(archiveId);
                boolean result = healthDisabilityStatusService.updateById(healthDisabilityStatus);
                if (result) {
                    log.info("更新残疾情况成功，档案ID：{}", archiveId);
                    return Result.success("更新成功", healthDisabilityStatus);
                } else {
                    return Result.error("更新失败");
                }
            }
        } catch (Exception e) {
            log.error("更新残疾情况失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthProfileTags;
import com.android.service.IHealthProfileTagsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 健康档案标签表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-26
 */
@Slf4j
@RestController
@RequestMapping("/health-profile-tags")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "健康档案标签", description = "健康档案标签相关接口")
public class HealthProfileTagsController {

    private final IHealthProfileTagsService healthProfileTagsService;

    /**
     * 根据健康档案ID更新健康档案标签
     *
     * @param archiveId 档案ID
     * @param healthProfileTags 健康档案标签对象
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新健康档案标签", description = "根据健康档案ID更新健康档案标签信息")
    @PutMapping("/archive/{archiveId}")
    public Result<HealthProfileTags> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody HealthProfileTags healthProfileTags) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 查询是否存在该archive_id的记录
            LambdaQueryWrapper<HealthProfileTags> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthProfileTags::getArchiveId, archiveId);
            HealthProfileTags existing = healthProfileTagsService.getOne(queryWrapper);

            if (existing == null) {
                // 如果不存在，则创建新记录
                healthProfileTags.setArchiveId(archiveId);
                boolean result = healthProfileTagsService.save(healthProfileTags);
                if (result) {
                    log.info("创建健康档案标签成功，档案ID：{}", archiveId);
                    return Result.success("创建成功", healthProfileTags);
                } else {
                    return Result.error("创建失败");
                }
            } else {
                // 如果存在，则更新记录（使用主键更新）
                healthProfileTags.setTagId(existing.getTagId());
                healthProfileTags.setArchiveId(archiveId);
                boolean result = healthProfileTagsService.updateById(healthProfileTags);
                if (result) {
                    log.info("更新健康档案标签成功，档案ID：{}", archiveId);
                    return Result.success("更新成功", healthProfileTags);
                } else {
                    return Result.error("更新失败");
                }
            }
        } catch (Exception e) {
            log.error("更新健康档案标签失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

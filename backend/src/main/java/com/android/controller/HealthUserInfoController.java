package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthUserInfo;
import com.android.service.IHealthUserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 健康档案用户基本信息表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-user-info")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "健康档案用户基本信息", description = "健康档案用户基本信息相关接口")
public class HealthUserInfoController {

    private final IHealthUserInfoService healthUserInfoService;

    /**
     * 根据健康档案ID更新用户基本信息
     *
     * @param archiveId 档案ID
     * @param healthUserInfo 用户基本信息对象
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新用户基本信息", description = "根据健康档案ID更新用户基本信息")
    @PutMapping("/archive/{archiveId}")
    public Result<HealthUserInfo> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody HealthUserInfo healthUserInfo) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 查询是否存在该archive_id的记录
            LambdaQueryWrapper<HealthUserInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthUserInfo::getArchiveId, archiveId);
            HealthUserInfo existing = healthUserInfoService.getOne(queryWrapper);

            if (existing == null) {
                // 如果不存在，则创建新记录
                healthUserInfo.setArchiveId(archiveId);
                boolean result = healthUserInfoService.save(healthUserInfo);
                if (result) {
                    log.info("创建用户基本信息成功，档案ID：{}", archiveId);
                    return Result.success("创建成功", healthUserInfo);
                } else {
                    return Result.error("创建失败");
                }
            } else {
                // 如果存在，则更新记录
                healthUserInfo.setInfoId(existing.getInfoId());
                healthUserInfo.setArchiveId(archiveId);
                boolean result = healthUserInfoService.updateById(healthUserInfo);
                if (result) {
                    log.info("更新用户基本信息成功，档案ID：{}", archiveId);
                    return Result.success("更新成功", healthUserInfo);
                } else {
                    return Result.error("更新失败");
                }
            }
        } catch (Exception e) {
            log.error("更新用户基本信息失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

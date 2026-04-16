package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthUserCertificates;
import com.android.service.IHealthUserCertificatesService;
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
 * 健康档案健康服务凭证表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-user-certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "健康服务凭证", description = "健康服务凭证相关接口")
public class HealthUserCertificatesController {

    private final IHealthUserCertificatesService healthUserCertificatesService;

    /**
     * 根据健康档案ID更新健康服务凭证列表
     * 先删除该档案ID的所有凭证，然后插入新的凭证列表
     *
     * @param archiveId 档案ID
     * @param certificates 健康服务凭证列表
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新健康服务凭证列表", description = "根据健康档案ID更新健康服务凭证列表，先删除旧数据再插入新数据")
    @PutMapping("/archive/{archiveId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<List<HealthUserCertificates>> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody List<HealthUserCertificates> certificates) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 删除该archive_id的所有凭证
            LambdaQueryWrapper<HealthUserCertificates> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthUserCertificates::getArchiveId, archiveId);
            healthUserCertificatesService.remove(queryWrapper);

            // 插入新的凭证列表
            if (certificates != null && !certificates.isEmpty()) {
                for (HealthUserCertificates certificate : certificates) {
                    certificate.setArchiveId(archiveId);
                    certificate.setCertificateId(null); // 确保ID为null，让数据库自动生成
                }
                boolean result = healthUserCertificatesService.saveBatch(certificates);
                if (result) {
                    log.info("更新健康服务凭证列表成功，档案ID：{}，凭证数量：{}", archiveId, certificates.size());
                    return Result.success("更新成功", certificates);
                } else {
                    return Result.error("更新失败");
                }
            } else {
                log.info("删除所有健康服务凭证，档案ID：{}", archiveId);
                return Result.success("更新成功（已清空所有凭证）", List.of());
            }
        } catch (Exception e) {
            log.error("更新健康服务凭证列表失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

package com.android.controller;

import com.android.common.Result;
import com.android.entity.HealthUserEmergencyContacts;
import com.android.service.IHealthUserEmergencyContactsService;
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
 * 健康档案用户紧急联系人表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-user-emergency-contacts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "紧急联系人", description = "紧急联系人相关接口")
public class HealthUserEmergencyContactsController {

    private final IHealthUserEmergencyContactsService healthUserEmergencyContactsService;

    /**
     * 根据健康档案ID更新紧急联系人列表
     * 先删除该档案ID的所有联系人，然后插入新的联系人列表
     *
     * @param archiveId 档案ID
     * @param contacts 紧急联系人列表
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新紧急联系人列表", description = "根据健康档案ID更新紧急联系人列表，先删除旧数据再插入新数据")
    @PutMapping("/archive/{archiveId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<List<HealthUserEmergencyContacts>> updateByArchiveId(
            @PathVariable Integer archiveId,
            @RequestBody List<HealthUserEmergencyContacts> contacts) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }

            // 删除该archive_id的所有联系人
            LambdaQueryWrapper<HealthUserEmergencyContacts> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HealthUserEmergencyContacts::getArchiveId, archiveId);
            healthUserEmergencyContactsService.remove(queryWrapper);

            // 插入新的联系人列表
            if (contacts != null && !contacts.isEmpty()) {
                for (HealthUserEmergencyContacts contact : contacts) {
                    contact.setArchiveId(archiveId);
                    contact.setContactId(null); // 确保ID为null，让数据库自动生成
                }
                boolean result = healthUserEmergencyContactsService.saveBatch(contacts);
                if (result) {
                    log.info("更新紧急联系人列表成功，档案ID：{}，联系人数量：{}", archiveId, contacts.size());
                    return Result.success("更新成功", contacts);
                } else {
                    return Result.error("更新失败");
                }
            } else {
                log.info("删除所有紧急联系人，档案ID：{}", archiveId);
                return Result.success("更新成功（已清空所有联系人）", List.of());
            }
        } catch (Exception e) {
            log.error("更新紧急联系人列表失败，档案ID：{}", archiveId, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

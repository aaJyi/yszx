package com.android.service.impl;

import com.android.dto.HealthArchiveRequest;
import com.android.entity.HealthArchive;
import com.android.entity.HealthProfileTags;
import com.android.mapper.HealthArchiveMapper;
import com.android.mapper.HealthProfileTagsMapper;
import com.android.service.IHealthArchiveService;
import com.android.util.UserContext;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * <p>
 * 健康档案主表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthArchiveServiceImpl extends ServiceImpl<HealthArchiveMapper, HealthArchive> implements IHealthArchiveService {

    private final HealthProfileTagsMapper healthProfileTagsMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArchive addHealthArchive(HealthArchiveRequest request, Integer userId) {
        // 获取用户ID：优先使用请求参数，其次从ThreadLocal获取
        Integer finalUserId = userId;
        if (finalUserId == null) {
            Long threadLocalUserId = UserContext.getUserId();
            if (threadLocalUserId != null) {
                finalUserId = threadLocalUserId.intValue();
            }
        }

        if (finalUserId == null) {
            throw new RuntimeException("用户ID不能为空，请提供userId参数或在请求头中设置用户信息");
        }

        // 验证必填字段
        if (request.getHealthArchive().getUserName() == null || request.getHealthArchive().getUserName().trim().isEmpty()) {
            throw new RuntimeException("档案姓名不能为空");
        }

        // 创建健康档案主表对象
        HealthArchive healthArchive = request.getHealthArchive();

        // 保存健康档案主表
        boolean saveResult = this.save(healthArchive);
        if (!saveResult) {
            throw new RuntimeException("保存健康档案主表失败");
        }

        log.info("健康档案主表保存成功，档案ID：{}，用户ID：{}", healthArchive.getArchiveId(), finalUserId);

        // 保存个人健康标识表
        if (request.getHealthProfileTags() != null) {
            HealthProfileTags tags = new HealthProfileTags();
            tags.setArchiveId(healthArchive.getArchiveId());
            tags.setIsChild06(request.getHealthProfileTags().getIsChild06());
            tags.setIsElderly65(request.getHealthProfileTags().getIsElderly65());
            tags.setIsPregnant(request.getHealthProfileTags().getIsPregnant());
            tags.setPregnancyRisk(request.getHealthProfileTags().getPregnancyRisk());
            tags.setWeightStatus(request.getHealthProfileTags().getWeightStatus());
            tags.setBloodType(request.getHealthProfileTags().getBloodType());

            // 将列表转换为JSON字符串
            try {
                if (request.getHealthProfileTags().getChronicDisease() != null) {
                    tags.setChronicDisease(objectMapper.writeValueAsString(request.getHealthProfileTags().getChronicDisease()));
                }
                if (request.getHealthProfileTags().getStatutoryInfo() != null) {
                    tags.setStatutoryInfo(objectMapper.writeValueAsString(request.getHealthProfileTags().getStatutoryInfo()));
                }
            } catch (JsonProcessingException e) {
                log.error("转换JSON失败", e);
                throw new RuntimeException("转换JSON失败: " + e.getMessage());
            }

            int insertResult = healthProfileTagsMapper.insert(tags);
            if (insertResult <= 0) {
                throw new RuntimeException("保存个人健康标识表失败");
            }

            log.info("个人健康标识表保存成功，档案ID：{}", healthArchive.getArchiveId());
        }

        return healthArchive;
    }
}

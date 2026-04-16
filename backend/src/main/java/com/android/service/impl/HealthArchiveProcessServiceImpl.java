package com.android.service.impl;

import com.android.dto.HealthArchiveRequest;
import com.android.entity.HealthArchive;
import com.android.entity.HealthProfileTags;
import com.android.entity.HealthUserInfo;
import com.android.entity.HealthUserEmergencyContacts;
import com.android.entity.HealthUserCertificates;
import com.android.entity.HealthAllergyHistory;
import com.android.entity.HealthExposureHistory;
import com.android.entity.HealthDiseaseHistory;
import com.android.entity.HealthVaccinationHistory;
import com.android.entity.HealthFamilyHistory;
import com.android.entity.HealthGeneticHistory;
import com.android.entity.HealthDisabilityStatus;
import com.android.entity.HealthAiAnalysis;
import com.android.entity.HealthRecommendation;
import com.android.entity.HealthLifestyleStatus;
import com.android.entity.HealthSystemDiseaseScreening;
import com.android.entity.HealthPsychologicalAssessment;
import com.android.entity.HealthSocialRelationshipAssessment;
import com.android.entity.HealthPhysicalExamination;
import com.android.entity.FamilyMember;
import com.android.entity.FamilyArchiveMapping;
import com.android.mapper.HealthAiAnalysisMapper;
import com.android.mapper.HealthRecommendationMapper;
import com.android.service.IHealthArchiveProcessService;
import com.android.util.PdfUtil;
import com.android.util.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 健康档案处理服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthArchiveProcessServiceImpl implements IHealthArchiveProcessService {


    private final HealthArchiveServiceImpl healthArchiveServiceImpl;
    private final HealthProfileTagsServiceImpl healthProfileTagsServiceImpl;
    private final HealthUserInfoServiceImpl healthUserInfoServiceImpl;
    private final HealthUserEmergencyContactsServiceImpl healthUserEmergencyContactsServiceImpl;
    private final HealthUserCertificatesServiceImpl healthUserCertificatesServiceImpl;
    private final HealthAllergyHistoryServiceImpl healthAllergyHistoryServiceImpl;
    private final HealthExposureHistoryServiceImpl healthExposureHistoryServiceImpl;
    private final HealthDiseaseHistoryServiceImpl healthDiseaseHistoryServiceImpl;
    private final HealthVaccinationHistoryServiceImpl healthVaccinationHistoryServiceImpl;
    private final HealthFamilyHistoryServiceImpl healthFamilyHistoryServiceImpl;
    private final HealthGeneticHistoryServiceImpl healthGeneticHistoryServiceImpl;
    private final HealthDisabilityStatusServiceImpl healthDisabilityStatusServiceImpl;
    private final HealthLifestyleStatusServiceImpl healthLifestyleStatusServiceImpl;
    private final HealthSystemDiseaseScreeningServiceImpl healthSystemDiseaseScreeningServiceImpl;
    private final HealthPsychologicalAssessmentServiceImpl healthPsychologicalAssessmentServiceImpl;
    private final HealthSocialRelationshipAssessmentServiceImpl healthSocialRelationshipAssessmentServiceImpl;
    private final HealthPhysicalExaminationServiceImpl healthPhysicalExaminationServiceImpl;
    private final HealthAiAnalysisMapper healthAiAnalysisMapper;
    private final HealthRecommendationMapper healthRecommendationMapper;
    private final FamilyMemberServiceImpl familyMemberServiceImpl;
    private final FamilyArchiveMappingServiceImpl familyArchiveMappingServiceImpl;
    private final com.android.service.IRawHealthDataService rawHealthDataService;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    /** 注入自身代理，用于同类内调用 @Async 方法，确保智能体 webhook 异步执行、不阻塞前端返回 */
    @Lazy
    @Autowired
    private HealthArchiveProcessServiceImpl self;

    /**
     * Medical AI 智能体服务基础URL
     * 配置示例：medical.ai.base.url=https://60e32277.r8.cpolar.cn
     * 如果未配置，则不调用智能体接口
     */
    @Value("${medical.ai.base.url:}")
    private String medicalAiBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArchive processAndSaveHealthArchive(HealthArchiveRequest request, Integer userId, Integer familyMemberId) {
        log.info("开始处理健康档案数据，用户ID：{}，家人ID：{}", userId, familyMemberId);

        try {
            // 验证请求数据
            if (request.getHealthArchive() == null) {
                throw new RuntimeException("健康档案信息不能为空");
            }

            // 获取创建者用户ID（用于记录谁创建的档案）
            Integer creatorUserId = userId;
            if (creatorUserId == null) {
                Long threadLocalUserId = UserContext.getUserId();
                if (threadLocalUserId != null) {
                    creatorUserId = threadLocalUserId.intValue();
                }
            }
            if (creatorUserId == null) {
                throw new RuntimeException("创建者用户ID不能为空，请先登录");
            }

            // 确定档案拥有者的userId（为谁创建档案，user_id就是谁）
            Integer archiveOwnerUserId;
            String ownerPhone = null;
            Integer familyRegisteredUserId = null;

            if (familyMemberId != null) {
                // 为家人创建档案
                com.android.entity.FamilyMember familyMember = familyMemberServiceImpl.getById(familyMemberId);
                if (familyMember == null) {
                    throw new RuntimeException("家人信息不存在");
                }
                if (!familyMember.getOwnerUserId().equals(creatorUserId)) {
                    throw new RuntimeException("无权为该家人创建档案");
                }
                if (familyMember.getIsRegistered() == null || !familyMember.getIsRegistered()) {
                    throw new RuntimeException("该家人尚未注册，无法创建健康档案");
                }
                if (familyMember.getRegisteredUserId() == null) {
                    throw new RuntimeException("该家人尚未注册，无法创建健康档案");
                }
                // 使用家人的userId
                archiveOwnerUserId = familyMember.getRegisteredUserId();
                ownerPhone = familyMember.getPhone();
                familyRegisteredUserId = familyMember.getRegisteredUserId();
                log.info("为家人创建档案，创建者ID：{}，家人ID：{}，家人用户ID：{}", creatorUserId, familyMemberId, archiveOwnerUserId);
            } else {
                // 为自己创建档案
                archiveOwnerUserId = creatorUserId;
                log.info("为自己创建档案，用户ID：{}", archiveOwnerUserId);
            }

            // 验证必填字段
            if (request.getHealthArchive().getUserName() == null || request.getHealthArchive().getUserName().trim().isEmpty()) {
                throw new RuntimeException("档案姓名不能为空");
            }

            // 获取健康档案对象
            HealthArchive healthArchive = request.getHealthArchive();
            // 重要：user_id使用档案拥有者的ID（为谁创建，user_id就是谁）
            healthArchive.setUserId(archiveOwnerUserId);

            // 处理档案年份（如果提供了日期但没有年份）
            if (healthArchive.getArchiveDate() != null && healthArchive.getArchiveYear() == null) {
                healthArchive.setArchiveYear(healthArchive.getArchiveDate().getYear());
            } else if (healthArchive.getArchiveDate() == null) {
                // 如果没有提供日期，使用当前日期
                LocalDate now = LocalDate.now();
                healthArchive.setArchiveDate(now);
                healthArchive.setArchiveYear(now.getYear());
            }

            // 保存健康档案主表（在同一事务中）
            boolean saveResult = healthArchiveServiceImpl.save(healthArchive);
            if (!saveResult) {
                throw new RuntimeException("保存健康档案主表失败");
            }

            log.info("健康档案主表保存成功，档案ID：{}，用户ID：{}", healthArchive.getArchiveId(), archiveOwnerUserId);

            // 保存个人健康标识表（在同一事务中）
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

                boolean insertResult = healthProfileTagsServiceImpl.save(tags);
                if (!insertResult) {
                    throw new RuntimeException("保存个人健康标识表失败");
                }

                log.info("个人健康标识表保存成功，档案ID：{}", healthArchive.getArchiveId());
            }

            // 保存健康档案用户基本信息（在同一事务中）
            if (request.getUserInfo() != null) {
                HealthUserInfo userInfo = request.getUserInfo();
                userInfo.setArchiveId(healthArchive.getArchiveId());
                userInfo.setCreateTime(LocalDateTime.now());
                userInfo.setUpdateTime(LocalDateTime.now());

                boolean userInfoResult = healthUserInfoServiceImpl.save(userInfo);
                if (!userInfoResult) {
                    throw new RuntimeException("保存健康档案用户基本信息失败");
                }

                log.info("健康档案用户基本信息保存成功，档案ID：{}，信息ID：{}", healthArchive.getArchiveId(), userInfo.getInfoId());
            }

            // 保存健康档案用户紧急联系人（在同一事务中）
            if (request.getUserEmergencyContacts() != null) {
                HealthUserEmergencyContacts emergencyContacts = request.getUserEmergencyContacts();
                emergencyContacts.setArchiveId(healthArchive.getArchiveId());
                emergencyContacts.setCreateTime(LocalDateTime.now());
                emergencyContacts.setUpdateTime(LocalDateTime.now());

                boolean emergencyResult = healthUserEmergencyContactsServiceImpl.save(emergencyContacts);
                if (!emergencyResult) {
                    throw new RuntimeException("保存健康档案用户紧急联系人失败");
                }

                log.info("健康档案用户紧急联系人保存成功，档案ID：{}，联系人ID：{}", healthArchive.getArchiveId(), emergencyContacts.getContactId());
            }

            // 保存健康档案用户健康服务凭证（在同一事务中）
            if (request.getUserCertificates() != null) {
                HealthUserCertificates certificates = request.getUserCertificates();
                certificates.setArchiveId(healthArchive.getArchiveId());
                certificates.setCreateTime(LocalDateTime.now());
                certificates.setUpdateTime(LocalDateTime.now());

                boolean certificatesResult = healthUserCertificatesServiceImpl.save(certificates);
                if (!certificatesResult) {
                    throw new RuntimeException("保存健康档案用户健康服务凭证失败");
                }

                log.info("健康档案用户健康服务凭证保存成功，档案ID：{}，凭证ID：{}", healthArchive.getArchiveId(), certificates.getCertificateId());
            }

            // 保存健康档案过敏史（在同一事务中）
            if (request.getAllergyHistory() != null) {
                HealthAllergyHistory allergyHistory = request.getAllergyHistory();
                allergyHistory.setArchiveId(healthArchive.getArchiveId());
                allergyHistory.setCreateTime(LocalDateTime.now());
                allergyHistory.setUpdateTime(LocalDateTime.now());

                boolean allergyResult = healthAllergyHistoryServiceImpl.save(allergyHistory);
                if (!allergyResult) {
                    throw new RuntimeException("保存健康档案过敏史失败");
                }

                log.info("健康档案过敏史保存成功，档案ID：{}，过敏ID：{}", healthArchive.getArchiveId(), allergyHistory.getAllergyId());
            }

            // 保存健康档案暴露史（在同一事务中）
            if (request.getExposureHistory() != null) {
                HealthExposureHistory exposureHistory = request.getExposureHistory();
                exposureHistory.setArchiveId(healthArchive.getArchiveId());
                exposureHistory.setCreateTime(LocalDateTime.now());
                exposureHistory.setUpdateTime(LocalDateTime.now());

                boolean exposureResult = healthExposureHistoryServiceImpl.save(exposureHistory);
                if (!exposureResult) {
                    throw new RuntimeException("保存健康档案暴露史失败");
                }

                log.info("健康档案暴露史保存成功，档案ID：{}，暴露ID：{}", healthArchive.getArchiveId(), exposureHistory.getExposureId());
            }

            // 保存健康档案既往史（在同一事务中，支持列表）
            if (request.getDiseaseHistory() != null && request.getDiseaseHistory().size() > 0) {
                for (HealthDiseaseHistory diseaseHistory : request.getDiseaseHistory()) {
                    diseaseHistory.setArchiveId(healthArchive.getArchiveId());
                    diseaseHistory.setCreateTime(LocalDateTime.now());
                    diseaseHistory.setUpdateTime(LocalDateTime.now());
                    healthDiseaseHistoryServiceImpl.save(diseaseHistory);
                }
                log.info("健康档案既往史保存成功，档案ID：{}，共{}条", healthArchive.getArchiveId(), request.getDiseaseHistory().size());
            }

            // 保存健康档案预防接种史（在同一事务中，支持列表）
            if (request.getVaccinationHistory() != null && request.getVaccinationHistory().size() > 0) {
                for (HealthVaccinationHistory vaccinationHistory : request.getVaccinationHistory()) {
                    vaccinationHistory.setArchiveId(healthArchive.getArchiveId());
                    vaccinationHistory.setCreateTime(LocalDateTime.now());
                    vaccinationHistory.setUpdateTime(LocalDateTime.now());
                    healthVaccinationHistoryServiceImpl.save(vaccinationHistory);
                }
                log.info("健康档案预防接种史保存成功，档案ID：{}，共{}条", healthArchive.getArchiveId(), request.getVaccinationHistory().size());
            }

            // 保存健康档案家族史（在同一事务中，支持列表）
            if (request.getFamilyHistory() != null && request.getFamilyHistory().size() > 0) {
                for (HealthFamilyHistory familyHistory : request.getFamilyHistory()) {
                    familyHistory.setArchiveId(healthArchive.getArchiveId());
                    familyHistory.setCreateTime(LocalDateTime.now());
                    familyHistory.setUpdateTime(LocalDateTime.now());
                    
                    // 处理diseases字段（如果是数组需要转换为JSON字符串）
                    if (familyHistory.getDiseases() != null && !(familyHistory.getDiseases() instanceof String)) {
                        try {
                            familyHistory.setDiseases(objectMapper.writeValueAsString(familyHistory.getDiseases()));
                        } catch (Exception e) {
                            log.warn("家族史疾病信息转换失败", e);
                        }
                    }
                    
                    healthFamilyHistoryServiceImpl.save(familyHistory);
                }
                log.info("健康档案家族史保存成功，档案ID：{}，共{}条", healthArchive.getArchiveId(), request.getFamilyHistory().size());
            }

            // 保存健康档案遗传病史（在同一事务中）
            if (request.getGeneticHistory() != null) {
                HealthGeneticHistory geneticHistory = request.getGeneticHistory();
                geneticHistory.setArchiveId(healthArchive.getArchiveId());
                geneticHistory.setCreateTime(LocalDateTime.now());
                geneticHistory.setUpdateTime(LocalDateTime.now());

                boolean geneticResult = healthGeneticHistoryServiceImpl.save(geneticHistory);
                if (!geneticResult) {
                    throw new RuntimeException("保存健康档案遗传病史失败");
                }

                log.info("健康档案遗传病史保存成功，档案ID：{}，遗传史ID：{}", healthArchive.getArchiveId(), geneticHistory.getGeneticId());
            }

            // 保存健康档案残疾情况（在同一事务中）
            if (request.getDisabilityHistory() != null) {
                HealthDisabilityStatus disabilityStatus = request.getDisabilityHistory();
                disabilityStatus.setArchiveId(healthArchive.getArchiveId());
                disabilityStatus.setCreateTime(LocalDateTime.now());
                disabilityStatus.setUpdateTime(LocalDateTime.now());

                boolean disabilityResult = healthDisabilityStatusServiceImpl.save(disabilityStatus);
                if (!disabilityResult) {
                    throw new RuntimeException("保存健康档案残疾情况失败");
                }

                log.info("健康档案残疾情况保存成功，档案ID：{}，残疾ID：{}", healthArchive.getArchiveId(), disabilityStatus.getDisabilityId());
            }

            // 如果为家人创建档案，需要创建映射记录
            if (familyMemberId != null && ownerPhone != null) {
                FamilyArchiveMapping mapping = new FamilyArchiveMapping();
                mapping.setArchiveId(healthArchive.getArchiveId());
                mapping.setCreatorUserId(creatorUserId);
                mapping.setOwnerPhone(ownerPhone);
                mapping.setOwnerUserId(familyRegisteredUserId);
                mapping.setCreateTime(LocalDateTime.now());
                mapping.setUpdateTime(LocalDateTime.now());

                boolean mappingResult = familyArchiveMappingServiceImpl.save(mapping);
                if (!mappingResult) {
                    throw new RuntimeException("保存家人档案映射失败");
                }
                log.info("家人档案映射保存成功，档案ID：{}，创建者ID：{}，拥有者ID：{}", 
                        healthArchive.getArchiveId(), creatorUserId, familyRegisteredUserId);
            }

            log.info("健康档案处理完成，档案ID：{}，用户ID：{}", healthArchive.getArchiveId(), archiveOwnerUserId);

            return healthArchive;

        } catch (Exception e) {
            log.error("处理健康档案数据失败，用户ID：{}，家人ID：{}", userId, familyMemberId, e);
            throw e;
        }
    }

    @Override
    public List<HealthArchive> getHealthArchiveListByUserId(Integer userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        // 查询该用户的所有健康档案（包括自己创建的和他人为自己创建的）
        // 因为user_id就是档案拥有者的ID，所以直接查询即可
        LambdaQueryWrapper<HealthArchive> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HealthArchive::getUserId, userId)
                .orderByDesc(HealthArchive::getArchiveYear)
                .orderByDesc(HealthArchive::getArchiveDate);

        List<HealthArchive> archiveList = healthArchiveServiceImpl.list(queryWrapper);
        log.info("查询用户{}的健康档案列表（档案拥有者），共{}条", userId, archiveList.size());
        return archiveList;
    }

    @Override
    public List<HealthArchive> getHealthArchiveListByCreatorId(Integer creatorUserId) {
        if (creatorUserId == null) {
            throw new RuntimeException("创建者用户ID不能为空");
        }

        // 查询创建者创建的所有健康档案（包括自己和家人的）
        // 1. 查询自己创建的（user_id = creatorUserId）
        LambdaQueryWrapper<HealthArchive> selfQueryWrapper = new LambdaQueryWrapper<>();
        selfQueryWrapper.eq(HealthArchive::getUserId, creatorUserId);

        // 2. 查询为家人创建的（通过family_archive_mapping）
        LambdaQueryWrapper<FamilyArchiveMapping> mappingWrapper = new LambdaQueryWrapper<>();
        mappingWrapper.eq(FamilyArchiveMapping::getCreatorUserId, creatorUserId);
        List<FamilyArchiveMapping> mappings = familyArchiveMappingServiceImpl.list(mappingWrapper);

        // 合并结果
        List<HealthArchive> archiveList = healthArchiveServiceImpl.list(selfQueryWrapper);

        // 添加为家人创建的档案
        for (FamilyArchiveMapping mapping : mappings) {
            HealthArchive archive = healthArchiveServiceImpl.getById(mapping.getArchiveId());
            if (archive != null && !archiveList.contains(archive)) {
                archiveList.add(archive);
            }
        }

        // 排序：按年份和创建时间降序
        archiveList.sort((a, b) -> {
            int yearCompare = Integer.compare(
                    b.getArchiveYear() != null ? b.getArchiveYear() : 0,
                    a.getArchiveYear() != null ? a.getArchiveYear() : 0
            );
            if (yearCompare != 0) {
                return yearCompare;
            }
            if (a.getArchiveDate() != null && b.getArchiveDate() != null) {
                return b.getArchiveDate().compareTo(a.getArchiveDate());
            }
            return 0;
        });

        log.info("查询用户{}创建的健康档案列表（创建者），共{}条", creatorUserId, archiveList.size());
        return archiveList;
    }

    @Override
    public Map<String, Object> getHealthArchiveDetailById(Integer archiveId) {
        if (archiveId == null) {
            throw new RuntimeException("档案ID不能为空");
        }

        Map<String, Object> result = new HashMap<>();

        // 查询健康档案主表
        HealthArchive healthArchive = healthArchiveServiceImpl.getById(archiveId);
        if (healthArchive == null) {
            throw new RuntimeException("健康档案不存在");
        }
        result.put("healthArchive", healthArchive);

        // 查询个人健康标识表
        LambdaQueryWrapper<HealthProfileTags> tagsWrapper = new LambdaQueryWrapper<>();
        tagsWrapper.eq(HealthProfileTags::getArchiveId, archiveId);
        HealthProfileTags tags = healthProfileTagsServiceImpl.getOne(tagsWrapper);
        result.put("healthProfileTags", tags);

        // 查询健康档案用户基本信息（按ID降序排序，取最新的记录）
        LambdaQueryWrapper<HealthUserInfo> userInfoWrapper = new LambdaQueryWrapper<>();
        userInfoWrapper.eq(HealthUserInfo::getArchiveId, archiveId)
                .orderByDesc(HealthUserInfo::getInfoId)
                .last("LIMIT 1");
        HealthUserInfo userInfo = healthUserInfoServiceImpl.getOne(userInfoWrapper);
        result.put("userInfo", userInfo);

        // 查询健康档案用户紧急联系人（按ID降序排序，取最新的记录）
        LambdaQueryWrapper<HealthUserEmergencyContacts> emergencyWrapper = new LambdaQueryWrapper<>();
        emergencyWrapper.eq(HealthUserEmergencyContacts::getArchiveId, archiveId)
                .orderByDesc(HealthUserEmergencyContacts::getContactId)
                .last("LIMIT 1");
        HealthUserEmergencyContacts emergencyContacts = healthUserEmergencyContactsServiceImpl.getOne(emergencyWrapper);
        result.put("userEmergencyContacts", emergencyContacts);

        // 查询健康档案用户健康服务凭证（按ID降序排序，取最新的记录）
        LambdaQueryWrapper<HealthUserCertificates> certificatesWrapper = new LambdaQueryWrapper<>();
        certificatesWrapper.eq(HealthUserCertificates::getArchiveId, archiveId)
                .orderByDesc(HealthUserCertificates::getCertificateId)
                .last("LIMIT 1");
        HealthUserCertificates certificates = healthUserCertificatesServiceImpl.getOne(certificatesWrapper);
        result.put("userCertificates", certificates);

        // 查询健康档案过敏史（每个档案只有一条记录，有唯一约束）
        LambdaQueryWrapper<HealthAllergyHistory> allergyWrapper = new LambdaQueryWrapper<>();
        allergyWrapper.eq(HealthAllergyHistory::getArchiveId, archiveId)
                .orderByDesc(HealthAllergyHistory::getAllergyId)
                .last("LIMIT 1");
        HealthAllergyHistory allergyHistory = healthAllergyHistoryServiceImpl.getOne(allergyWrapper);
        result.put("allergyHistory", allergyHistory);
        log.debug("查询过敏史，档案ID：{}，结果：{}", archiveId, allergyHistory != null ? "有数据" : "无数据");

        // 查询健康档案暴露史（每个档案只有一条记录，有唯一约束）
        LambdaQueryWrapper<HealthExposureHistory> exposureWrapper = new LambdaQueryWrapper<>();
        exposureWrapper.eq(HealthExposureHistory::getArchiveId, archiveId)
                .orderByDesc(HealthExposureHistory::getExposureId)
                .last("LIMIT 1");
        HealthExposureHistory exposureHistory = healthExposureHistoryServiceImpl.getOne(exposureWrapper);
        result.put("exposureHistory", exposureHistory);
        log.debug("查询暴露史，档案ID：{}，结果：{}", archiveId, exposureHistory != null ? "有数据" : "无数据");

        // 查询健康档案既往史（查询所有记录，因为一个档案可以有多条既往史）
        LambdaQueryWrapper<HealthDiseaseHistory> diseaseWrapper = new LambdaQueryWrapper<>();
        diseaseWrapper.eq(HealthDiseaseHistory::getArchiveId, archiveId)
                .orderByDesc(HealthDiseaseHistory::getDiseaseId);
        List<HealthDiseaseHistory> diseaseHistoryList = healthDiseaseHistoryServiceImpl.list(diseaseWrapper);
        result.put("diseaseHistory", diseaseHistoryList); // 始终返回列表，即使为空
        result.put("diseaseHistoryList", diseaseHistoryList); // 同时提供列表格式
        log.debug("查询既往史，档案ID：{}，记录数：{}", archiveId, diseaseHistoryList.size());

        // 查询健康档案预防接种史（查询所有记录，因为一个档案可以有多条接种史）
        LambdaQueryWrapper<HealthVaccinationHistory> vaccinationWrapper = new LambdaQueryWrapper<>();
        vaccinationWrapper.eq(HealthVaccinationHistory::getArchiveId, archiveId)
                .orderByDesc(HealthVaccinationHistory::getVaccinationId);
        List<HealthVaccinationHistory> vaccinationHistoryList = healthVaccinationHistoryServiceImpl.list(vaccinationWrapper);
        result.put("vaccinationHistory", vaccinationHistoryList); // 始终返回列表，即使为空
        result.put("vaccinationHistoryList", vaccinationHistoryList); // 同时提供列表格式
        log.debug("查询预防接种史，档案ID：{}，记录数：{}", archiveId, vaccinationHistoryList.size());

        // 查询健康档案家族史（查询所有记录，因为一个档案可以有多条家族史）
        LambdaQueryWrapper<HealthFamilyHistory> familyWrapper = new LambdaQueryWrapper<>();
        familyWrapper.eq(HealthFamilyHistory::getArchiveId, archiveId)
                .orderByDesc(HealthFamilyHistory::getFamilyId);
        List<HealthFamilyHistory> familyHistoryList = healthFamilyHistoryServiceImpl.list(familyWrapper);
        result.put("familyHistory", familyHistoryList); // 始终返回列表，即使为空
        result.put("familyHistoryList", familyHistoryList); // 同时提供列表格式
        log.debug("查询家族史，档案ID：{}，记录数：{}", archiveId, familyHistoryList.size());

        // 查询健康档案遗传病史（每个档案只有一条记录，有唯一约束）
        LambdaQueryWrapper<HealthGeneticHistory> geneticWrapper = new LambdaQueryWrapper<>();
        geneticWrapper.eq(HealthGeneticHistory::getArchiveId, archiveId)
                .orderByDesc(HealthGeneticHistory::getGeneticId)
                .last("LIMIT 1");
        HealthGeneticHistory geneticHistory = healthGeneticHistoryServiceImpl.getOne(geneticWrapper);
        result.put("geneticHistory", geneticHistory);
        log.debug("查询遗传病史，档案ID：{}，结果：{}", archiveId, geneticHistory != null ? "有数据" : "无数据");

        // 查询健康档案残疾情况（每个档案只有一条记录，有唯一约束）
        LambdaQueryWrapper<HealthDisabilityStatus> disabilityWrapper = new LambdaQueryWrapper<>();
        disabilityWrapper.eq(HealthDisabilityStatus::getArchiveId, archiveId)
                .orderByDesc(HealthDisabilityStatus::getDisabilityId)
                .last("LIMIT 1");
        HealthDisabilityStatus disabilityStatus = healthDisabilityStatusServiceImpl.getOne(disabilityWrapper);
        result.put("disabilityHistory", disabilityStatus);
        log.debug("查询残疾情况，档案ID：{}，结果：{}", archiveId, disabilityStatus != null ? "有数据" : "无数据");

        // 查询生活方式状态表（按ID降序排序，取最新的记录）
        LambdaQueryWrapper<HealthLifestyleStatus> lifestyleWrapper = new LambdaQueryWrapper<>();
        lifestyleWrapper.eq(HealthLifestyleStatus::getArchiveId, archiveId)
                .orderByDesc(HealthLifestyleStatus::getLifestyleId)
                .last("LIMIT 1");
        HealthLifestyleStatus lifestyleStatus = healthLifestyleStatusServiceImpl.getOne(lifestyleWrapper);
        result.put("lifestyleStatus", lifestyleStatus);

        // 查询系统疾病与症状筛查表（按ID降序排序，取最新的记录）
        LambdaQueryWrapper<HealthSystemDiseaseScreening> screeningWrapper = new LambdaQueryWrapper<>();
        screeningWrapper.eq(HealthSystemDiseaseScreening::getArchiveId, archiveId)
                .orderByDesc(HealthSystemDiseaseScreening::getScreeningId)
                .last("LIMIT 1");
        HealthSystemDiseaseScreening systemScreening = healthSystemDiseaseScreeningServiceImpl.getOne(screeningWrapper);
        result.put("systemDiseaseScreening", systemScreening);

        // 查询心理评估表（按ID降序排序，取最新的记录）
        LambdaQueryWrapper<HealthPsychologicalAssessment> psychologicalWrapper = new LambdaQueryWrapper<>();
        psychologicalWrapper.eq(HealthPsychologicalAssessment::getArchiveId, archiveId)
                .orderByDesc(HealthPsychologicalAssessment::getAssessmentId)
                .last("LIMIT 1");
        HealthPsychologicalAssessment psychologicalAssessment = healthPsychologicalAssessmentServiceImpl.getOne(psychologicalWrapper);
        result.put("psychologicalAssessment", psychologicalAssessment);

        // 查询社会关系评估表（按ID降序排序，取最新的记录）
        LambdaQueryWrapper<HealthSocialRelationshipAssessment> socialWrapper = new LambdaQueryWrapper<>();
        socialWrapper.eq(HealthSocialRelationshipAssessment::getArchiveId, archiveId)
                .orderByDesc(HealthSocialRelationshipAssessment::getAssessmentId)
                .last("LIMIT 1");
        HealthSocialRelationshipAssessment socialAssessment = healthSocialRelationshipAssessmentServiceImpl.getOne(socialWrapper);
        result.put("socialRelationshipAssessment", socialAssessment);

        // 查询体格检查表（按ID降序排序，取最新的记录）
        LambdaQueryWrapper<HealthPhysicalExamination> physicalWrapper = new LambdaQueryWrapper<>();
        physicalWrapper.eq(HealthPhysicalExamination::getArchiveId, archiveId)
                .orderByDesc(HealthPhysicalExamination::getExamId)
                .last("LIMIT 1");
        HealthPhysicalExamination physicalExamination = healthPhysicalExaminationServiceImpl.getOne(physicalWrapper);
        result.put("physicalExamination", physicalExamination);

        // 查询该档案ID对应的最新健康总结（按总结ID降序排序，取最新的总结ID）
        LambdaQueryWrapper<HealthAiAnalysis> analysisWrapper = new LambdaQueryWrapper<>();
        analysisWrapper.eq(HealthAiAnalysis::getArchiveId, archiveId)
                .orderByDesc(HealthAiAnalysis::getAnalysisId)
                .last("LIMIT 1");
        HealthAiAnalysis latestAnalysis = healthAiAnalysisMapper.selectOne(analysisWrapper);
        if (latestAnalysis != null) {
            // 获取最新的总结ID
            Long latestAnalysisId = latestAnalysis.getAnalysisId();
            result.put("healthSummary", latestAnalysis);

            // 根据最新的总结ID，查询该总结ID对应的所有健康建议
            LambdaQueryWrapper<HealthRecommendation> recommendationWrapper = new LambdaQueryWrapper<>();
            recommendationWrapper.eq(HealthRecommendation::getAnalysisId, latestAnalysisId)
                    .orderByDesc(HealthRecommendation::getPriority)
                    .orderByAsc(HealthRecommendation::getCreateTime);
            List<HealthRecommendation> recommendations = healthRecommendationMapper.selectList(recommendationWrapper);
            
            // 如果有建议，则添加到结果中；如果没有，则不添加该字段（前端判断为空则不显示）
            if (recommendations != null && !recommendations.isEmpty()) {
                result.put("healthRecommendations", recommendations);
            }
        }
        // 如果没有健康总结，则不添加healthSummary和healthRecommendations字段（前端判断为空则不显示）

        log.info("查询健康档案详情，档案ID：{}", archiveId);
        return result;
    }

    @Override
    public byte[] generateHealthArchivePdf(Map<String, Object> archiveData) throws Exception {
        return PdfUtil.generateHealthArchivePdf(archiveData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArchive updateHealthArchive(Integer archiveId, HealthArchiveRequest request, Integer userId) throws JsonProcessingException {
        log.info("开始更新健康档案数据，档案ID：{}，用户ID：{}", archiveId, userId);

        try {
            // 验证请求数据
            if (request == null) {
                throw new RuntimeException("健康档案请求数据不能为空");
            }

            // 获取用户ID
            Integer finalUserId = userId;
            if (finalUserId == null) {
                Long threadLocalUserId = UserContext.getUserId();
                if (threadLocalUserId != null) {
                    finalUserId = threadLocalUserId.intValue();
                }
            }
            if (finalUserId == null) {
                throw new RuntimeException("用户ID不能为空，请先登录");
            }

            // 验证档案是否存在
            HealthArchive existingArchive = healthArchiveServiceImpl.getById(archiveId);
            if (existingArchive == null) {
                throw new RuntimeException("健康档案不存在");
            }

            // 验证权限（只有档案拥有者或创建者可以修改）
            // 这里简化处理，只验证是否为档案拥有者
            if (!existingArchive.getUserId().equals(finalUserId)) {
                throw new RuntimeException("无权修改该健康档案");
            }

            // 更新健康档案主表
            if (request.getHealthArchive() != null) {
                HealthArchive healthArchive = request.getHealthArchive();
                healthArchive.setArchiveId(archiveId);
                healthArchive.setUserId(existingArchive.getUserId()); // 保持原有userId
                
                // 处理档案年份
                if (healthArchive.getArchiveDate() != null && healthArchive.getArchiveYear() == null) {
                    healthArchive.setArchiveYear(healthArchive.getArchiveDate().getYear());
                }
                
                boolean updateResult = healthArchiveServiceImpl.updateById(healthArchive);
                if (!updateResult) {
                    throw new RuntimeException("更新健康档案主表失败");
                }
                log.info("健康档案主表更新成功，档案ID：{}", archiveId);
            }

            // 更新个人健康标识表
            if (request.getHealthProfileTags() != null) {
                LambdaQueryWrapper<HealthProfileTags> tagsWrapper = new LambdaQueryWrapper<>();
                tagsWrapper.eq(HealthProfileTags::getArchiveId, archiveId);
                HealthProfileTags existingTags = healthProfileTagsServiceImpl.getOne(tagsWrapper);
                
                if (existingTags != null) {
                    // 更新现有记录（使用主键更新）
                    HealthProfileTags tags = new HealthProfileTags();
                    tags.setTagId(existingTags.getTagId()); // 设置主键ID
                    tags.setArchiveId(archiveId);
                    tags.setIsChild06(request.getHealthProfileTags().getIsChild06());
                    tags.setIsElderly65(request.getHealthProfileTags().getIsElderly65());
                    tags.setIsPregnant(request.getHealthProfileTags().getIsPregnant());
                    tags.setPregnancyRisk(request.getHealthProfileTags().getPregnancyRisk());
                    tags.setWeightStatus(request.getHealthProfileTags().getWeightStatus());
                    tags.setBloodType(request.getHealthProfileTags().getBloodType());

                    if (request.getHealthProfileTags().getChronicDisease() != null) {
                        tags.setChronicDisease(objectMapper.writeValueAsString(request.getHealthProfileTags().getChronicDisease()));
                    }
                    if (request.getHealthProfileTags().getStatutoryInfo() != null) {
                        tags.setStatutoryInfo(objectMapper.writeValueAsString(request.getHealthProfileTags().getStatutoryInfo()));
                    }

                    boolean updateResult = healthProfileTagsServiceImpl.updateById(tags);
                    if (!updateResult) {
                        throw new RuntimeException("更新个人健康标识表失败");
                    }
                } else {
                    // 创建新记录
                    HealthProfileTags tags = new HealthProfileTags();
                    tags.setArchiveId(archiveId);
                    tags.setIsChild06(request.getHealthProfileTags().getIsChild06());
                    tags.setIsElderly65(request.getHealthProfileTags().getIsElderly65());
                    tags.setIsPregnant(request.getHealthProfileTags().getIsPregnant());
                    tags.setPregnancyRisk(request.getHealthProfileTags().getPregnancyRisk());
                    tags.setWeightStatus(request.getHealthProfileTags().getWeightStatus());
                    tags.setBloodType(request.getHealthProfileTags().getBloodType());

                    if (request.getHealthProfileTags().getChronicDisease() != null) {
                        tags.setChronicDisease(objectMapper.writeValueAsString(request.getHealthProfileTags().getChronicDisease()));
                    }
                    if (request.getHealthProfileTags().getStatutoryInfo() != null) {
                        tags.setStatutoryInfo(objectMapper.writeValueAsString(request.getHealthProfileTags().getStatutoryInfo()));
                    }

                    boolean saveResult = healthProfileTagsServiceImpl.save(tags);
                    if (!saveResult) {
                        throw new RuntimeException("保存个人健康标识表失败");
                    }
                }
                log.info("个人健康标识表更新成功，档案ID：{}", archiveId);
            }

            // 更新用户基本信息
            if (request.getUserInfo() != null) {
                LambdaQueryWrapper<HealthUserInfo> userInfoWrapper = new LambdaQueryWrapper<>();
                userInfoWrapper.eq(HealthUserInfo::getArchiveId, archiveId)
                        .orderByDesc(HealthUserInfo::getInfoId)
                        .last("LIMIT 1");
                HealthUserInfo existingUserInfo = healthUserInfoServiceImpl.getOne(userInfoWrapper);
                
                if (existingUserInfo != null) {
                    // 更新现有记录
                    HealthUserInfo userInfo = request.getUserInfo();
                    userInfo.setInfoId(existingUserInfo.getInfoId());
                    userInfo.setArchiveId(archiveId);
                    userInfo.setUpdateTime(LocalDateTime.now());
                    
                    boolean updateResult = healthUserInfoServiceImpl.updateById(userInfo);
                    if (!updateResult) {
                        throw new RuntimeException("更新用户基本信息失败");
                    }
                } else {
                    // 创建新记录
                    HealthUserInfo userInfo = request.getUserInfo();
                    userInfo.setArchiveId(archiveId);
                    userInfo.setCreateTime(LocalDateTime.now());
                    userInfo.setUpdateTime(LocalDateTime.now());
                    
                    boolean saveResult = healthUserInfoServiceImpl.save(userInfo);
                    if (!saveResult) {
                        throw new RuntimeException("保存用户基本信息失败");
                    }
                }
                log.info("用户基本信息更新成功，档案ID：{}", archiveId);
            }

            // 更新紧急联系人
            if (request.getUserEmergencyContacts() != null) {
                LambdaQueryWrapper<HealthUserEmergencyContacts> emergencyWrapper = new LambdaQueryWrapper<>();
                emergencyWrapper.eq(HealthUserEmergencyContacts::getArchiveId, archiveId)
                        .orderByDesc(HealthUserEmergencyContacts::getContactId)
                        .last("LIMIT 1");
                HealthUserEmergencyContacts existingContact = healthUserEmergencyContactsServiceImpl.getOne(emergencyWrapper);
                
                if (existingContact != null) {
                    HealthUserEmergencyContacts emergencyContacts = request.getUserEmergencyContacts();
                    emergencyContacts.setContactId(existingContact.getContactId());
                    emergencyContacts.setArchiveId(archiveId);
                    emergencyContacts.setUpdateTime(LocalDateTime.now());
                    
                    boolean updateResult = healthUserEmergencyContactsServiceImpl.updateById(emergencyContacts);
                    if (!updateResult) {
                        throw new RuntimeException("更新紧急联系人失败");
                    }
                } else {
                    HealthUserEmergencyContacts emergencyContacts = request.getUserEmergencyContacts();
                    emergencyContacts.setArchiveId(archiveId);
                    emergencyContacts.setCreateTime(LocalDateTime.now());
                    emergencyContacts.setUpdateTime(LocalDateTime.now());
                    
                    boolean saveResult = healthUserEmergencyContactsServiceImpl.save(emergencyContacts);
                    if (!saveResult) {
                        throw new RuntimeException("保存紧急联系人失败");
                    }
                }
                log.info("紧急联系人更新成功，档案ID：{}", archiveId);
            }

            // 更新健康服务凭证
            if (request.getUserCertificates() != null) {
                LambdaQueryWrapper<HealthUserCertificates> certificatesWrapper = new LambdaQueryWrapper<>();
                certificatesWrapper.eq(HealthUserCertificates::getArchiveId, archiveId)
                        .orderByDesc(HealthUserCertificates::getCertificateId)
                        .last("LIMIT 1");
                HealthUserCertificates existingCertificate = healthUserCertificatesServiceImpl.getOne(certificatesWrapper);
                
                if (existingCertificate != null) {
                    HealthUserCertificates certificates = request.getUserCertificates();
                    certificates.setCertificateId(existingCertificate.getCertificateId());
                    certificates.setArchiveId(archiveId);
                    certificates.setUpdateTime(LocalDateTime.now());
                    
                    boolean updateResult = healthUserCertificatesServiceImpl.updateById(certificates);
                    if (!updateResult) {
                        throw new RuntimeException("更新健康服务凭证失败");
                    }
                } else {
                    HealthUserCertificates certificates = request.getUserCertificates();
                    certificates.setArchiveId(archiveId);
                    certificates.setCreateTime(LocalDateTime.now());
                    certificates.setUpdateTime(LocalDateTime.now());
                    
                    boolean saveResult = healthUserCertificatesServiceImpl.save(certificates);
                    if (!saveResult) {
                        throw new RuntimeException("保存健康服务凭证失败");
                    }
                }
                log.info("健康服务凭证更新成功，档案ID：{}", archiveId);
            }

            // 更新过敏史
            if (request.getAllergyHistory() != null) {
                LambdaQueryWrapper<HealthAllergyHistory> allergyWrapper = new LambdaQueryWrapper<>();
                allergyWrapper.eq(HealthAllergyHistory::getArchiveId, archiveId);
                HealthAllergyHistory existingAllergy = healthAllergyHistoryServiceImpl.getOne(allergyWrapper);
                
                if (existingAllergy != null) {
                    HealthAllergyHistory allergyHistory = request.getAllergyHistory();
                    allergyHistory.setAllergyId(existingAllergy.getAllergyId());
                    allergyHistory.setArchiveId(archiveId);
                    allergyHistory.setUpdateTime(LocalDateTime.now());
                    
                    boolean updateResult = healthAllergyHistoryServiceImpl.updateById(allergyHistory);
                    if (!updateResult) {
                        throw new RuntimeException("更新过敏史失败");
                    }
                } else {
                    HealthAllergyHistory allergyHistory = request.getAllergyHistory();
                    allergyHistory.setArchiveId(archiveId);
                    allergyHistory.setCreateTime(LocalDateTime.now());
                    allergyHistory.setUpdateTime(LocalDateTime.now());
                    
                    boolean saveResult = healthAllergyHistoryServiceImpl.save(allergyHistory);
                    if (!saveResult) {
                        throw new RuntimeException("保存过敏史失败");
                    }
                }
                log.info("过敏史更新成功，档案ID：{}", archiveId);
            }

            // 更新暴露史
            if (request.getExposureHistory() != null) {
                LambdaQueryWrapper<HealthExposureHistory> exposureWrapper = new LambdaQueryWrapper<>();
                exposureWrapper.eq(HealthExposureHistory::getArchiveId, archiveId);
                HealthExposureHistory existingExposure = healthExposureHistoryServiceImpl.getOne(exposureWrapper);
                
                if (existingExposure != null) {
                    HealthExposureHistory exposureHistory = request.getExposureHistory();
                    exposureHistory.setExposureId(existingExposure.getExposureId());
                    exposureHistory.setArchiveId(archiveId);
                    exposureHistory.setUpdateTime(LocalDateTime.now());
                    
                    boolean updateResult = healthExposureHistoryServiceImpl.updateById(exposureHistory);
                    if (!updateResult) {
                        throw new RuntimeException("更新暴露史失败");
                    }
                } else {
                    HealthExposureHistory exposureHistory = request.getExposureHistory();
                    exposureHistory.setArchiveId(archiveId);
                    exposureHistory.setCreateTime(LocalDateTime.now());
                    exposureHistory.setUpdateTime(LocalDateTime.now());
                    
                    boolean saveResult = healthExposureHistoryServiceImpl.save(exposureHistory);
                    if (!saveResult) {
                        throw new RuntimeException("保存暴露史失败");
                    }
                }
                log.info("暴露史更新成功，档案ID：{}", archiveId);
            }

            // 更新既往史（删除旧记录，插入新记录）
            if (request.getDiseaseHistory() != null && request.getDiseaseHistory().size() > 0) {
                // 删除旧的既往史记录
                LambdaQueryWrapper<HealthDiseaseHistory> diseaseWrapper = new LambdaQueryWrapper<>();
                diseaseWrapper.eq(HealthDiseaseHistory::getArchiveId, archiveId);
                healthDiseaseHistoryServiceImpl.remove(diseaseWrapper);
                
                // 插入新的既往史记录
                for (HealthDiseaseHistory diseaseHistory : request.getDiseaseHistory()) {
                    diseaseHistory.setArchiveId(archiveId);
                    diseaseHistory.setCreateTime(LocalDateTime.now());
                    diseaseHistory.setUpdateTime(LocalDateTime.now());
                    healthDiseaseHistoryServiceImpl.save(diseaseHistory);
                }
                log.info("既往史更新成功，档案ID：{}，共{}条", archiveId, request.getDiseaseHistory().size());
            }

            // 更新预防接种史（删除旧记录，插入新记录）
            if (request.getVaccinationHistory() != null && request.getVaccinationHistory().size() > 0) {
                // 删除旧的预防接种史记录
                LambdaQueryWrapper<HealthVaccinationHistory> vaccinationWrapper = new LambdaQueryWrapper<>();
                vaccinationWrapper.eq(HealthVaccinationHistory::getArchiveId, archiveId);
                healthVaccinationHistoryServiceImpl.remove(vaccinationWrapper);
                
                // 插入新的预防接种史记录
                for (HealthVaccinationHistory vaccinationHistory : request.getVaccinationHistory()) {
                    vaccinationHistory.setArchiveId(archiveId);
                    vaccinationHistory.setCreateTime(LocalDateTime.now());
                    vaccinationHistory.setUpdateTime(LocalDateTime.now());
                    healthVaccinationHistoryServiceImpl.save(vaccinationHistory);
                }
                log.info("预防接种史更新成功，档案ID：{}，共{}条", archiveId, request.getVaccinationHistory().size());
            }

            // 更新家族史（删除旧记录，插入新记录）
            if (request.getFamilyHistory() != null && request.getFamilyHistory().size() > 0) {
                // 删除旧的家族史记录
                LambdaQueryWrapper<HealthFamilyHistory> familyWrapper = new LambdaQueryWrapper<>();
                familyWrapper.eq(HealthFamilyHistory::getArchiveId, archiveId);
                healthFamilyHistoryServiceImpl.remove(familyWrapper);
                
                // 插入新的家族史记录
                for (HealthFamilyHistory familyHistory : request.getFamilyHistory()) {
                    familyHistory.setArchiveId(archiveId);
                    familyHistory.setCreateTime(LocalDateTime.now());
                    familyHistory.setUpdateTime(LocalDateTime.now());
                    
                    // 处理diseases字段（如果是字符串需要转换为JSON）
                    if (familyHistory.getDiseases() != null && !(familyHistory.getDiseases() instanceof String)) {
                        try {
                            familyHistory.setDiseases(objectMapper.writeValueAsString(familyHistory.getDiseases()));
                        } catch (Exception e) {
                            log.warn("家族史疾病信息转换失败", e);
                        }
                    }
                    
                    healthFamilyHistoryServiceImpl.save(familyHistory);
                }
                log.info("家族史更新成功，档案ID：{}，共{}条", archiveId, request.getFamilyHistory().size());
            }

            // 更新遗传病史
            if (request.getGeneticHistory() != null) {
                LambdaQueryWrapper<HealthGeneticHistory> geneticWrapper = new LambdaQueryWrapper<>();
                geneticWrapper.eq(HealthGeneticHistory::getArchiveId, archiveId);
                HealthGeneticHistory existingGenetic = healthGeneticHistoryServiceImpl.getOne(geneticWrapper);
                
                if (existingGenetic != null) {
                    HealthGeneticHistory geneticHistory = request.getGeneticHistory();
                    geneticHistory.setGeneticId(existingGenetic.getGeneticId());
                    geneticHistory.setArchiveId(archiveId);
                    geneticHistory.setUpdateTime(LocalDateTime.now());
                    
                    boolean updateResult = healthGeneticHistoryServiceImpl.updateById(geneticHistory);
                    if (!updateResult) {
                        throw new RuntimeException("更新遗传病史失败");
                    }
                } else {
                    HealthGeneticHistory geneticHistory = request.getGeneticHistory();
                    geneticHistory.setArchiveId(archiveId);
                    geneticHistory.setCreateTime(LocalDateTime.now());
                    geneticHistory.setUpdateTime(LocalDateTime.now());
                    
                    boolean saveResult = healthGeneticHistoryServiceImpl.save(geneticHistory);
                    if (!saveResult) {
                        throw new RuntimeException("保存遗传病史失败");
                    }
                }
                log.info("遗传病史更新成功，档案ID：{}", archiveId);
            }

            // 更新残疾情况
            if (request.getDisabilityHistory() != null) {
                LambdaQueryWrapper<HealthDisabilityStatus> disabilityWrapper = new LambdaQueryWrapper<>();
                disabilityWrapper.eq(HealthDisabilityStatus::getArchiveId, archiveId);
                HealthDisabilityStatus existingDisability = healthDisabilityStatusServiceImpl.getOne(disabilityWrapper);
                
                if (existingDisability != null) {
                    HealthDisabilityStatus disabilityStatus = request.getDisabilityHistory();
                    disabilityStatus.setDisabilityId(existingDisability.getDisabilityId());
                    disabilityStatus.setArchiveId(archiveId);
                    disabilityStatus.setUpdateTime(LocalDateTime.now());
                    
                    // 处理disabilityTypes字段（如果是数组需要转换为JSON字符串）
                    if (disabilityStatus.getDisabilityTypes() != null && !(disabilityStatus.getDisabilityTypes() instanceof String)) {
                        try {
                            disabilityStatus.setDisabilityTypes(objectMapper.writeValueAsString(disabilityStatus.getDisabilityTypes()));
                        } catch (Exception e) {
                            log.warn("残疾类型转换失败", e);
                        }
                    }
                    
                    boolean updateResult = healthDisabilityStatusServiceImpl.updateById(disabilityStatus);
                    if (!updateResult) {
                        throw new RuntimeException("更新残疾情况失败");
                    }
                } else {
                    HealthDisabilityStatus disabilityStatus = request.getDisabilityHistory();
                    disabilityStatus.setArchiveId(archiveId);
                    disabilityStatus.setCreateTime(LocalDateTime.now());
                    disabilityStatus.setUpdateTime(LocalDateTime.now());
                    
                    // 处理disabilityTypes字段
                    if (disabilityStatus.getDisabilityTypes() != null && !(disabilityStatus.getDisabilityTypes() instanceof String)) {
                        try {
                            disabilityStatus.setDisabilityTypes(objectMapper.writeValueAsString(disabilityStatus.getDisabilityTypes()));
                        } catch (Exception e) {
                            log.warn("残疾类型转换失败", e);
                        }
                    }
                    
                    boolean saveResult = healthDisabilityStatusServiceImpl.save(disabilityStatus);
                    if (!saveResult) {
                        throw new RuntimeException("保存残疾情况失败");
                    }
                }
                log.info("残疾情况更新成功，档案ID：{}", archiveId);
            }

            // 重新查询更新后的健康档案
            HealthArchive updatedArchive = healthArchiveServiceImpl.getById(archiveId);
            log.info("健康档案更新完成，档案ID：{}，用户ID：{}", archiveId, finalUserId);

            return updatedArchive;

        } catch (Exception e) {
            log.error("更新健康档案数据失败，档案ID：{}，用户ID：{}", archiveId, userId, e);
            throw e;
        }
    }

    @Override
    public List<String> getChronicDiseaseList(Integer userId) {
        // 如果userId为空，尝试从UserContext获取
        Integer finalUserId = userId;
        if (finalUserId == null) {
            Long threadLocalUserId = UserContext.getUserId();
            if (threadLocalUserId == null) {
                throw new RuntimeException("用户ID不能为空，请提供userId参数或确保已登录");
            }
            finalUserId = threadLocalUserId.intValue();
        }

        log.info("查询用户{}的最新健康档案中的慢病列表", finalUserId);

        // 1. 查询用户最新的健康档案（按年份和日期降序排列，取第一条）
        LambdaQueryWrapper<HealthArchive> archiveWrapper = new LambdaQueryWrapper<>();
        archiveWrapper.eq(HealthArchive::getUserId, finalUserId)
                .orderByDesc(HealthArchive::getArchiveYear)
                .orderByDesc(HealthArchive::getArchiveDate)
                .last("LIMIT 1");

        HealthArchive latestArchive = healthArchiveServiceImpl.getOne(archiveWrapper);

        if (latestArchive == null) {
            log.warn("用户{}没有健康档案", finalUserId);
            return List.of(); // 返回空列表
        }

        log.info("找到用户{}的最新健康档案，档案ID：{}", finalUserId, latestArchive.getArchiveId());

        // 2. 根据档案ID查询健康标识表中的慢病信息
        LambdaQueryWrapper<HealthProfileTags> tagsWrapper = new LambdaQueryWrapper<>();
        tagsWrapper.eq(HealthProfileTags::getArchiveId, latestArchive.getArchiveId());
        HealthProfileTags tags = healthProfileTagsServiceImpl.getOne(tagsWrapper);

        if (tags == null || tags.getChronicDisease() == null || tags.getChronicDisease().trim().isEmpty()) {
            log.info("用户{}的最新健康档案中没有慢病信息", finalUserId);
            return List.of(); // 返回空列表
        }

        // 3. 解析JSON格式的慢病数据
        try {
            // chronic_disease字段存储的是JSON数组字符串，例如：["高血压", "2型糖尿病", "脑卒中"]
            List<String> chronicDiseaseList = objectMapper.readValue(
                    tags.getChronicDisease(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            log.info("用户{}的慢病列表：{}", finalUserId, chronicDiseaseList);
            return chronicDiseaseList;
        } catch (JsonProcessingException e) {
            log.error("解析慢病JSON数据失败，用户ID：{}，档案ID：{}，慢病数据：{}", 
                    finalUserId, latestArchive.getArchiveId(), tags.getChronicDisease(), e);
            throw new RuntimeException("解析慢病数据失败: " + e.getMessage());
        }
    }

    @Override
    public boolean updateArchiveById(HealthArchive healthArchive) {
        if (healthArchive == null || healthArchive.getArchiveId() == null) {
            throw new RuntimeException("健康档案对象或档案ID不能为空");
        }
        
        boolean result = healthArchiveServiceImpl.updateById(healthArchive);
        log.info("更新健康档案主表，档案ID：{}，结果：{}", healthArchive.getArchiveId(), result);
        return result;
    }

    @Override
    public Map<String, Integer> getHealthArchiveStats(Integer userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        Map<String, Integer> stats = new HashMap<>();

        // 1. 总记录数：我创建的所有健康档案（包括自己和家人的）
        List<HealthArchive> myArchives = getHealthArchiveListByCreatorId(userId);
        int totalCount = myArchives.size();
        stats.put("totalCount", totalCount);

        // 2. 获取我管理的家人列表（包括已注册和未注册的）
        LambdaQueryWrapper<FamilyMember> familyWrapper = new LambdaQueryWrapper<>();
        familyWrapper.eq(FamilyMember::getOwnerUserId, userId);
        List<FamilyMember> familyMembers = familyMemberServiceImpl.list(familyWrapper);

        // 收集所有需要统计的用户ID（包括自己和家人）
        List<Long> userIdsToCount = new java.util.ArrayList<>();
        userIdsToCount.add(userId.longValue());
        for (FamilyMember member : familyMembers) {
            if (member.getRegisteredUserId() != null) {
                userIdsToCount.add(member.getRegisteredUserId().longValue());
            }
        }

        // 3. 统计体检报告：REPORT和MEDICAL_RECORD
        LambdaQueryWrapper<com.android.entity.RawHealthData> reportWrapper = new LambdaQueryWrapper<>();
        reportWrapper.in(com.android.entity.RawHealthData::getUserId, userIdsToCount)
                .and(wrapper -> wrapper.eq(com.android.entity.RawHealthData::getDataType, "REPORT")
                        .or()
                        .eq(com.android.entity.RawHealthData::getDataType, "MEDICAL_RECORD"));
        long reportCount = rawHealthDataService.count(reportWrapper);
        stats.put("reportCount", (int) reportCount);

        // 4. 统计监测数据：LAB、EMOTION、GENETIC、SLEEP
        LambdaQueryWrapper<com.android.entity.RawHealthData> monitorWrapper = new LambdaQueryWrapper<>();
        monitorWrapper.in(com.android.entity.RawHealthData::getUserId, userIdsToCount)
                .and(wrapper -> {
                    wrapper.eq(com.android.entity.RawHealthData::getDataType, "LAB")
                            .or()
                            .eq(com.android.entity.RawHealthData::getDataType, "EMOTION")
                            .or()
                            .eq(com.android.entity.RawHealthData::getDataType, "GENETIC")
                            .or()
                            .eq(com.android.entity.RawHealthData::getDataType, "SLEEP");
                });
        long monitorCount = rawHealthDataService.count(monitorWrapper);
        stats.put("monitorCount", (int) monitorCount);

        log.info("健康档案统计数据，用户ID：{}，总记录数：{}，体检报告：{}，监测数据：{}", 
                userId, totalCount, reportCount, monitorCount);

        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArchive generateLatestHealthArchive(Integer userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        log.info("开始生成用户{}的最新健康档案", userId);

        // 1. 查询用户最新的健康档案
        LambdaQueryWrapper<HealthArchive> archiveWrapper = new LambdaQueryWrapper<>();
        archiveWrapper.eq(HealthArchive::getUserId, userId)
                .orderByDesc(HealthArchive::getArchiveYear)
                .orderByDesc(HealthArchive::getArchiveDate)
                .last("LIMIT 1");

        HealthArchive latestArchive = healthArchiveServiceImpl.getOne(archiveWrapper);

        if (latestArchive == null) {
            throw new RuntimeException("用户没有历史健康档案，无法复制基本信息");
        }

        log.info("找到用户{}的最新健康档案，档案ID：{}，年份：{}", userId, latestArchive.getArchiveId(), latestArchive.getArchiveYear());

        // 2. 获取最新档案的完整信息（包括基本信息）
        Map<String, Object> latestArchiveDetail = getHealthArchiveDetailById(latestArchive.getArchiveId());
        HealthUserInfo latestUserInfo = null;
        if (latestArchiveDetail.get("userInfo") != null) {
            Object userInfoObj = latestArchiveDetail.get("userInfo");
            if (userInfoObj instanceof HealthUserInfo) {
                latestUserInfo = (HealthUserInfo) userInfoObj;
            } else if (userInfoObj instanceof Map) {
                // 如果是Map，转换为实体
                try {
                    latestUserInfo = objectMapper.convertValue(userInfoObj, HealthUserInfo.class);
                } catch (Exception e) {
                    log.warn("转换用户基本信息失败", e);
                }
            }
        }

        // 3. 创建新的健康档案（使用当前年份）
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        HealthArchive newArchive = new HealthArchive();
        newArchive.setUserId(userId);
        newArchive.setUserName(latestArchive.getUserName()); // 复制姓名
        newArchive.setArchiveDate(now);
        newArchive.setArchiveYear(currentYear);
        
        // 生成档案编号：HA + 年月日 + 序号（简化处理，使用时间戳后3位）
        String archiveNo = String.format("HA%d%02d%02d%03d", 
                currentYear, now.getMonthValue(), now.getDayOfMonth(),
                (int)(System.currentTimeMillis() % 1000));
        newArchive.setArchiveNo(archiveNo);
        
        // 生成档案名称：姓名 + 年份 + 年度健康档案
        String archiveName = String.format("%s%d年度健康档案", 
                latestArchive.getUserName() != null ? latestArchive.getUserName() : "用户", 
                currentYear);
        newArchive.setArchiveName(archiveName);
        
        newArchive.setArchiveManagerName(latestArchive.getArchiveManagerName());
        newArchive.setArchiveManagerPhone(latestArchive.getArchiveManagerPhone());

        // 保存新档案主表
        boolean saveResult = healthArchiveServiceImpl.save(newArchive);
        if (!saveResult) {
            throw new RuntimeException("保存新健康档案主表失败");
        }

        log.info("新健康档案主表保存成功，档案ID：{}，档案名称：{}", newArchive.getArchiveId(), archiveName);

        // 4. 复制所有从表数据（创建新记录，不覆盖历史档案）
        // 4.1 复制个人健康标识
        Object tagsObj = latestArchiveDetail.get("healthProfileTags");
        HealthProfileTags latestTags = null;
        if (tagsObj instanceof HealthProfileTags) {
            latestTags = (HealthProfileTags) tagsObj;
        } else if (tagsObj instanceof Map) {
            latestTags = objectMapper.convertValue(tagsObj, HealthProfileTags.class);
        }
        if (latestTags != null) {
            HealthProfileTags newTags = new HealthProfileTags();
            newTags.setArchiveId(newArchive.getArchiveId());
            newTags.setIsChild06(latestTags.getIsChild06());
            newTags.setIsElderly65(latestTags.getIsElderly65());
            newTags.setIsPregnant(latestTags.getIsPregnant());
            newTags.setPregnancyRisk(latestTags.getPregnancyRisk());
            newTags.setWeightStatus(latestTags.getWeightStatus());
            newTags.setBloodType(latestTags.getBloodType());
            newTags.setChronicDisease(latestTags.getChronicDisease()); // JSON字符串
            newTags.setStatutoryInfo(latestTags.getStatutoryInfo()); // JSON字符串
            
            boolean tagsResult = healthProfileTagsServiceImpl.save(newTags);
            if (!tagsResult) {
                throw new RuntimeException("保存新健康档案个人健康标识失败");
            }
            log.info("新健康档案个人健康标识保存成功，档案ID：{}", newArchive.getArchiveId());
        }

        // 4.2 复制用户基本信息
        if (latestUserInfo != null) {
            HealthUserInfo newUserInfo = new HealthUserInfo();
            newUserInfo.setArchiveId(newArchive.getArchiveId());
            // 复制所有基本信息字段
            newUserInfo.setFullName(latestUserInfo.getFullName());
            newUserInfo.setGender(latestUserInfo.getGender());
            newUserInfo.setBirthDate(latestUserInfo.getBirthDate());
            newUserInfo.setIdType(latestUserInfo.getIdType());
            newUserInfo.setIdNumber(latestUserInfo.getIdNumber());
            newUserInfo.setWorkSchool(latestUserInfo.getWorkSchool());
            newUserInfo.setNativePlace(latestUserInfo.getNativePlace());
            newUserInfo.setBirthPlace(latestUserInfo.getBirthPlace());
            newUserInfo.setEthnicity(latestUserInfo.getEthnicity());
            newUserInfo.setFamilyDoctorSigned(latestUserInfo.getFamilyDoctorSigned());
            newUserInfo.setFamilyDoctorName(latestUserInfo.getFamilyDoctorName());
            newUserInfo.setFamilyDoctorPhone(latestUserInfo.getFamilyDoctorPhone());
            newUserInfo.setPersonalPhone(latestUserInfo.getPersonalPhone());
            newUserInfo.setResidenceType(latestUserInfo.getResidenceType());
            newUserInfo.setResidenceAddress(latestUserInfo.getResidenceAddress());
            newUserInfo.setEducationLevel(latestUserInfo.getEducationLevel());
            newUserInfo.setOccupation(latestUserInfo.getOccupation());
            newUserInfo.setMaritalStatus(latestUserInfo.getMaritalStatus());
            newUserInfo.setPaymentMethod(latestUserInfo.getPaymentMethod());
            newUserInfo.setCreateTime(LocalDateTime.now());
            newUserInfo.setUpdateTime(LocalDateTime.now());

            boolean userInfoResult = healthUserInfoServiceImpl.save(newUserInfo);
            if (!userInfoResult) {
                throw new RuntimeException("保存新健康档案用户基本信息失败");
            }
            log.info("新健康档案用户基本信息保存成功，档案ID：{}，信息ID：{}", newArchive.getArchiveId(), newUserInfo.getInfoId());
        }

        // 4.3 复制紧急联系人
        Object emergencyObj = latestArchiveDetail.get("userEmergencyContacts");
        HealthUserEmergencyContacts latestEmergency = null;
        if (emergencyObj instanceof HealthUserEmergencyContacts) {
            latestEmergency = (HealthUserEmergencyContacts) emergencyObj;
        } else if (emergencyObj instanceof Map) {
            latestEmergency = objectMapper.convertValue(emergencyObj, HealthUserEmergencyContacts.class);
        }
        if (latestEmergency != null) {
            HealthUserEmergencyContacts newEmergency = new HealthUserEmergencyContacts();
            newEmergency.setArchiveId(newArchive.getArchiveId());
            newEmergency.setContactName(latestEmergency.getContactName());
            newEmergency.setRelationship(latestEmergency.getRelationship());
            newEmergency.setPhoneNumber(latestEmergency.getPhoneNumber());
            newEmergency.setCreateTime(LocalDateTime.now());
            newEmergency.setUpdateTime(LocalDateTime.now());
            
            boolean emergencyResult = healthUserEmergencyContactsServiceImpl.save(newEmergency);
            if (!emergencyResult) {
                throw new RuntimeException("保存新健康档案紧急联系人失败");
            }
            log.info("新健康档案紧急联系人保存成功，档案ID：{}", newArchive.getArchiveId());
        }

        // 4.4 复制健康服务凭证
        Object certificatesObj = latestArchiveDetail.get("userCertificates");
        HealthUserCertificates latestCertificates = null;
        if (certificatesObj instanceof HealthUserCertificates) {
            latestCertificates = (HealthUserCertificates) certificatesObj;
        } else if (certificatesObj instanceof Map) {
            latestCertificates = objectMapper.convertValue(certificatesObj, HealthUserCertificates.class);
        }
        if (latestCertificates != null) {
            HealthUserCertificates newCertificates = new HealthUserCertificates();
            newCertificates.setArchiveId(newArchive.getArchiveId());
            newCertificates.setCertificateType(latestCertificates.getCertificateType());
            newCertificates.setCertificateIssuer(latestCertificates.getCertificateIssuer());
            newCertificates.setIssueDate(latestCertificates.getIssueDate());
            newCertificates.setExpiryDate(latestCertificates.getExpiryDate());
            newCertificates.setCreateTime(LocalDateTime.now());
            newCertificates.setUpdateTime(LocalDateTime.now());
            
            boolean certificatesResult = healthUserCertificatesServiceImpl.save(newCertificates);
            if (!certificatesResult) {
                throw new RuntimeException("保存新健康档案健康服务凭证失败");
            }
            log.info("新健康档案健康服务凭证保存成功，档案ID：{}", newArchive.getArchiveId());
        }

        // 4.5 复制过敏史
        Object allergyObj = latestArchiveDetail.get("allergyHistory");
        HealthAllergyHistory latestAllergy = null;
        if (allergyObj instanceof HealthAllergyHistory) {
            latestAllergy = (HealthAllergyHistory) allergyObj;
        } else if (allergyObj instanceof Map) {
            latestAllergy = objectMapper.convertValue(allergyObj, HealthAllergyHistory.class);
        }
        if (latestAllergy != null) {
            HealthAllergyHistory newAllergy = new HealthAllergyHistory();
            newAllergy.setArchiveId(newArchive.getArchiveId());
            newAllergy.setAllergyType(latestAllergy.getAllergyType());
            newAllergy.setDrugAllergyDetails(latestAllergy.getDrugAllergyDetails());
            newAllergy.setFoodAllergyDetails(latestAllergy.getFoodAllergyDetails());
            newAllergy.setOtherAllergyDetails(latestAllergy.getOtherAllergyDetails());
            newAllergy.setCreateTime(LocalDateTime.now());
            newAllergy.setUpdateTime(LocalDateTime.now());
            
            boolean allergyResult = healthAllergyHistoryServiceImpl.save(newAllergy);
            if (!allergyResult) {
                throw new RuntimeException("保存新健康档案过敏史失败");
            }
            log.info("新健康档案过敏史保存成功，档案ID：{}", newArchive.getArchiveId());
        }

        // 4.6 复制暴露史
        Object exposureObj = latestArchiveDetail.get("exposureHistory");
        HealthExposureHistory latestExposure = null;
        if (exposureObj instanceof HealthExposureHistory) {
            latestExposure = (HealthExposureHistory) exposureObj;
        } else if (exposureObj instanceof Map) {
            latestExposure = objectMapper.convertValue(exposureObj, HealthExposureHistory.class);
        }
        if (latestExposure != null) {
            HealthExposureHistory newExposure = new HealthExposureHistory();
            newExposure.setArchiveId(newArchive.getArchiveId());
            newExposure.setExposureType(latestExposure.getExposureType());
            newExposure.setExposureDetails(latestExposure.getExposureDetails());
            newExposure.setCreateTime(LocalDateTime.now());
            newExposure.setUpdateTime(LocalDateTime.now());
            
            boolean exposureResult = healthExposureHistoryServiceImpl.save(newExposure);
            if (!exposureResult) {
                throw new RuntimeException("保存新健康档案暴露史失败");
            }
            log.info("新健康档案暴露史保存成功，档案ID：{}", newArchive.getArchiveId());
        }

        // 4.7 复制既往史（列表）
        Object diseaseHistoryObj = latestArchiveDetail.get("diseaseHistory");
        List<HealthDiseaseHistory> latestDiseaseHistory = null;
        if (diseaseHistoryObj instanceof List) {
            List<?> rawList = (List<?>) diseaseHistoryObj;
            if (!rawList.isEmpty() && rawList.get(0) instanceof HealthDiseaseHistory) {
                @SuppressWarnings("unchecked")
                List<HealthDiseaseHistory> tempList = (List<HealthDiseaseHistory>) rawList;
                latestDiseaseHistory = tempList;
            } else if (!rawList.isEmpty() && rawList.get(0) instanceof Map) {
                latestDiseaseHistory = new java.util.ArrayList<>();
                for (Object item : rawList) {
                    latestDiseaseHistory.add(objectMapper.convertValue(item, HealthDiseaseHistory.class));
                }
            }
        }
        if (latestDiseaseHistory != null && !latestDiseaseHistory.isEmpty()) {
            for (HealthDiseaseHistory latestDisease : latestDiseaseHistory) {
                HealthDiseaseHistory newDisease = new HealthDiseaseHistory();
                newDisease.setArchiveId(newArchive.getArchiveId());
                newDisease.setDiseaseName(latestDisease.getDiseaseName());
                newDisease.setOnsetDate(latestDisease.getOnsetDate());
                newDisease.setCreateTime(LocalDateTime.now());
                newDisease.setUpdateTime(LocalDateTime.now());
                healthDiseaseHistoryServiceImpl.save(newDisease);
            }
            log.info("新健康档案既往史保存成功，档案ID：{}，共{}条", newArchive.getArchiveId(), latestDiseaseHistory.size());
        }

        // 4.8 复制预防接种史（列表）
        Object vaccinationHistoryObj = latestArchiveDetail.get("vaccinationHistory");
        List<HealthVaccinationHistory> latestVaccinationHistory = null;
        if (vaccinationHistoryObj instanceof List) {
            List<?> rawList = (List<?>) vaccinationHistoryObj;
            if (!rawList.isEmpty() && rawList.get(0) instanceof HealthVaccinationHistory) {
                @SuppressWarnings("unchecked")
                List<HealthVaccinationHistory> tempList = (List<HealthVaccinationHistory>) rawList;
                latestVaccinationHistory = tempList;
            } else if (!rawList.isEmpty() && rawList.get(0) instanceof Map) {
                latestVaccinationHistory = new java.util.ArrayList<>();
                for (Object item : rawList) {
                    latestVaccinationHistory.add(objectMapper.convertValue(item, HealthVaccinationHistory.class));
                }
            }
        }
        if (latestVaccinationHistory != null && !latestVaccinationHistory.isEmpty()) {
            for (HealthVaccinationHistory latestVaccination : latestVaccinationHistory) {
                HealthVaccinationHistory newVaccination = new HealthVaccinationHistory();
                newVaccination.setArchiveId(newArchive.getArchiveId());
                newVaccination.setVaccineName(latestVaccination.getVaccineName());
                newVaccination.setVaccinationDate(latestVaccination.getVaccinationDate());
                newVaccination.setCreateTime(LocalDateTime.now());
                newVaccination.setUpdateTime(LocalDateTime.now());
                healthVaccinationHistoryServiceImpl.save(newVaccination);
            }
            log.info("新健康档案预防接种史保存成功，档案ID：{}，共{}条", newArchive.getArchiveId(), latestVaccinationHistory.size());
        }

        // 4.9 复制家族史（列表）
        Object familyHistoryObj = latestArchiveDetail.get("familyHistory");
        List<HealthFamilyHistory> latestFamilyHistory = null;
        if (familyHistoryObj instanceof List) {
            List<?> rawList = (List<?>) familyHistoryObj;
            if (!rawList.isEmpty() && rawList.get(0) instanceof HealthFamilyHistory) {
                @SuppressWarnings("unchecked")
                List<HealthFamilyHistory> tempList = (List<HealthFamilyHistory>) (List<?>) rawList;
                latestFamilyHistory = tempList;
            } else if (!rawList.isEmpty() && rawList.get(0) instanceof Map) {
                latestFamilyHistory = new java.util.ArrayList<>();
                for (Object item : rawList) {
                    latestFamilyHistory.add(objectMapper.convertValue(item, HealthFamilyHistory.class));
                }
            }
        }
        if (latestFamilyHistory != null && !latestFamilyHistory.isEmpty()) {
            for (HealthFamilyHistory latestFamily : latestFamilyHistory) {
                HealthFamilyHistory newFamily = new HealthFamilyHistory();
                newFamily.setArchiveId(newArchive.getArchiveId());
                newFamily.setRelativeType(latestFamily.getRelativeType());
                newFamily.setDiseases(latestFamily.getDiseases()); // JSON字符串
                newFamily.setCreateTime(LocalDateTime.now());
                newFamily.setUpdateTime(LocalDateTime.now());
                healthFamilyHistoryServiceImpl.save(newFamily);
            }
            log.info("新健康档案家族史保存成功，档案ID：{}，共{}条", newArchive.getArchiveId(), latestFamilyHistory.size());
        }

        // 4.10 复制遗传病史
        Object geneticObj = latestArchiveDetail.get("geneticHistory");
        HealthGeneticHistory latestGenetic = null;
        if (geneticObj instanceof HealthGeneticHistory) {
            latestGenetic = (HealthGeneticHistory) geneticObj;
        } else if (geneticObj instanceof Map) {
            latestGenetic = objectMapper.convertValue(geneticObj, HealthGeneticHistory.class);
        }
        if (latestGenetic != null) {
            HealthGeneticHistory newGenetic = new HealthGeneticHistory();
            newGenetic.setArchiveId(newArchive.getArchiveId());
            newGenetic.setHasGeneticDisease(latestGenetic.getHasGeneticDisease());
            newGenetic.setDiseaseName(latestGenetic.getDiseaseName());
            newGenetic.setCreateTime(LocalDateTime.now());
            newGenetic.setUpdateTime(LocalDateTime.now());
            
            boolean geneticResult = healthGeneticHistoryServiceImpl.save(newGenetic);
            if (!geneticResult) {
                throw new RuntimeException("保存新健康档案遗传病史失败");
            }
            log.info("新健康档案遗传病史保存成功，档案ID：{}", newArchive.getArchiveId());
        }

        // 4.11 复制残疾情况
        Object disabilityObj = latestArchiveDetail.get("disabilityHistory");
        HealthDisabilityStatus latestDisability = null;
        if (disabilityObj instanceof HealthDisabilityStatus) {
            latestDisability = (HealthDisabilityStatus) disabilityObj;
        } else if (disabilityObj instanceof Map) {
            latestDisability = objectMapper.convertValue(disabilityObj, HealthDisabilityStatus.class);
        }
        if (latestDisability != null) {
            HealthDisabilityStatus newDisability = new HealthDisabilityStatus();
            newDisability.setArchiveId(newArchive.getArchiveId());
            newDisability.setDisabilityTypes(latestDisability.getDisabilityTypes()); // JSON字符串
            newDisability.setCreateTime(LocalDateTime.now());
            newDisability.setUpdateTime(LocalDateTime.now());
            
            boolean disabilityResult = healthDisabilityStatusServiceImpl.save(newDisability);
            if (!disabilityResult) {
                throw new RuntimeException("保存新健康档案残疾情况失败");
            }
            log.info("新健康档案残疾情况保存成功，档案ID：{}", newArchive.getArchiveId());
        }

        log.info("健康档案生成完成，档案ID：{}，用户ID：{}，所有从表数据已复制", newArchive.getArchiveId(), userId);

        // 5. 异步调用智能体接口生成剩余数据（包括总结和建议）
        // 异步通知智能体（通过代理调用，确保 @Async 生效，不阻塞返回；前端立即看到“健康档案生成成功”）
        self.callAiAgentToGenerateRemainingData(userId, newArchive.getArchiveId());

        return newArchive;
    }

    /**
     * 异步调用智能体接口生成健康档案的剩余数据
     * TODO: 智能体开发程序员 - 后续更新 webhook/archive-created 地址、请求体、响应处理
     * 包括：生活方式状态、系统疾病筛查、心理评估、社会关系评估、体格检查、健康总结和建议
     * 
     * 调用流程：
     * 1. 通过webhook通知智能体系统，告知有新健康档案创建
     * 2. 智能体接收到通知后，会：
     *    - 调用 /health-ai-analysis/data/user/{userId} 获取用户完整数据（原始数据+健康档案）
     *    - 分析原始数据和最新健康档案
     *    - 生成剩余的健康档案数据（生活方式状态、系统疾病筛查、心理评估、社会关系评估、体格检查）
     *    - 调用 /health-archive-process/update/{archiveId} 更新健康档案的剩余数据
     *    - 调用 /health-ai-analysis/submit/user/{userId} 提交分析结果（包括健康总结和建议）
     * 
     * @param userId 用户ID
     * @param archiveId 新创建的健康档案ID
     */
    @Async
    public void callAiAgentToGenerateRemainingData(Integer userId, Integer archiveId) {
        if (medicalAiBaseUrl == null || medicalAiBaseUrl.trim().isEmpty()) {
            log.debug("未配置 Medical AI 基础URL，跳过智能体接口调用，用户ID：{}，档案ID：{}", userId, archiveId);
            return;
        }

        if (restTemplate == null) {
            log.warn("RestTemplate 未注入，无法调用智能体接口，用户ID：{}，档案ID：{}", userId, archiveId);
            return;
        }

        try {
            // 构建请求URL
            // 通过webhook通知智能体系统，告知有新健康档案创建
            // 智能体系统会主动调用我们的接口获取数据并生成剩余内容
            String baseUrl = medicalAiBaseUrl.endsWith("/") 
                    ? medicalAiBaseUrl.substring(0, medicalAiBaseUrl.length() - 1) 
                    : medicalAiBaseUrl;
            
            // Webhook接口：通知智能体系统有新健康档案创建
            // 智能体系统应该提供此接口，用于接收新档案创建的通知
            String webhookUrl = baseUrl + "/webhook/archive-created";
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId);
            requestBody.put("archiveId", archiveId);
            requestBody.put("event", "archive_created");
            requestBody.put("timestamp", LocalDateTime.now().toString());

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            log.info("通知智能体系统生成健康档案剩余数据，用户ID：{}，档案ID：{}，URL：{}", 
                    userId, archiveId, webhookUrl);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    requestEntity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            // 处理响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Integer code = (Integer) responseBody.get("code");
                String message = (String) responseBody.get("message");
                
                if (code != null && code == 200) {
                    log.info("智能体系统通知成功，用户ID：{}，档案ID：{}，响应消息：{}", userId, archiveId, message);
                } else {
                    log.warn("智能体系统通知失败，用户ID：{}，档案ID：{}，响应码：{}，响应消息：{}", 
                            userId, archiveId, code, message);
                }
            } else {
                log.warn("智能体系统通知返回非成功状态码，用户ID：{}，档案ID：{}，HTTP状态码：{}", 
                        userId, archiveId, response.getStatusCode());
            }

        } catch (Exception e) {
            // 智能体接口调用失败不影响健康档案的创建
            // 智能体可以在后台定时任务中扫描新创建的档案并生成剩余数据
            log.error("通知智能体系统生成健康档案剩余数据失败，用户ID：{}，档案ID：{}，错误：{}", 
                    userId, archiveId, e.getMessage());
        }
    }
    /**
     * TODO 功能
     * 首页：个性化推荐
     * 服务：在线问诊-》AI问诊 ai医生 医生分身 预约挂号-》医生问诊 医生列表
     *       情绪检测-》情绪管理  心理评估-》情绪检测  心理咨询  快乐活动
     *       社区活动-》快乐活动 社区活动+旅游疗愈
     *       就医服务增加会诊 转诊 陪诊  睡眠管理 基因检测
     *       健康服务增加上门服务 营养管理
     *
     *       角色：系统管理员 医生 客户 健康管理师
     *       商城：跳转乐语
     *       管理后台开发
     *       尽快上线 医健家小程序
     */
}

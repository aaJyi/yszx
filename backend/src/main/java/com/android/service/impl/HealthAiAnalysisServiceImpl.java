package com.android.service.impl;

import com.android.dto.AiAnalysisDataResponse;
import com.android.dto.AiAnalysisRequest;
import com.android.dto.AiRecommendationData;
import com.android.entity.HealthAiAnalysis;
import com.android.entity.HealthArchive;
import com.android.entity.HealthLifestyleStatus;
import com.android.entity.HealthPhysicalExamination;
import com.android.entity.HealthPsychologicalAssessment;
import com.android.entity.HealthRecommendation;
import com.android.entity.HealthRecommendationRule;
import com.android.entity.HealthSocialRelationshipAssessment;
import com.android.entity.HealthSystemDiseaseScreening;
import com.android.mapper.HealthAiAnalysisMapper;
import com.android.mapper.HealthLifestyleStatusMapper;
import com.android.mapper.HealthPhysicalExaminationMapper;
import com.android.mapper.HealthPsychologicalAssessmentMapper;
import com.android.mapper.HealthRecommendationMapper;
import com.android.mapper.HealthRecommendationRuleMapper;
import com.android.mapper.HealthSocialRelationshipAssessmentMapper;
import com.android.mapper.HealthSystemDiseaseScreeningMapper;
import com.android.mapper.UserClubProfileMapper;
import com.android.mapper.UserInferredDiseaseMapper;
import com.android.entity.UserClubProfile;
import com.android.entity.UserInferredDisease;
import com.android.service.IHealthAiAnalysisService;
import com.android.service.IHealthArchiveProcessService;
import com.android.service.IRawHealthDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * AI健康分析结果表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthAiAnalysisServiceImpl extends ServiceImpl<HealthAiAnalysisMapper, HealthAiAnalysis> implements IHealthAiAnalysisService {

    private final HealthRecommendationMapper recommendationMapper;
    private final HealthRecommendationRuleMapper ruleMapper;
    private final IHealthArchiveProcessService healthArchiveProcessService;
    private final IRawHealthDataService rawHealthDataService;
    private final HealthLifestyleStatusMapper lifestyleStatusMapper;
    private final HealthSystemDiseaseScreeningMapper systemScreeningMapper;
    private final HealthPsychologicalAssessmentMapper psychologicalAssessmentMapper;
    private final HealthSocialRelationshipAssessmentMapper socialAssessmentMapper;
    private final HealthPhysicalExaminationMapper physicalExaminationMapper;
    private final UserInferredDiseaseMapper userInferredDiseaseMapper;
    private final UserClubProfileMapper userClubProfileMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthAiAnalysis saveAnalysisAndGenerateRecommendations(AiAnalysisRequest request) {
        log.info("开始保存AI分析结果，用户ID：{}，档案ID：{}", request.getUserId(), request.getArchiveId());

        try {
            // 1. 构建分析结果JSON
            Map<String, Object> analysisDetailJson = buildAnalysisDetailJson(request);
            String analysisDetailStr = objectMapper.writeValueAsString(analysisDetailJson);

            // 2. 构建分析元信息JSON
            Map<String, Object> analysisMetaJson = buildAnalysisMetaJson(request);
            String analysisMetaStr = objectMapper.writeValueAsString(analysisMetaJson);

            // 3. 保存AI分析结果到 health_ai_analysis 表
            // userId 从路径参数获取（Controller层已设置到request中）
            HealthAiAnalysis analysis = new HealthAiAnalysis();
            analysis.setUserId(request.getUserId()); // 使用路径参数中的userId
            analysis.setArchiveId(request.getArchiveId());
            analysis.setSourceRawId(request.getSourceRawId());
            analysis.setModelName(request.getModelName());
            analysis.setModelVersion(request.getModelVersion());
            analysis.setOverallHealthScore(request.getAnalysisResult().getOverallAssessment().getOverallHealthScore());
            analysis.setRiskLevel(request.getAnalysisResult().getOverallAssessment().getRiskLevel());
            analysis.setAnalysisSummary(request.getAnalysisResult().getOverallAssessment().getSummary());
            analysis.setAnalysisDetail(analysisDetailStr);
            analysis.setAnalysisMeta(analysisMetaStr);
            analysis.setCreateTime(LocalDateTime.now());
            analysis.setUpdateTime(LocalDateTime.now());

            boolean saveResult = this.save(analysis);
            if (!saveResult) {
                throw new RuntimeException("保存AI分析结果失败");
            }

            log.info("AI分析结果保存成功，分析ID：{}，用户ID：{}，档案ID：{}", 
                    analysis.getAnalysisId(), analysis.getUserId(), analysis.getArchiveId());

            // 4. 保存扩展数据到相关表（生活方式状态、系统症状筛查、心理评估、社会背景、体格检查）
            saveExtendedData(analysis.getArchiveId(), request);

            // 4.1 保存社团分析画像 + 推断疾病（用于社团划分与病友推荐）
            saveCommunityProfileAndInferredDiseases(analysis, request);

            // TODO: 智能体开发程序员 - 后续更新智能体建议接口、extendedData 格式
            // 5. 生成健康建议（优先使用智能体提交的建议，失败时使用规则引擎）
            generateRecommendations(analysis, request);

            return analysis;

        } catch (Exception e) {
            log.error("保存AI分析结果失败", e);
            throw new RuntimeException("保存AI分析结果失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将智能体分析结果抽取为社团画像，并同步刷新 user_inferred_disease。
     */
    private void saveCommunityProfileAndInferredDiseases(HealthAiAnalysis analysis, AiAnalysisRequest request) {
        try {
            Map<String, Object> meta = request.getAnalysisResult() != null ? request.getAnalysisResult().getAnalysisMeta() : null;
            Map<String, Object> communityFeatures = null;
            if (meta != null && meta.get("communityFeatures") instanceof Map) {
                communityFeatures = safeCastToMap(meta.get("communityFeatures"));
            }
            if (communityFeatures == null || communityFeatures.isEmpty()) {
                communityFeatures = buildFallbackCommunityFeatures(request);
            }

            List<Map<String, Object>> inferredDiseases = extractInferredDiseases(communityFeatures, request);
            refreshInferredDiseases(analysis, inferredDiseases);

            UserClubProfile profile = upsertClubProfile(analysis, communityFeatures);
            log.info("社团画像写入成功，userId={}，profileId={}", analysis.getUserId(), profile.getId());
        } catch (Exception e) {
            log.warn("写入社团画像/推断疾病失败，不影响主流程: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildFallbackCommunityFeatures(AiAnalysisRequest request) {
        Map<String, Object> root = new HashMap<>();

        // 疾病与风险层
        Map<String, Object> diseaseRisk = new HashMap<>();
        diseaseRisk.put("confirmedDiseases", new ArrayList<>());
        diseaseRisk.put("riskLevel", request.getAnalysisResult() != null && request.getAnalysisResult().getOverallAssessment() != null
                ? request.getAnalysisResult().getOverallAssessment().getRiskLevel() : "中");
        diseaseRisk.put("comorbidityPattern", new ArrayList<>());
        diseaseRisk.put("inferredDiseases", extractInferredDiseases(new HashMap<>(), request));
        root.put("diseaseRisk", diseaseRisk);

        // 行为层（从扩展数据提取）
        Map<String, Object> behavior = new HashMap<>();
        Map<String, Object> ext = request.getAnalysisResult() != null ? request.getAnalysisResult().getExtendedData() : null;
        Map<String, Object> lifestyle = ext != null ? safeCastToMap(ext.get("lifestyleStatus")) : null;
        if (lifestyle != null) {
            behavior.put("exerciseLevel", lifestyle.get("exerciseFrequency"));
            behavior.put("sleepQuality", lifestyle.get("sleepStatus"));
            behavior.put("smokingDrinkingLevel", String.valueOf(lifestyle.get("smokingStatus")) + "/" + String.valueOf(lifestyle.get("drinkingStatus")));
            behavior.put("dietPattern", lifestyle.get("dietType"));
        }
        behavior.putIfAbsent("adherenceScore", 0.5d);
        behavior.putIfAbsent("followupRegularity", 0.5d);
        root.put("behavior", behavior);

        // 指标趋势层
        Map<String, Object> trend = new HashMap<>();
        trend.put("trendStatus", "stable");
        trend.put("keyMetricCount", request.getAnalysisResult() != null && request.getAnalysisResult().getAbnormalIndicators() != null
                ? request.getAnalysisResult().getAbnormalIndicators().size() : 0);
        root.put("metricTrend", trend);

        // 阶段需求层
        Map<String, Object> stage = new HashMap<>();
        stage.put("currentGoals", request.getAnalysisResult() != null && request.getAnalysisResult().getOverallAssessment() != null
                ? request.getAnalysisResult().getOverallAssessment().getKeyRisks() : new ArrayList<>());
        stage.put("careStage", "稳定管理期");
        root.put("stageNeed", stage);

        return root;
    }

    private List<Map<String, Object>> extractInferredDiseases(Map<String, Object> communityFeatures, AiAnalysisRequest request) {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        // 1) 优先读取 communityFeatures.diseaseRisk.inferredDiseases
        Map<String, Object> diseaseRisk = safeCastToMap(communityFeatures.get("diseaseRisk"));
        if (diseaseRisk != null && diseaseRisk.get("inferredDiseases") instanceof List) {
            List<?> arr = (List<?>) diseaseRisk.get("inferredDiseases");
            for (Object it : arr) {
                if (!(it instanceof Map)) continue;
                Map<String, Object> row = safeCastToMap(it);
                String name = row != null && row.get("name") != null ? String.valueOf(row.get("name")).trim() : null;
                if (name == null || name.isBlank() || seen.contains(name)) continue;
                Double conf = row.get("confidence") instanceof Number ? ((Number) row.get("confidence")).doubleValue() : 0.6d;
                Map<String, Object> m = new HashMap<>();
                m.put("name", name);
                m.put("confidence", conf);
                out.add(m);
                seen.add(name);
                if (out.size() >= 5) return out;
            }
        }

        // 2) 兜底：从异常指标名称推导
        if (request.getAnalysisResult() != null && request.getAnalysisResult().getAbnormalIndicators() != null) {
            request.getAnalysisResult().getAbnormalIndicators().forEach(ind -> {
                if (out.size() >= 5) return;
                String name = ind.getIndicatorName() != null ? ind.getIndicatorName().trim() : null;
                if (name == null || name.isBlank() || seen.contains(name)) return;
                Map<String, Object> m = new HashMap<>();
                m.put("name", name);
                m.put("confidence", 0.55d);
                out.add(m);
                seen.add(name);
            });
        }
        return out;
    }

    private void refreshInferredDiseases(HealthAiAnalysis analysis, List<Map<String, Object>> inferredDiseases) {
        userInferredDiseaseMapper.delete(new LambdaQueryWrapper<UserInferredDisease>()
                .eq(UserInferredDisease::getUserId, analysis.getUserId())
                .eq(UserInferredDisease::getSourceType, "agent"));

        for (Map<String, Object> d : inferredDiseases) {
            String name = d.get("name") != null ? String.valueOf(d.get("name")).trim() : null;
            if (name == null || name.isBlank()) continue;
            BigDecimal conf = BigDecimal.valueOf(d.get("confidence") instanceof Number
                    ? ((Number) d.get("confidence")).doubleValue() : 0.6d);
            UserInferredDisease row = new UserInferredDisease();
            row.setUserId(analysis.getUserId());
            row.setDiseaseName(name);
            row.setConfidence(conf);
            row.setSourceType("agent");
            row.setAnalysisId(analysis.getAnalysisId());
            row.setCreatedAt(LocalDateTime.now());
            userInferredDiseaseMapper.insert(row);
        }
    }

    private UserClubProfile upsertClubProfile(HealthAiAnalysis analysis, Map<String, Object> features) throws Exception {
        // 只保留最小可用向量，社团推荐中再做组合评分
        List<Double> vector = buildProfileVector(features);
        String featureJson = objectMapper.writeValueAsString(features);
        String vectorJson = objectMapper.writeValueAsString(vector);

        UserClubProfile existing = userClubProfileMapper.selectOne(
                new LambdaQueryWrapper<UserClubProfile>()
                        .eq(UserClubProfile::getUserId, analysis.getUserId())
                        .last("LIMIT 1"));
        if (existing == null) {
            UserClubProfile row = new UserClubProfile();
            row.setUserId(analysis.getUserId());
            row.setArchiveId(analysis.getArchiveId());
            row.setAnalysisId(analysis.getAnalysisId());
            row.setProfileVersion("v1");
            row.setFeatureJson(featureJson);
            row.setVectorJson(vectorJson);
            row.setUpdatedAt(LocalDateTime.now());
            userClubProfileMapper.insert(row);
            return row;
        }
        existing.setArchiveId(analysis.getArchiveId());
        existing.setAnalysisId(analysis.getAnalysisId());
        existing.setProfileVersion("v1");
        existing.setFeatureJson(featureJson);
        existing.setVectorJson(vectorJson);
        existing.setUpdatedAt(LocalDateTime.now());
        userClubProfileMapper.updateById(existing);
        return existing;
    }

    private List<Double> buildProfileVector(Map<String, Object> features) {
        // disease(40) + behavior(25) + trend(20) + stage(15)
        double disease = 0.5d;
        Map<String, Object> diseaseRisk = safeCastToMap(features.get("diseaseRisk"));
        if (diseaseRisk != null) {
            disease = switch (String.valueOf(diseaseRisk.getOrDefault("riskLevel", "中"))) {
                case "低" -> 0.25d;
                case "高" -> 0.75d;
                case "极高" -> 1.0d;
                default -> 0.5d;
            };
        }
        Map<String, Object> behavior = safeCastToMap(features.get("behavior"));
        double behaviorScore = behavior != null && behavior.get("adherenceScore") instanceof Number
                ? ((Number) behavior.get("adherenceScore")).doubleValue() : 0.5d;
        Map<String, Object> trend = safeCastToMap(features.get("metricTrend"));
        double trendScore = 0.5d;
        if (trend != null && trend.get("trendStatus") != null) {
            String status = String.valueOf(trend.get("trendStatus"));
            if ("improving".equalsIgnoreCase(status)) trendScore = 0.8d;
            else if ("worsening".equalsIgnoreCase(status)) trendScore = 0.2d;
        }
        Map<String, Object> stage = safeCastToMap(features.get("stageNeed"));
        double stageScore = stage != null && stage.get("currentGoals") instanceof List
                ? Math.min(((List<?>) stage.get("currentGoals")).size(), 5) / 5.0d : 0.4d;

        List<Double> vec = new ArrayList<>();
        vec.add(0.40d * disease);
        vec.add(0.25d * behaviorScore);
        vec.add(0.20d * trendScore);
        vec.add(0.15d * stageScore);
        return vec;
    }

    /**
     * 构建分析详情JSON
     */
    private Map<String, Object> buildAnalysisDetailJson(AiAnalysisRequest request) {
        Map<String, Object> detail = new HashMap<>();
        
        // 转换维度分析
        List<Map<String, Object>> dimensionList = new ArrayList<>();
        if (request.getAnalysisResult().getDimensionAnalysis() != null) {
            for (var dimension : request.getAnalysisResult().getDimensionAnalysis()) {
                Map<String, Object> dimMap = new HashMap<>();
                dimMap.put("dimensionCode", dimension.getDimensionCode());
                dimMap.put("dimensionName", dimension.getDimensionName());
                dimMap.put("riskLevel", dimension.getRiskLevel());
                dimMap.put("evidence", dimension.getEvidence());
                dimMap.put("interpretation", dimension.getInterpretation());
                dimMap.put("confidence", dimension.getConfidence());
                dimensionList.add(dimMap);
            }
        }
        
        detail.put("dimensionAnalysis", dimensionList);
        detail.put("overallAssessment", request.getAnalysisResult().getOverallAssessment());
        detail.put("abnormalIndicators", request.getAnalysisResult().getAbnormalIndicators());
        detail.put("modelConfidence", request.getAnalysisResult().getModelConfidence());
        
        return detail;
    }

    /**
     * 构建分析元信息JSON
     */
    private Map<String, Object> buildAnalysisMetaJson(AiAnalysisRequest request) {
        Map<String, Object> meta = new HashMap<>();
        if (request.getAnalysisResult().getAnalysisMeta() != null) {
            meta.putAll(request.getAnalysisResult().getAnalysisMeta());
        } else {
            meta.put("sourceType", "PHYSICAL_REPORT");
            meta.put("analysisTime", LocalDateTime.now().toString());
        }
        return meta;
    }

    /**
     * 生成健康建议
     * 优先使用智能体提交的建议，如果智能体没有提交建议或提交失败，则使用规则引擎生成
     */
    private void generateRecommendations(HealthAiAnalysis analysis, AiAnalysisRequest request) {
        log.info("开始生成健康建议，分析ID：{}", analysis.getAnalysisId());

        // 优先尝试保存智能体提交的建议
        boolean aiRecommendationsSaved = false;
        if (request.getRecommendations() != null && !request.getRecommendations().isEmpty()) {
            try {
                aiRecommendationsSaved = saveAiRecommendations(analysis, request.getRecommendations());
                if (aiRecommendationsSaved) {
                    log.info("成功保存智能体提交的{}条建议", request.getRecommendations().size());
                    return; // 智能体建议保存成功，直接返回
                }
            } catch (Exception e) {
                log.warn("保存智能体提交的建议失败，将使用规则引擎生成：{}", e.getMessage());
            }
        }

        // 如果智能体没有提交建议或保存失败，使用规则引擎生成
        log.info("使用规则引擎生成健康建议");
        generateRecommendationsByRule(analysis, request);
    }

    /**
     * 保存智能体提交的建议
     */
    private boolean saveAiRecommendations(HealthAiAnalysis analysis, List<AiRecommendationData> aiRecommendations) {
        List<HealthRecommendation> recommendations = new ArrayList<>();

        for (AiRecommendationData aiRec : aiRecommendations) {
            // 验证必填字段
            if (aiRec.getTitle() == null || aiRec.getTitle().trim().isEmpty()) {
                log.warn("建议标题为空，跳过该建议");
                continue;
            }
            if (aiRec.getContent() == null || aiRec.getContent().trim().isEmpty()) {
                log.warn("建议内容为空，跳过该建议");
                continue;
            }
            if (aiRec.getRecommendationType() == null || aiRec.getRecommendationType().trim().isEmpty()) {
                log.warn("建议类型为空，跳过该建议");
                continue;
            }

            // 保存健康建议到 health_recommendation 表
            // userId 使用 analysis.getUserId()，该值来自路径参数
            HealthRecommendation recommendation = new HealthRecommendation();
            recommendation.setAnalysisId(analysis.getAnalysisId());
            recommendation.setUserId(analysis.getUserId()); // 使用路径参数中的userId
            recommendation.setRecommendationType(aiRec.getRecommendationType());
            recommendation.setTitle(aiRec.getTitle());
            recommendation.setContent(aiRec.getContent());
            recommendation.setPriority(aiRec.getPriority() != null ? aiRec.getPriority() : "中");
            recommendation.setIsActive(true);
            recommendation.setValidFrom(LocalDate.now());
            recommendation.setDimensionCode(aiRec.getDimensionCode());
            recommendation.setCreateTime(LocalDateTime.now());
            recommendation.setUpdateTime(LocalDateTime.now());

            recommendations.add(recommendation);
        }

        // 批量保存建议到 health_recommendation 表
        if (!recommendations.isEmpty()) {
            for (HealthRecommendation rec : recommendations) {
                recommendationMapper.insert(rec);
                log.debug("保存健康建议成功，建议ID：{}，用户ID：{}，分析ID：{}", 
                        rec.getRecommendationId(), rec.getUserId(), rec.getAnalysisId());
            }
            log.info("成功保存{}条智能体生成的健康建议，用户ID：{}", 
                    recommendations.size(), analysis.getUserId());
            return true;
        }

        return false;
    }

    /**
     * 根据规则引擎生成建议（后备方案）
     */
    private void generateRecommendationsByRule(HealthAiAnalysis analysis, AiAnalysisRequest request) {
        if (request.getAnalysisResult().getDimensionAnalysis() == null || 
            request.getAnalysisResult().getDimensionAnalysis().isEmpty()) {
            log.warn("没有维度分析数据，跳过规则引擎建议生成");
            return;
        }

        List<HealthRecommendation> recommendations = new ArrayList<>();

        // 遍历每个维度分析，匹配规则
        for (var dimension : request.getAnalysisResult().getDimensionAnalysis()) {
            String dimensionCode = dimension.getDimensionCode();
            String riskLevel = dimension.getRiskLevel();

            // 查询匹配的规则
            LambdaQueryWrapper<HealthRecommendationRule> ruleWrapper = new LambdaQueryWrapper<>();
            ruleWrapper.eq(HealthRecommendationRule::getIsActive, true)
                    .and(wrapper -> wrapper
                            .eq(HealthRecommendationRule::getDimensionCode, dimensionCode)
                            .eq(HealthRecommendationRule::getRiskLevel, riskLevel)
                            .or()
                            .isNull(HealthRecommendationRule::getDimensionCode)
                            .isNull(HealthRecommendationRule::getRiskLevel))
                    .orderByAsc(HealthRecommendationRule::getOrderNum);

            List<HealthRecommendationRule> rules = ruleMapper.selectList(ruleWrapper);

            // 为每个匹配的规则生成建议
            for (HealthRecommendationRule rule : rules) {
                // 如果规则有维度代码和风险等级，需要精确匹配
                if (rule.getDimensionCode() != null && rule.getRiskLevel() != null) {
                    if (!dimensionCode.equals(rule.getDimensionCode()) || !riskLevel.equals(rule.getRiskLevel())) {
                        continue; // 不匹配，跳过
                    }
                }

                // 保存健康建议到 health_recommendation 表（规则引擎生成）
                // userId 使用 analysis.getUserId()，该值来自路径参数
                HealthRecommendation recommendation = new HealthRecommendation();
                recommendation.setAnalysisId(analysis.getAnalysisId());
                recommendation.setUserId(analysis.getUserId()); // 使用路径参数中的userId
                recommendation.setRecommendationType(rule.getRecommendationType());
                recommendation.setTitle(rule.getTitleTemplate());
                recommendation.setContent(rule.getContentTemplate());
                recommendation.setPriority(rule.getPriority());
                recommendation.setIsActive(true);
                recommendation.setValidFrom(LocalDate.now());
                recommendation.setDimensionCode(dimensionCode);
                recommendation.setCreateTime(LocalDateTime.now());
                recommendation.setUpdateTime(LocalDateTime.now());

                recommendations.add(recommendation);
            }
        }

        // 批量保存建议到 health_recommendation 表
        if (!recommendations.isEmpty()) {
            for (HealthRecommendation rec : recommendations) {
                recommendationMapper.insert(rec);
                log.debug("保存健康建议成功（规则引擎），建议ID：{}，用户ID：{}，分析ID：{}", 
                        rec.getRecommendationId(), rec.getUserId(), rec.getAnalysisId());
            }
            log.info("规则引擎成功生成{}条健康建议，用户ID：{}", 
                    recommendations.size(), analysis.getUserId());
        } else {
            log.warn("未匹配到任何规则，未生成建议，用户ID：{}", analysis.getUserId());
        }
    }

    @Override
    public List<HealthAiAnalysis> getAnalysisByArchiveId(Integer archiveId) {
        LambdaQueryWrapper<HealthAiAnalysis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthAiAnalysis::getArchiveId, archiveId)
                .orderByDesc(HealthAiAnalysis::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<HealthAiAnalysis> getAnalysisByUserId(Long userId) {
        LambdaQueryWrapper<HealthAiAnalysis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthAiAnalysis::getUserId, userId)
                .orderByDesc(HealthAiAnalysis::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public Map<String, Object> getLatestAnalysisForUser(Long userId) {
        List<HealthAiAnalysis> list = getAnalysisByUserId(userId);
        if (list == null || list.isEmpty()) {
            return null;
        }
        HealthAiAnalysis latest = list.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("archiveId", latest.getArchiveId());
        result.put("sourceRawId", latest.getSourceRawId() != null ? latest.getSourceRawId() : 0);
        result.put("analysisId", latest.getAnalysisId());
        result.put("overallHealthScore", latest.getOverallHealthScore());
        result.put("riskLevel", latest.getRiskLevel());
        result.put("analysisSummary", latest.getAnalysisSummary());
        try {
            if (latest.getAnalysisDetail() != null && !latest.getAnalysisDetail().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> analysisResult = objectMapper.readValue(latest.getAnalysisDetail(), Map.class);
                result.put("analysisResult", analysisResult);
            } else {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("overallAssessment", latest.getAnalysisSummary());
                fallback.put("dimensionAnalysis", new ArrayList<>());
                result.put("analysisResult", fallback);
            }
        } catch (Exception e) {
            log.warn("解析 analysisDetail 失败，使用简化结构", e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("overallAssessment", latest.getAnalysisSummary());
            fallback.put("dimensionAnalysis", new ArrayList<>());
            result.put("analysisResult", fallback);
        }
        return result;
    }

    @Override
    public AiAnalysisDataResponse getUserCompleteData(Long userId) {
        log.info("开始获取用户{}的完整数据", userId);

        AiAnalysisDataResponse response = new AiAnalysisDataResponse();
        response.setUserId(userId);

        try {
            // 1. 获取该用户的所有原始健康数据
            List<Map<String, Object>> rawHealthDataList = rawHealthDataService.getAllLatestHealthDataByUserId(userId);
            response.setRawHealthDataList(rawHealthDataList);
            log.info("获取用户{}的原始数据，共{}条", userId, rawHealthDataList.size());

            // 2. 获取该用户的所有健康档案
            List<HealthArchive> archiveList = healthArchiveProcessService.getHealthArchiveListByUserId(userId.intValue());
            
            // 3. 为每个档案获取完整的从表数据
            List<Map<String, Object>> healthArchives = new ArrayList<>();
            for (HealthArchive archive : archiveList) {
                Map<String, Object> archiveDetail = healthArchiveProcessService.getHealthArchiveDetailById(archive.getArchiveId());
                healthArchives.add(archiveDetail);
            }
            response.setHealthArchives(healthArchives);
            log.info("获取用户{}的健康档案，共{}个", userId, healthArchives.size());

            return response;

        } catch (Exception e) {
            log.error("获取用户{}的完整数据失败", userId, e);
            throw new RuntimeException("获取用户完整数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存扩展数据到相关表
     * 从analysisResult的extendedData中提取数据并映射到对应的实体类
     */
    private void saveExtendedData(Integer archiveId, AiAnalysisRequest request) {
        if (request.getAnalysisResult() == null || 
            request.getAnalysisResult().getExtendedData() == null || 
            request.getAnalysisResult().getExtendedData().isEmpty()) {
            log.info("未提供扩展数据，跳过保存扩展表数据");
            return;
        }

        Map<String, Object> extendedData = request.getAnalysisResult().getExtendedData();
        log.info("开始保存扩展数据到相关表，档案ID：{}", archiveId);

        try {
            // 1. 保存生活方式状态表 (health_lifestyle_status)
            if (extendedData.containsKey("lifestyleStatus")) {
                Map<String, Object> lifestyleData = safeCastToMap(extendedData.get("lifestyleStatus"));
                if (lifestyleData != null) {
                    saveLifestyleStatus(archiveId, lifestyleData);
                }
            }

            // 2. 保存系统症状筛查表 (health_system_disease_screening)
            // 注意：用户提到的是health_system_symptom_screening，但实际表名是health_system_disease_screening
            if (extendedData.containsKey("systemSymptomScreening") || extendedData.containsKey("systemDiseaseScreening")) {
                Object screeningObj = extendedData.getOrDefault(
                    "systemSymptomScreening", extendedData.get("systemDiseaseScreening"));
                Map<String, Object> screeningData = safeCastToMap(screeningObj);
                if (screeningData != null) {
                    saveSystemDiseaseScreening(archiveId, screeningData);
                }
            }

            // 3. 保存心理评估表 (health_psychological_assessment)
            if (extendedData.containsKey("psychologicalAssessment")) {
                Map<String, Object> psychologicalData = safeCastToMap(extendedData.get("psychologicalAssessment"));
                if (psychologicalData != null) {
                    savePsychologicalAssessment(archiveId, psychologicalData);
                }
            }

            // 4. 保存社会背景表 (health_social_relationship_assessment)
            // 注意：用户提到的是health_social_background，但实际表名是health_social_relationship_assessment
            if (extendedData.containsKey("socialBackground") || extendedData.containsKey("socialRelationshipAssessment")) {
                Object socialObj = extendedData.getOrDefault(
                    "socialBackground", extendedData.get("socialRelationshipAssessment"));
                Map<String, Object> socialData = safeCastToMap(socialObj);
                if (socialData != null) {
                    saveSocialRelationshipAssessment(archiveId, socialData);
                }
            }

            // 5. 保存体格检查表 (health_physical_examination)
            if (extendedData.containsKey("physicalExamination")) {
                Map<String, Object> physicalData = safeCastToMap(extendedData.get("physicalExamination"));
                if (physicalData != null) {
                    savePhysicalExamination(archiveId, physicalData);
                }
            }

            log.info("扩展数据保存完成，档案ID：{}", archiveId);

        } catch (Exception e) {
            log.error("保存扩展数据失败，档案ID：{}", archiveId, e);
            // 不抛出异常，避免影响主流程
            log.warn("扩展数据保存失败，但继续执行后续流程");
        }
    }

    /**
     * 保存生活方式状态数据
     */
    private void saveLifestyleStatus(Integer archiveId, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            HealthLifestyleStatus lifestyle = new HealthLifestyleStatus();
            lifestyle.setArchiveId(archiveId);
            
            // 使用ObjectMapper将Map转换为实体，支持字段映射
            mapToEntity(data, lifestyle, HealthLifestyleStatus.class);
            
            lifestyle.setCreateTime(LocalDateTime.now());
            lifestyle.setUpdateTime(LocalDateTime.now());

            lifestyleStatusMapper.insert(lifestyle);
            log.info("保存生活方式状态数据成功，档案ID：{}", archiveId);
        } catch (Exception e) {
            log.error("保存生活方式状态数据失败，档案ID：{}", archiveId, e);
            throw e;
        }
    }

    /**
     * 保存系统疾病与症状筛查数据
     */
    private void saveSystemDiseaseScreening(Integer archiveId, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            HealthSystemDiseaseScreening screening = new HealthSystemDiseaseScreening();
            screening.setArchiveId(archiveId);
            
            mapToEntity(data, screening, HealthSystemDiseaseScreening.class);
            
            screening.setCreateTime(LocalDateTime.now());
            screening.setUpdateTime(LocalDateTime.now());

            systemScreeningMapper.insert(screening);
            log.info("保存系统疾病与症状筛查数据成功，档案ID：{}", archiveId);
        } catch (Exception e) {
            log.error("保存系统疾病与症状筛查数据失败，档案ID：{}", archiveId, e);
            throw e;
        }
    }

    /**
     * 保存心理评估数据
     */
    private void savePsychologicalAssessment(Integer archiveId, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            HealthPsychologicalAssessment assessment = new HealthPsychologicalAssessment();
            assessment.setArchiveId(archiveId);
            
            mapToEntity(data, assessment, HealthPsychologicalAssessment.class);
            
            assessment.setCreateTime(LocalDateTime.now());
            assessment.setUpdateTime(LocalDateTime.now());

            psychologicalAssessmentMapper.insert(assessment);
            log.info("保存心理评估数据成功，档案ID：{}", archiveId);
        } catch (Exception e) {
            log.error("保存心理评估数据失败，档案ID：{}", archiveId, e);
            throw e;
        }
    }

    /**
     * 保存社会关系评估数据
     */
    private void saveSocialRelationshipAssessment(Integer archiveId, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            HealthSocialRelationshipAssessment assessment = new HealthSocialRelationshipAssessment();
            assessment.setArchiveId(archiveId);
            
            mapToEntity(data, assessment, HealthSocialRelationshipAssessment.class);
            
            assessment.setCreateTime(LocalDateTime.now());
            assessment.setUpdateTime(LocalDateTime.now());

            socialAssessmentMapper.insert(assessment);
            log.info("保存社会关系评估数据成功，档案ID：{}", archiveId);
        } catch (Exception e) {
            log.error("保存社会关系评估数据失败，档案ID：{}", archiveId, e);
            throw e;
        }
    }

    /**
     * 保存体格检查数据
     */
    private void savePhysicalExamination(Integer archiveId, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            HealthPhysicalExamination examination = new HealthPhysicalExamination();
            examination.setArchiveId(archiveId);
            
            mapToEntity(data, examination, HealthPhysicalExamination.class);
            
            examination.setCreateTime(LocalDateTime.now());
            examination.setUpdateTime(LocalDateTime.now());

            physicalExaminationMapper.insert(examination);
            log.info("保存体格检查数据成功，档案ID：{}", archiveId);
        } catch (Exception e) {
            log.error("保存体格检查数据失败，档案ID：{}", archiveId, e);
            throw e;
        }
    }

    /**
     * 安全地将Object转换为Map<String, Object>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> safeCastToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        log.warn("无法将对象转换为Map: {}", obj.getClass().getName());
        return null;
    }

    /**
     * 将Map数据映射到实体类
     * 使用反射和ObjectMapper进行字段映射
     */
    private <T> void mapToEntity(Map<String, Object> data, T entity, Class<T> entityClass) {
        try {
            // 使用ObjectMapper将Map转换为实体对象
            // 这样可以自动处理字段名映射和类型转换
            T mappedEntity = objectMapper.convertValue(data, entityClass);
            
            // 将映射后的实体字段复制到目标实体
            // 使用反射复制所有非null字段
            java.lang.reflect.Field[] fields = entityClass.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(mappedEntity);
                    if (value != null) {
                        field.set(entity, value);
                    }
                } catch (Exception e) {
                    // 忽略无法设置的字段
                    log.debug("无法设置字段 {}: {}", field.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("使用ObjectMapper映射失败，尝试手动映射: {}", e.getMessage());
            // 如果ObjectMapper失败，尝试手动映射常见字段
            manualMapFields(data, entity, entityClass);
        }
    }

    /**
     * 手动映射字段（备用方案）
     */
    private <T> void manualMapFields(Map<String, Object> data, T entity, Class<T> entityClass) {
        java.lang.reflect.Field[] fields = entityClass.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            try {
                String fieldName = field.getName();
                if (data.containsKey(fieldName)) {
                    field.setAccessible(true);
                    Object value = data.get(fieldName);
                    
                    // 简单的类型转换
                    if (value != null) {
                        Class<?> fieldType = field.getType();
                        if (fieldType.isAssignableFrom(value.getClass())) {
                            field.set(entity, value);
                        } else if (fieldType == String.class) {
                            field.set(entity, value.toString());
                        } else if (fieldType == Integer.class && value instanceof Number) {
                            field.set(entity, ((Number) value).intValue());
                        } else if (fieldType == Boolean.class && value instanceof Boolean) {
                            field.set(entity, value);
                        } else if (fieldType == java.math.BigDecimal.class && value instanceof Number) {
                            field.set(entity, java.math.BigDecimal.valueOf(((Number) value).doubleValue()));
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("手动映射字段 {} 失败: {}", field.getName(), e.getMessage());
            }
        }
    }
}

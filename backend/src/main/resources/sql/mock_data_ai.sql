-- =====================================================
-- AI健康分析模拟数据
-- =====================================================

-- 插入AI分析结果（模拟数据）
INSERT INTO health_ai_analysis (
    user_id, 
    archive_id, 
    source_raw_id, 
    model_name, 
    model_version, 
    overall_health_score, 
    risk_level, 
    analysis_summary, 
    analysis_detail, 
    analysis_meta,
    create_time
) VALUES
-- 示例1：用户2，档案ID=1，代谢高风险
(2, 1, 100, 'health-ai-v1', '1.0.0', 68, '高', 
 '体检结果显示部分代谢及血脂指标偏离参考范围，建议持续关注。',
 '{
   "dimensionAnalysis": [
     {
       "dimensionCode": "METABOLIC",
       "dimensionName": "代谢健康",
       "riskLevel": "高",
       "evidence": [
         {"indicator": "BMI", "value": "27.1", "reference": "18.5-23.9"},
         {"indicator": "甘油三酯", "value": "4.25", "reference": "<1.7 mmol/L"}
       ],
       "interpretation": "体重指数及血脂指标高于参考范围，提示存在代谢异常风险。",
       "confidence": 0.82
     },
     {
       "dimensionCode": "GLYCEMIC",
       "dimensionName": "血糖控制",
       "riskLevel": "中",
       "evidence": [
         {"indicator": "HbA1c", "value": "6.2%", "reference": "<5.7%"}
       ],
       "interpretation": "糖化血红蛋白略高于参考范围，提示血糖控制需要关注。",
       "confidence": 0.76
     }
   ],
   "overallAssessment": {
     "overallHealthScore": 68,
     "riskLevel": "高",
     "keyRisks": ["代谢异常风险", "血脂异常风险"],
     "summary": "体检结果显示部分代谢及血脂指标偏离参考范围，建议持续关注。"
   },
   "abnormalIndicators": [
     {
       "indicatorCode": "TG",
       "indicatorName": "甘油三酯",
       "value": "4.25",
       "reference": "<1.7",
       "severity": "高",
       "relatedDimension": "METABOLIC"
     },
     {
       "indicatorCode": "HbA1c",
       "indicatorName": "糖化血红蛋白",
       "value": "6.2%",
       "reference": "<5.7%",
       "severity": "中",
       "relatedDimension": "GLYCEMIC"
     }
   ],
   "modelConfidence": {
     "overallConfidence": 0.78,
     "limitations": ["部分指标来源于单次检测", "未包含长期随访数据"]
   }
 }',
 '{
   "sourceType": "PHYSICAL_REPORT",
   "sourceDate": "2025-01-05",
   "analysisTime": "2025-01-06T10:32:00",
   "populationGroup": ["成人", "非孕", "非老年"]
 }',
 NOW()),

-- 示例2：用户2，档案ID=2，泌尿系统异常
(2, 2, 101, 'health-ai-v1', '1.0.0', 75, '中',
 '体检结果显示部分泌尿系统指标存在异常，需要进一步关注。',
 '{
   "dimensionAnalysis": [
     {
       "dimensionCode": "URINARY",
       "dimensionName": "泌尿系统",
       "riskLevel": "高",
       "evidence": [
         {"indicator": "尿隐血", "value": "+", "reference": "阴性"},
         {"indicator": "红细胞", "value": "15/HPF", "reference": "<3/HPF"}
       ],
       "interpretation": "尿检结果显示隐血阳性和红细胞增多，提示可能存在泌尿系统异常。",
       "confidence": 0.70
     }
   ],
   "overallAssessment": {
     "overallHealthScore": 75,
     "riskLevel": "中",
     "keyRisks": ["泌尿系统异常风险"],
     "summary": "体检结果显示部分泌尿系统指标存在异常，需要进一步关注。"
   },
   "abnormalIndicators": [
     {
       "indicatorCode": "URINE_BLOOD",
       "indicatorName": "尿隐血",
       "value": "+",
       "reference": "阴性",
       "severity": "中",
       "relatedDimension": "URINARY"
     },
     {
       "indicatorCode": "RBC",
       "indicatorName": "红细胞",
       "value": "15/HPF",
       "reference": "<3/HPF",
       "severity": "高",
       "relatedDimension": "URINARY"
     }
   ],
   "modelConfidence": {
     "overallConfidence": 0.70,
     "limitations": ["单次检测结果", "需结合临床症状"]
   }
 }',
 '{
   "sourceType": "PHYSICAL_REPORT",
   "sourceDate": "2025-01-10",
   "analysisTime": "2025-01-11T14:20:00",
   "populationGroup": ["成人", "非孕", "非老年"]
 }',
 NOW()),

-- 示例3：用户3，档案ID=3，整体健康良好
(3, 3, 102, 'health-ai-v1', '1.0.0', 88, '低',
 '体检结果整体良好，各项指标基本在正常范围内，建议继续保持良好的生活习惯。',
 '{
   "dimensionAnalysis": [
     {
       "dimensionCode": "GENERAL",
       "dimensionName": "整体健康",
       "riskLevel": "低",
       "evidence": [
         {"indicator": "血压", "value": "120/80", "reference": "<140/90"},
         {"indicator": "心率", "value": "72", "reference": "60-100"}
       ],
       "interpretation": "各项基础指标在正常范围内，整体健康状况良好。",
       "confidence": 0.85
     }
   ],
   "overallAssessment": {
     "overallHealthScore": 88,
     "riskLevel": "低",
     "keyRisks": [],
     "summary": "体检结果整体良好，各项指标基本在正常范围内，建议继续保持良好的生活习惯。"
   },
   "abnormalIndicators": [],
   "modelConfidence": {
     "overallConfidence": 0.85,
     "limitations": ["建议定期体检"]
   }
 }',
 '{
   "sourceType": "PHYSICAL_REPORT",
   "sourceDate": "2025-01-08",
   "analysisTime": "2025-01-09T09:15:00",
   "populationGroup": ["成人", "非孕", "非老年"]
 }',
 NOW());

-- 注意：建议数据会通过规则引擎自动生成，这里不手动插入
-- 如果需要手动插入建议数据，可以参考以下格式：

-- INSERT INTO health_recommendation (
--     analysis_id, 
--     user_id, 
--     recommendation_type, 
--     title, 
--     content, 
--     priority, 
--     is_active, 
--     dimension_code,
--     valid_from,
--     create_time
-- ) VALUES
-- (1, 2, 'LIFESTYLE', '关注代谢健康，建议进行生活方式管理', '根据体检结果显示，部分代谢指标与参考范围存在差异...', '高', TRUE, 'METABOLIC', CURDATE(), NOW()),
-- (1, 2, 'MEDICAL', '建议进一步关注相关指标', '体检结果中部分代谢指标需要进一步关注...', '高', TRUE, 'METABOLIC', CURDATE(), NOW());

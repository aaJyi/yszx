"""
扩展数据结构定义模块
用于定义 extendedData 字段的5张表的完整字段列表和工具函数
"""
from typing import Dict, Any, List, Set
import logging

logger = logging.getLogger(__name__)

# ========== 1. lifestyleStatus（生活方式状态表 - health_lifestyle_status）==========
# 共34个字段
LIFESTYLE_STATUS_FIELDS = [
    "dietType",                    # String (SET) - 主要膳食种类
    "mealsRegular",                # Boolean - 三餐是否规律
    "mealsIrregularDesc",          # String (TEXT) - 三餐不规律描述
    "eatOutFrequency",             # Integer - 外出用餐频率（次/周）
    "specialDietHabit",            # String (ENUM) - 特殊饮食习惯
    "specialDietDesc",             # String (TEXT) - 特殊饮食习惯描述
    "appetite",                    # String (ENUM) - 食欲
    "urination",                   # String (ENUM) - 排尿情况
    "defecationStatus",             # String (ENUM) - 排便情况
    "constipationDays",            # Integer - 便秘持续天数
    "constipationNeedAssist",       # Boolean - 是否辅助排便
    "diarrheaTimesPerDay",         # Integer - 腹泻次数（次/日）
    "activityAbility",              # String (ENUM) - 活动能力
    "selfCareAbility",              # String (ENUM) - 自理能力
    "exerciseType",                 # String (SET) - 体格锻炼方式
    "exerciseFrequency",            # Integer - 体格锻炼频率（次/周）
    "commuteMode",                  # String (SET) - 外出/上班方式
    "routineRegular",               # Boolean - 作息是否规律
    "routineIrregularDesc",         # String (TEXT) - 作息不规律描述
    "sleepStatus",                  # String (ENUM) - 睡眠情况
    "sleepDesc",                   # String (TEXT) - 睡眠异常描述
    "regularTherapy",               # String (ENUM) - 定期保养/理疗
    "therapyProject",               # String (TEXT) - 理疗项目
    "therapyFrequencyPerYear",      # Integer - 理疗频率（次/年）
    "regularCheckup",               # String (ENUM) - 是否定期体检
    "checkupFrequencyPerYear",      # Integer - 体检频率（次/年）
    "weightControl",                # String (ENUM) - 减肥/增重行为
    "weightChange",                 # String (ENUM) - 体重与去年对比
    "smokingStatus",                # String (ENUM) - 吸烟情况
    "cigarettesPerDay",             # Integer - 吸烟量（支/日）
    "smokingYears",                 # Integer - 已吸烟年数
    "quitSmokingYears",             # Integer - 已戒烟年数
    "drinkingStatus",               # String (ENUM) - 饮酒情况
    "drinkingTimesPerDay",          # Integer - 饮酒次数（次/日）
    "drinkingYears",                # Integer - 已饮酒年数
    "quitDrinkingYears",            # Integer - 已戒酒年数
    "drugDependence",               # String (ENUM) - 药物依赖
    "drugDependenceDesc"            # String (TEXT) - 药物名称及剂量描述
]

# ========== 2. systemDiseaseScreening（系统疾病与症状筛查表 - health_system_disease_screening）==========
# 共10个字段
SYSTEM_DISEASE_SCREENING_FIELDS = [
    "headFacialSymptoms",          # String (SET) - 头颅五官系统症状
    "respiratorySymptoms",          # String (SET) - 呼吸系统症状
    "circulatorySymptoms",          # String (SET) - 循环系统症状
    "digestiveSymptoms",            # String (SET) - 消化系统症状
    "urogenitalSymptoms",           # String (SET) - 泌尿生殖系统症状
    "endocrineMetabolicSymptoms",   # String (SET) - 内分泌与代谢症状
    "hematopoieticSymptoms",        # String (SET) - 造血系统症状
    "musculoskeletalSymptoms",      # String (SET) - 肌肉骨骼系统症状
    "neurologicalSymptoms",         # String (SET) - 神经系统症状
    "mentalStateSymptoms"           # String (SET) - 精神状态症状
]

# ========== 3. psychologicalAssessment（心理评估表 - health_psychological_assessment）==========
# 共13个字段
PSYCHOLOGICAL_ASSESSMENT_FIELDS = [
    "fatigueDepression",           # String (ENUM) - 疲劳、压抑
    "memoryDecline",                # String (ENUM) - 记忆力减退
    "adaptabilityDecline",          # String (ENUM) - 适应能力减退
    "vitalityResponseDecline",      # String (ENUM) - 活力、反应能力减退
    "emotionStatus",                # String (SET) - 情绪状态
    "stressStatus",                 # String (ENUM) - 是否存在压力
    "stressSource",                 # String (SET) - 压力来源
    "stressReliefMethods",          # String (SET) - 缓压方法
    "selfPerception",               # String (ENUM) - 对自我的看法
    "diseaseCognition",             # String (ENUM) - 对疾病的认识程度
    "majorLifeEvent",               # String (ENUM) - 过去1年内是否有重要生活事件
    "majorLifeEventDesc",           # String (TEXT) - 重要生活事件描述
    "preferredConfidant"             # String (SET) - 遇到困难最愿倾诉对象
]

# ========== 4. socialRelationshipAssessment（社会关系评估表 - health_social_relationship_assessment）==========
# 共7个字段
SOCIAL_RELATIONSHIP_ASSESSMENT_FIELDS = [
    "familyRelationship",           # String (ENUM) - 家庭关系
    "maritalStatus",                # String (ENUM) - 婚姻状况
    "livingCondition",              # String (ENUM) - 居住情况
    "occupationType",                # String (ENUM) - 职业性质
    "educationLevel",               # String (ENUM) - 文化程度
    "socialInteraction",             # String (ENUM) - 社会交往情况
    "medicalPaymentMethod"           # String (ENUM) - 医疗费用支付形式
]

# ========== 5. physicalExamination（体格检查表 - health_physical_examination）==========
# 共80个字段
PHYSICAL_EXAMINATION_FIELDS = [
    "temperature",                  # BigDecimal - 体温 ℃
    "pulse",                        # Integer - 脉搏 次/分
    "respiration",                  # Integer - 呼吸 次/分
    "systolicBp",                   # Integer - 收缩压 mmHg
    "diastolicBp",                  # Integer - 舒张压 mmHg
    "heightCm",                     # BigDecimal - 身高 cm
    "weightKg",                     # BigDecimal - 体重 kg
    "gluValue",                     # BigDecimal - 血糖 mmol/L
    "gluType",                      # String (ENUM) - 血糖类型
    "development",                  # String (ENUM) - 发育情况
    "developmentDesc",              # String (TEXT) - 发育异常描述
    "nutrition",                    # String (ENUM) - 营养状况
    "bodyType",                     # String (ENUM) - 体型
    "facialExpression",             # String (ENUM) - 面容
    "facialExpressionDesc",         # String (TEXT) - 病容类型
    "posture",                      # String (ENUM) - 体位
    "postureDesc",                  # String (TEXT) - 强迫体位类型
    "gait",                         # String (ENUM) - 步态
    "gaitDesc",                     # String (TEXT) - 异常步态描述
    "consciousness",                # String (ENUM) - 意识状态
    "speech",                       # String (ENUM) - 语言表达
    "skinColor",                    # String (SET) - 皮肤颜色
    "skinMoisture",                 # String (ENUM) - 皮肤湿度
    "skinTemperature",              # String (ENUM) - 皮肤温度
    "skinElasticity",               # String (ENUM) - 皮肤弹性
    "edema",                        # String (ENUM) - 水肿
    "edemaDesc",                    # String (TEXT) - 水肿部位及程度
    "skinIntegrity",                # String (SET) - 皮肤完整性
    "skinIntegrityDesc",            # String (TEXT) - 皮损描述
    "lymphNodes",                   # String (ENUM) - 淋巴结
    "eyelid",                       # String (ENUM) - 眼睑
    "conjunctiva",                  # String (ENUM) - 结膜
    "sclera",                       # String (ENUM) - 巩膜
    "pupil",                        # String (ENUM) - 瞳孔
    "pupilDesc",                    # String (TEXT) - 瞳孔异常描述
    "lightReflex",                  # String (ENUM) - 对光反射
    "lips",                         # String (SET) - 口唇
    "oralMucosa",                   # String (SET) - 口腔黏膜
    "teeth",                        # String (ENUM) - 牙齿
    "vision",                       # String (ENUM) - 视力
    "visionDesc",                   # String (TEXT) - 视力异常描述
    "hearing",                      # String (ENUM) - 听力
    "hearingDesc",                  # String (TEXT) - 听力异常描述
    "smell",                        # String (ENUM) - 嗅觉
    "smellDesc",                    # String (TEXT) - 嗅觉异常描述
    "neckStiffness",                # String (ENUM) - 颈项强直
    "jugularVein",                  # String (ENUM) - 颈静脉
    "trachea",                      # String (ENUM) - 气管
    "hepatojugularReflex",          # String (ENUM) - 肝颈静脉回流征
    "breathingMode",                # String (ENUM) - 呼吸方式
    "breathingRhythm",              # String (ENUM) - 呼吸节律
    "breathingRhythmDesc",          # String (TEXT) - 呼吸节律异常描述
    "dyspnea",                      # String (ENUM) - 呼吸困难
    "breathSound",                  # String (ENUM) - 呼吸音
    "breathSoundDesc",              # String (TEXT) - 异常呼吸音描述
    "rales",                        # String (ENUM) - 啰音
    "ralesDesc",                    # String (TEXT) - 啰音描述
    "heartRate",                    # Integer - 心率 次/分
    "heartRhythm",                  # String (ENUM) - 心律
    "murmur",                       # String (ENUM) - 杂音
    "murmurDesc",                   # String (TEXT) - 杂音描述
    "abdomenShape",                 # String (ENUM) - 腹部外形
    "abdominalMass",                # String (ENUM) - 腹部包块
    "abdominalMassDesc",            # String (TEXT) - 包块描述
    "abdominalTension",             # String (ENUM) - 腹肌紧张
    "abdominalTensionDesc",          # String (TEXT) - 腹肌紧张描述
    "tenderness",                   # String (ENUM) - 压痛
    "tendernessDesc",               # String (TEXT) - 压痛描述
    "reboundTenderness",            # String (ENUM) - 反跳痛
    "reboundTendernessDesc",        # String (TEXT) - 反跳痛描述
    "hepatomegaly",                 # String (ENUM) - 肝大
    "hepatomegalyDesc",             # String (TEXT) - 肝大描述
    "splenomegaly",                 # String (ENUM) - 脾大
    "splenomegalyDesc",             # String (TEXT) - 脾大描述
    "shiftingDullness",             # String (ENUM) - 移动性浊音
    "bowelSounds",                  # Integer - 肠鸣音 次/分
    "bowelSoundStatus",             # String (ENUM) - 肠鸣音状态
    "rectalExam",                   # String (ENUM) - 直肠肛门
    "rectalExamDesc",               # String (TEXT) - 直肠异常描述
    "genitalExam",                  # String (ENUM) - 外生殖器
    "genitalExamDesc",              # String (TEXT) - 外生殖器异常描述
    "spineShape",                   # String (ENUM) - 脊柱外形
    "spineDesc",                    # String (TEXT) - 脊柱畸形描述
    "spineActivity",                # String (ENUM) - 脊柱活动
    "limbShape",                    # String (ENUM) - 四肢外形
    "limbDesc",                     # String (TEXT) - 四肢畸形描述
    "limbActivity",                 # String (ENUM) - 四肢活动
    "pain",                         # String (ENUM) - 疼痛
    "painDesc",                     # String (TEXT) - 疼痛描述
    "painScore",                    # String (ENUM) - 疼痛评分
    "muscleStrength",               # String (ENUM) - 肌力
    "muscleStrengthDesc",           # String (TEXT) - 肌力异常描述
    "paralysis",                    # String (ENUM) - 肢体瘫痪
    "paralysisDesc",                # String (TEXT) - 瘫痪描述
    "muscleStrengthGrade",          # Integer - 肌力分级
    "pathologicalReflex",           # String (ENUM) - 病理反射
    "meningealSign",                # String (ENUM) - 脑膜刺激征
    "meningealSignType"             # String (SET) - 脑膜刺激征类型
]


def build_default_extended_data() -> Dict[str, Any]:
    """
    构建默认的 extendedData 结构
    
    为所有5张表的所有字段生成默认值。
    默认值策略：
    - 布尔类型：false
    - 整数类型：0
    - 字符串类型（ENUM/SET）：null 或合理的默认值（如 "0-正常"）
    - 字符串类型（TEXT）：null
    - BigDecimal 类型：null
    
    Returns:
        包含5张表默认数据的字典
    """
    return {
        "lifestyleStatus": _build_default_lifestyle_status(),
        "systemDiseaseScreening": _build_default_system_disease_screening(),
        "psychologicalAssessment": _build_default_psychological_assessment(),
        "socialRelationshipAssessment": _build_default_social_relationship_assessment(),
        "physicalExamination": _build_default_physical_examination()
    }


def _build_default_lifestyle_status() -> Dict[str, Any]:
    """构建生活方式状态表的默认值"""
    return {
        "dietType": None,
        "mealsRegular": False,
        "mealsIrregularDesc": None,
        "eatOutFrequency": 0,
        "specialDietHabit": "0-无",
        "specialDietDesc": None,
        "appetite": "0-正常",
        "urination": "0-正常",
        "defecationStatus": "0-正常",
        "constipationDays": 0,
        "constipationNeedAssist": False,
        "diarrheaTimesPerDay": 0,
        "activityAbility": "0-无限制",
        "selfCareAbility": "0-完全自理",
        "exerciseType": None,
        "exerciseFrequency": 0,
        "commuteMode": None,
        "routineRegular": False,
        "routineIrregularDesc": None,
        "sleepStatus": "0-正常",
        "sleepDesc": None,
        "regularTherapy": "0-无",
        "therapyProject": None,
        "therapyFrequencyPerYear": 0,
        "regularCheckup": "0-无",
        "checkupFrequencyPerYear": 0,
        "weightControl": "0-无",
        "weightChange": "0-基本无差异",
        "smokingStatus": "0-无",
        "cigarettesPerDay": 0,
        "smokingYears": 0,
        "quitSmokingYears": 0,
        "drinkingStatus": "0-无",
        "drinkingTimesPerDay": 0,
        "drinkingYears": 0,
        "quitDrinkingYears": 0,
        "drugDependence": "0-无",
        "drugDependenceDesc": None
    }


def _build_default_system_disease_screening() -> Dict[str, Any]:
    """构建系统疾病与症状筛查表的默认值"""
    return {
        "headFacialSymptoms": "0-正常/无异",
        "respiratorySymptoms": "0-正常/无异",
        "circulatorySymptoms": "0-正常/无异",
        "digestiveSymptoms": "0-正常/无异",
        "urogenitalSymptoms": "0-正常/无异",
        "endocrineMetabolicSymptoms": "0-正常/无异",
        "hematopoieticSymptoms": "0-正常/无异",
        "musculoskeletalSymptoms": "0-正常/无异",
        "neurologicalSymptoms": "0-正常/无异",
        "mentalStateSymptoms": "0-正常/无异"
    }


def _build_default_psychological_assessment() -> Dict[str, Any]:
    """构建心理评估表的默认值"""
    return {
        "fatigueDepression": "0-无",
        "memoryDecline": "0-无",
        "adaptabilityDecline": "0-无",
        "vitalityResponseDecline": "0-无",
        "emotionStatus": "0-镇静",
        "stressStatus": "0-无",
        "stressSource": None,
        "stressReliefMethods": None,
        "selfPerception": "0-满意",
        "diseaseCognition": "0-完全认识",
        "majorLifeEvent": "0-无",
        "majorLifeEventDesc": None,
        "preferredConfidant": None
    }


def _build_default_social_relationship_assessment() -> Dict[str, Any]:
    """构建社会关系评估表的默认值"""
    return {
        "familyRelationship": "0-和睦",
        "maritalStatus": None,  # 需要从用户信息中获取
        "livingCondition": "1-和家人同住",
        "occupationType": None,  # 需要从用户信息中获取
        "educationLevel": None,  # 需要从用户信息中获取
        "socialInteraction": "1-正常",
        "medicalPaymentMethod": "1-医疗保险"
    }


def _build_default_physical_examination() -> Dict[str, Any]:
    """构建体格检查表的默认值"""
    return {
        "temperature": None,
        "pulse": None,
        "respiration": None,
        "systolicBp": None,
        "diastolicBp": None,
        "heightCm": None,
        "weightKg": None,
        "gluValue": None,
        "gluType": None,
        "development": "0-正常",
        "developmentDesc": None,
        "nutrition": "0-良好",
        "bodyType": "0-正常",
        "facialExpression": "0-正常",
        "facialExpressionDesc": None,
        "posture": "0-主动",
        "postureDesc": None,
        "gait": "0-正常",
        "gaitDesc": None,
        "consciousness": "0-清楚",
        "speech": "0-清楚",
        "skinColor": "0-正常",
        "skinMoisture": "0-正常",
        "skinTemperature": "0-正常",
        "skinElasticity": "0-正常",
        "edema": "0-无",
        "edemaDesc": None,
        "skinIntegrity": "0-完整",
        "skinIntegrityDesc": None,
        "lymphNodes": "0-正常",
        "eyelid": "0-正常",
        "conjunctiva": "0-正常",
        "sclera": "0-正常",
        "pupil": "0-正常",
        "pupilDesc": None,
        "lightReflex": "0-正常",
        "lips": "0-红润",
        "oralMucosa": "0-正常",
        "teeth": "0-完好",
        "vision": "0-正常",
        "visionDesc": None,
        "hearing": "0-正常",
        "hearingDesc": None,
        "smell": "0-正常",
        "smellDesc": None,
        "neckStiffness": "0-无",
        "jugularVein": "0-正常",
        "trachea": "0-居中",
        "hepatojugularReflex": "0-阴性",
        "breathingMode": "0-自主呼吸",
        "breathingRhythm": "0-规则",
        "breathingRhythmDesc": None,
        "dyspnea": "0-无",
        "breathSound": "0-正常",
        "breathSoundDesc": None,
        "rales": "0-无",
        "ralesDesc": None,
        "heartRate": None,
        "heartRhythm": "0-齐",
        "murmur": "0-无",
        "murmurDesc": None,
        "abdomenShape": "0-正常",
        "abdominalMass": "0-无",
        "abdominalMassDesc": None,
        "abdominalTension": "0-无",
        "abdominalTensionDesc": None,
        "tenderness": "0-无",
        "tendernessDesc": None,
        "reboundTenderness": "0-无",
        "reboundTendernessDesc": None,
        "hepatomegaly": "0-无",
        "hepatomegalyDesc": None,
        "splenomegaly": "0-无",
        "splenomegalyDesc": None,
        "shiftingDullness": "0-阴性",
        "bowelSounds": None,
        "bowelSoundStatus": "0-正常",
        "rectalExam": "0-未查",
        "rectalExamDesc": None,
        "genitalExam": "0-未查",
        "genitalExamDesc": None,
        "spineShape": "0-正常",
        "spineDesc": None,
        "spineActivity": "0-正常",
        "limbShape": "0-正常",
        "limbDesc": None,
        "limbActivity": "0-正常",
        "pain": "0-无",
        "painDesc": None,
        "painScore": None,
        "muscleStrength": "0-正常",
        "muscleStrengthDesc": None,
        "paralysis": "0-无",
        "paralysisDesc": None,
        "muscleStrengthGrade": None,
        "pathologicalReflex": "0-阴性",
        "meningealSign": "0-无",
        "meningealSignType": None
    }


def merge_extended_data(llm_data: Dict[str, Any], default_data: Dict[str, Any]) -> Dict[str, Any]:
    """
    合并 LLM 生成的数据和默认值
    
    对于每张表，如果 LLM 生成了数据，则使用 LLM 的数据（保留已存在的字段），
    对于缺失的字段，使用默认值填充。
    
    Args:
        llm_data: LLM 生成的 extendedData（可能不完整）
        default_data: 默认的 extendedData 结构
    
    Returns:
        合并后的 extendedData 字典
    """
    merged = {}
    
    # 定义5张表的字段列表
    tables = {
        "lifestyleStatus": LIFESTYLE_STATUS_FIELDS,
        "systemDiseaseScreening": SYSTEM_DISEASE_SCREENING_FIELDS,
        "systemSymptomScreening": SYSTEM_DISEASE_SCREENING_FIELDS,  # 别名
        "psychologicalAssessment": PSYCHOLOGICAL_ASSESSMENT_FIELDS,
        "socialRelationshipAssessment": SOCIAL_RELATIONSHIP_ASSESSMENT_FIELDS,
        "socialBackground": SOCIAL_RELATIONSHIP_ASSESSMENT_FIELDS,  # 别名
        "physicalExamination": PHYSICAL_EXAMINATION_FIELDS
    }
    
    # 遍历每张表
    for table_name, field_list in tables.items():
        # 获取 LLM 生成的数据（可能为空或部分字段）
        llm_table_data = llm_data.get(table_name, {})
        
        # 如果 LLM 使用了别名，也尝试获取
        if not llm_table_data:
            if table_name == "systemDiseaseScreening":
                llm_table_data = llm_data.get("systemSymptomScreening", {})
            elif table_name == "socialRelationshipAssessment":
                llm_table_data = llm_data.get("socialBackground", {})
        
        # 获取默认数据
        default_table_data = default_data.get(table_name, {})
        
        # 合并：优先使用 LLM 数据，缺失的字段使用默认值
        merged_table_data = {}
        for field in field_list:
            if field in llm_table_data:
                # LLM 提供了该字段，使用 LLM 的值
                merged_table_data[field] = llm_table_data[field]
            elif field in default_table_data:
                # 使用默认值
                merged_table_data[field] = default_table_data[field]
            else:
                # 默认值也没有，使用 None
                merged_table_data[field] = None
        
        # 保存合并后的表数据（使用标准表名）
        if table_name in ["systemDiseaseScreening", "systemSymptomScreening"]:
            merged["systemDiseaseScreening"] = merged_table_data
        elif table_name in ["socialRelationshipAssessment", "socialBackground"]:
            merged["socialRelationshipAssessment"] = merged_table_data
        else:
            merged[table_name] = merged_table_data
    
    return merged


def validate_extended_data(extended_data: Dict[str, Any]) -> bool:
    """
    验证 extendedData 字段完整性
    
    检查是否包含所有5张表，以及每张表是否包含所有必需字段。
    
    Args:
        extended_data: 待验证的 extendedData 字典
    
    Returns:
        如果字段完整返回 True，否则返回 False
    """
    # 定义5张表的必需字段
    required_tables = {
        "lifestyleStatus": LIFESTYLE_STATUS_FIELDS,
        "systemDiseaseScreening": SYSTEM_DISEASE_SCREENING_FIELDS,
        "psychologicalAssessment": PSYCHOLOGICAL_ASSESSMENT_FIELDS,
        "socialRelationshipAssessment": SOCIAL_RELATIONSHIP_ASSESSMENT_FIELDS,
        "physicalExamination": PHYSICAL_EXAMINATION_FIELDS
    }
    
    # 检查每张表是否存在
    for table_name, field_list in required_tables.items():
        if table_name not in extended_data:
            logger.warning(f"extendedData 缺少表: {table_name}")
            return False
        
        table_data = extended_data[table_name]
        if not isinstance(table_data, dict):
            logger.warning(f"extendedData.{table_name} 不是字典类型")
            return False
        
        # 检查每个字段是否存在
        missing_fields = [field for field in field_list if field not in table_data]
        if missing_fields:
            logger.warning(f"extendedData.{table_name} 缺少字段: {', '.join(missing_fields)}")
            return False
    
    logger.debug("extendedData 字段完整性验证通过")
    return True

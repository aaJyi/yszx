package com.android.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * PDF生成工具类
 *
 * @author sjt
 * @since 2026-01-20
 */
@Slf4j
public class PdfUtil {

    private static BaseFont baseFont;
    private static final ObjectMapper objectMapper;

    static {
        // 初始化 ObjectMapper，支持 Java 8 时间类型
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    static {
        try {
            // 尝试使用Adobe CJK内建字体（STSong-Light）支持中文
            try {
                baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
                log.info("成功加载中文字体：STSong-Light");
            } catch (Exception e) {
                // 如果STSong-Light不可用，尝试使用Helvetica作为备选
                try {
                    baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                    log.warn("无法加载中文字体STSong-Light，使用Helvetica，中文可能显示异常");
                } catch (Exception e2) {
                    log.error("初始化字体完全失败", e2);
                    // 使用最简单的字体作为最后备选
                    baseFont = BaseFont.createFont();
                }
            }
        } catch (Exception e) {
            log.error("初始化字体失败", e);
            try {
                baseFont = BaseFont.createFont();
            } catch (Exception e2) {
                log.error("无法创建默认字体", e2);
            }
        }
    }

    /**
     * 生成健康档案PDF
     *
     * @param archiveData 健康档案数据
     * @return PDF文件的字节数组
     */
    public static byte[] generateHealthArchivePdf(Map<String, Object> archiveData) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = new Font(baseFont, 20, Font.BOLD);
            Font headerFont = new Font(baseFont, 14, Font.BOLD);
            Font normalFont = new Font(baseFont, 10, Font.NORMAL);

            // 标题
            Paragraph title = new Paragraph("健康档案", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 基本信息
            Map<String, Object> healthArchive = convertToMap(archiveData.get("healthArchive"));
            if (healthArchive != null) {
                addSection(document, "健康档案基本信息", headerFont, normalFont);
                addKeyValue(document, "档案姓名", getString(healthArchive.get("userName")), normalFont);
                addKeyValue(document, "档案编号", getString(healthArchive.get("archiveNo")), normalFont);
                addKeyValue(document, "档案名称", getString(healthArchive.get("archiveName")), normalFont);
                addKeyValue(document, "档案日期", getString(healthArchive.get("archiveDate")), normalFont);
                document.add(new Paragraph(" ")); // 空行
            }

            // 用户基本信息
            Map<String, Object> userInfo = convertToMap(archiveData.get("userInfo"));
            if (userInfo != null) {
                addSection(document, "用户基本信息", headerFont, normalFont);
                addKeyValue(document, "姓名", getString(userInfo.get("fullName")), normalFont);
                addKeyValue(document, "性别", getString(userInfo.get("gender")), normalFont);
                addKeyValue(document, "出生日期", getString(userInfo.get("birthDate")), normalFont);
                addKeyValue(document, "联系电话", getString(userInfo.get("personalPhone")), normalFont);
                document.add(new Paragraph(" ")); // 空行
            }

            // 生活方式状态
            Map<String, Object> lifestyleStatus = convertToMap(archiveData.get("lifestyleStatus"));
            if (lifestyleStatus != null) {
                addSection(document, "生活方式状态", headerFont, normalFont);
                addKeyValue(document, "主要膳食种类", getString(lifestyleStatus.get("dietType")), normalFont);
                addKeyValue(document, "三餐是否规律", getString(lifestyleStatus.get("mealsRegular")), normalFont);
                addKeyValue(document, "吸烟情况", getString(lifestyleStatus.get("smokingStatus")), normalFont);
                addKeyValue(document, "饮酒情况", getString(lifestyleStatus.get("drinkingStatus")), normalFont);
                addKeyValue(document, "睡眠情况", getString(lifestyleStatus.get("sleepStatus")), normalFont);
                document.add(new Paragraph(" ")); // 空行
            }

            // 系统疾病与症状筛查
            Map<String, Object> systemScreening = convertToMap(archiveData.get("systemDiseaseScreening"));
            if (systemScreening != null) {
                addSection(document, "系统疾病与症状筛查", headerFont, normalFont);
                addKeyValue(document, "呼吸系统症状", getString(systemScreening.get("respiratorySymptoms")), normalFont);
                addKeyValue(document, "循环系统症状", getString(systemScreening.get("circulatorySymptoms")), normalFont);
                addKeyValue(document, "消化系统症状", getString(systemScreening.get("digestiveSymptoms")), normalFont);
                document.add(new Paragraph(" ")); // 空行
            }

            // 心理评估
            Map<String, Object> psychologicalAssessment = convertToMap(archiveData.get("psychologicalAssessment"));
            if (psychologicalAssessment != null) {
                addSection(document, "心理评估", headerFont, normalFont);
                addKeyValue(document, "情绪状态", getString(psychologicalAssessment.get("emotionStatus")), normalFont);
                addKeyValue(document, "是否存在压力", getString(psychologicalAssessment.get("stressStatus")), normalFont);
                addKeyValue(document, "对自我的看法", getString(psychologicalAssessment.get("selfPerception")), normalFont);
                document.add(new Paragraph(" ")); // 空行
            }

            // 社会关系评估
            Map<String, Object> socialAssessment = convertToMap(archiveData.get("socialRelationshipAssessment"));
            if (socialAssessment != null) {
                addSection(document, "社会关系评估", headerFont, normalFont);
                addKeyValue(document, "家庭关系", getString(socialAssessment.get("familyRelationship")), normalFont);
                addKeyValue(document, "婚姻状况", getString(socialAssessment.get("maritalStatus")), normalFont);
                addKeyValue(document, "职业性质", getString(socialAssessment.get("occupationType")), normalFont);
                document.add(new Paragraph(" ")); // 空行
            }

            // 体格检查
            Map<String, Object> physicalExamination = convertToMap(archiveData.get("physicalExamination"));
            if (physicalExamination != null) {
                addSection(document, "体格检查", headerFont, normalFont);
                addKeyValue(document, "体温", getString(physicalExamination.get("temperature")) + " ℃", normalFont);
                addKeyValue(document, "脉搏", getString(physicalExamination.get("pulse")) + " 次/分", normalFont);
                addKeyValue(document, "收缩压", getString(physicalExamination.get("systolicBp")) + " mmHg", normalFont);
                addKeyValue(document, "舒张压", getString(physicalExamination.get("diastolicBp")) + " mmHg", normalFont);
                addKeyValue(document, "身高", getString(physicalExamination.get("heightCm")) + " cm", normalFont);
                addKeyValue(document, "体重", getString(physicalExamination.get("weightKg")) + " kg", normalFont);
                document.add(new Paragraph(" ")); // 空行
            }

            // 健康总结
            Map<String, Object> healthSummary = convertToMap(archiveData.get("healthSummary"));
            if (healthSummary != null) {
                addSection(document, "AI健康总结", headerFont, normalFont);
                addKeyValue(document, "健康评分", getString(healthSummary.get("overallHealthScore")) + " 分", normalFont);
                addKeyValue(document, "风险等级", getString(healthSummary.get("riskLevel")), normalFont);
                String summary = getString(healthSummary.get("analysisSummary"));
                if (summary != null && !summary.isEmpty()) {
                    Paragraph summaryPara = new Paragraph("分析摘要：" + summary, normalFont);
                    summaryPara.setSpacingAfter(10);
                    document.add(summaryPara);
                }
                document.add(new Paragraph(" ")); // 空行
            }

            // 健康建议
            Object recommendationsObj = archiveData.get("healthRecommendations");
            if (recommendationsObj != null) {
                List<Map<String, Object>> recommendations = convertListToMapList(recommendationsObj);
                if (recommendations != null && !recommendations.isEmpty()) {
                    addSection(document, "健康建议", headerFont, normalFont);
                    for (int i = 0; i < recommendations.size(); i++) {
                        Map<String, Object> rec = recommendations.get(i);
                        Paragraph recPara = new Paragraph(
                            (i + 1) + ". " + getString(rec.get("title")),
                            new Font(baseFont, 11, Font.BOLD)
                        );
                        recPara.setSpacingAfter(5);
                        document.add(recPara);

                        String content = getString(rec.get("content"));
                        if (content != null && !content.isEmpty()) {
                            Paragraph contentPara = new Paragraph(content, normalFont);
                            contentPara.setSpacingAfter(10);
                            document.add(contentPara);
                        }
                    }
                }
            }

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("生成PDF失败", e);
            if (document.isOpen()) {
                document.close();
            }
            throw e;
        }
    }

    /**
     * 添加章节标题
     */
    private static void addSection(Document document, String title, Font font, Font normalFont) throws DocumentException {
        Paragraph section = new Paragraph(title, font);
        section.setSpacingBefore(10);
        section.setSpacingAfter(10);
        document.add(section);
    }

    /**
     * 添加键值对
     */
    private static void addKeyValue(Document document, String key, String value, Font font) throws DocumentException {
        if (value == null || value.isEmpty()) {
            value = "无";
        }
        Paragraph para = new Paragraph(key + "：" + value, font);
        para.setSpacingAfter(5);
        document.add(para);
    }

    /**
     * 安全获取字符串
     */
    private static String getString(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }

    /**
     * 将对象转换为Map，支持实体对象和Map对象
     *
     * @param obj 待转换的对象
     * @return Map对象，如果obj为null则返回null
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> convertToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        try {
            // 使用ObjectMapper将实体对象转换为Map
            return objectMapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            log.warn("将对象转换为Map失败，对象类型：{}", obj.getClass().getName(), e);
            return null;
        }
    }

    /**
     * 将列表对象转换为Map列表，支持实体对象列表和Map列表
     *
     * @param obj 待转换的列表对象
     * @return Map列表，如果obj为null或转换失败则返回null
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> convertListToMapList(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (list.isEmpty()) {
                return (List<Map<String, Object>>) list;
            }
            // 检查第一个元素是否是Map
            Object first = list.get(0);
            if (first instanceof Map) {
                return (List<Map<String, Object>>) list;
            }
            // 如果是实体对象列表，需要转换
            try {
                List<Map<String, Object>> result = new java.util.ArrayList<>();
                for (Object item : list) {
                    Map<String, Object> map = convertToMap(item);
                    if (map != null) {
                        result.add(map);
                    }
                }
                return result;
            } catch (Exception e) {
                log.warn("将列表转换为Map列表失败", e);
                return null;
            }
        }
        return null;
    }
}

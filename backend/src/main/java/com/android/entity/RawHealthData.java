package com.android.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 原始健康数据表实体类
 * </p>
 *
 * @author sjt
 * @since 2026-01-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("raw_health_data")
public class RawHealthData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（关联t_user表）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 数据类型：REPORT-拍报告，MEDICAL_RECORD-就诊记录，SKIN-拍皮肤，WEARABLE-可穿戴设备，
     * LAB-检查检验，EMOTION-情绪检测，GENETIC-基因检测，SLEEP-睡眠监测，MEAL-拍三餐，EXERCISE-运动数据
     */
    @TableField("data_type")
    private String dataType;

    /**
     * 格式类型：IMAGE-图片（JPEG/PNG等），PDF-PDF文档，JSON-JSON格式数据，CSV-CSV表格数据，TEXT-纯文本
     */
    @TableField("format_type")
    private String formatType;

    /**
     * 文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 原始二进制数据（LONGBLOB）
     * 注意：MyBatis会自动处理byte[]与BLOB的转换
     */
    @TableField("raw_data")
    private byte[] rawData;

    /**
     * 上传时间
     */
    @TableField("upload_time")
    private LocalDateTime uploadTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 拍三餐：food-train 模型估算的营养成分（不落库，仅接口返回）
     */
    @TableField(exist = false)
    private Map<String, Object> mealNutrition;

    /**
     * 预测失败或未配置服务时的说明
     */
    @TableField(exist = false)
    private String mealNutritionError;
}

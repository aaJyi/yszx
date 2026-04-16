package com.android.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户推断可能疾病（智能体写入，用于资讯关键词）
 */
@Data
@TableName("user_inferred_disease")
public class UserInferredDisease implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String diseaseName;

    private BigDecimal confidence;

    /** agent / manual */
    private String sourceType;

    private Long analysisId;

    private LocalDateTime createdAt;
}

package com.android.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 器官-疾病知识条目（详述与治疗措施）
 */
@Data
@TableName("organ_disease_detail")
public class OrganDiseaseDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String organKey;

    private String organZh;

    private String diseaseName;

    private String description;

    private String treatmentMeasures;

    private String dataSource;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

package com.android.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端家人图谱：边（关系说明）
 */
@Data
@Builder
public class FamilyGraphLinkVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String source;
    private String target;
    /** 如：父亲、配偶；线上展示 */
    private String relation;
}

package com.android.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端家人图谱：节点（注册用户 / 未注册家人）
 */
@Data
@Builder
public class FamilyGraphNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 图内唯一 id，如 u_12、fm_3 */
    private String id;
    /** 展示姓名 */
    private String name;
    /** 0=注册用户 1=仅家人档案（未在小程序注册） */
    private int category;
}

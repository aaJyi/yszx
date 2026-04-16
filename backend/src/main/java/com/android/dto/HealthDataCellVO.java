package com.android.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 健康数据表格单元格：展示最近一条摘要
 */
@Data
@Builder
public class HealthDataCellVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 最近一条 */
    private RawHealthDataMetaVO latest;
    /** 同类型记录条数 */
    private int totalCount;
}

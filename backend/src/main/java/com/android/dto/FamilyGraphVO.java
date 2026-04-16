package com.android.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 管理端家人图谱：力导向图数据
 */
@Data
@Builder
public class FamilyGraphVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<FamilyGraphNodeVO> nodes;
    private List<FamilyGraphLinkVO> links;
}

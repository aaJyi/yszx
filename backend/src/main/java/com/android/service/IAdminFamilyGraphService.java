package com.android.service;

import com.android.dto.FamilyGraphVO;

/**
 * 管理端：家人关系图谱
 */
public interface IAdminFamilyGraphService {

    FamilyGraphVO buildGraph();
}

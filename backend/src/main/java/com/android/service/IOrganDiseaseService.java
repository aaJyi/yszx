package com.android.service;

import com.android.dto.OrganDiseaseBundleVO;

public interface IOrganDiseaseService {

    /**
     * 按器官 key 查询知识条目（按 sort_order）
     */
    OrganDiseaseBundleVO listByOrganKey(String organKey);
}

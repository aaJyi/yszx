package com.android.service;

import com.android.dto.HealthDataOverviewRowVO;
import com.android.dto.PageResultVO;
import com.android.dto.RawHealthDataMetaVO;

import java.util.List;

/**
 * 管理端健康数据查询
 */
public interface IAdminHealthDataQueryService {

    PageResultVO<HealthDataOverviewRowVO> pageOverview(long pageNum, long pageSize);

    List<RawHealthDataMetaVO> listRecordsByUserAndType(Long userId, String dataType);
}

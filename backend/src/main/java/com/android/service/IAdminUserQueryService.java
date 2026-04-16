package com.android.service;

import com.android.dto.AdminUserRowVO;
import com.android.dto.PageResultVO;

/**
 * 管理端用户查询
 */
public interface IAdminUserQueryService {

    PageResultVO<AdminUserRowVO> pageUsers(long pageNum, long pageSize);
}

package com.android.service.impl;

import com.android.entity.HealthFamilyHistory;
import com.android.mapper.HealthFamilyHistoryMapper;
import com.android.service.IHealthFamilyHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 家族史表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Service
public class HealthFamilyHistoryServiceImpl extends ServiceImpl<HealthFamilyHistoryMapper, HealthFamilyHistory> implements IHealthFamilyHistoryService {

}

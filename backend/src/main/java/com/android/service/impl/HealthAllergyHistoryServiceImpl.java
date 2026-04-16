package com.android.service.impl;

import com.android.entity.HealthAllergyHistory;
import com.android.mapper.HealthAllergyHistoryMapper;
import com.android.service.IHealthAllergyHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 过敏史表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Service
public class HealthAllergyHistoryServiceImpl extends ServiceImpl<HealthAllergyHistoryMapper, HealthAllergyHistory> implements IHealthAllergyHistoryService {

}

package com.android.service.impl;

import com.android.entity.HealthDiseaseHistory;
import com.android.mapper.HealthDiseaseHistoryMapper;
import com.android.service.IHealthDiseaseHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 既往史表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Service
public class HealthDiseaseHistoryServiceImpl extends ServiceImpl<HealthDiseaseHistoryMapper, HealthDiseaseHistory> implements IHealthDiseaseHistoryService {

}

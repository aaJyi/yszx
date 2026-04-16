package com.android.service.impl;

import com.android.entity.HealthPhysicalExamination;
import com.android.mapper.HealthPhysicalExaminationMapper;
import com.android.service.IHealthPhysicalExaminationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 健康档案-体格检查表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-20
 */
@Service
public class HealthPhysicalExaminationServiceImpl extends ServiceImpl<HealthPhysicalExaminationMapper, HealthPhysicalExamination> implements IHealthPhysicalExaminationService {

}

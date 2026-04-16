package com.android.service.impl;

import com.android.entity.HealthUserCertificates;
import com.android.mapper.HealthUserCertificatesMapper;
import com.android.service.IHealthUserCertificatesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 健康档案健康服务凭证表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Service
public class HealthUserCertificatesServiceImpl extends ServiceImpl<HealthUserCertificatesMapper, HealthUserCertificates> implements IHealthUserCertificatesService {

}

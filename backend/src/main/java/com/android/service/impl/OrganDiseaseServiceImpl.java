package com.android.service.impl;

import com.android.dto.OrganDiseaseBundleVO;
import com.android.dto.OrganDiseaseItemVO;
import com.android.entity.OrganDiseaseDetail;
import com.android.mapper.OrganDiseaseDetailMapper;
import com.android.service.IOrganDiseaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganDiseaseServiceImpl implements IOrganDiseaseService {

    private final OrganDiseaseDetailMapper organDiseaseDetailMapper;

    @Override
    public OrganDiseaseBundleVO listByOrganKey(String organKey) {
        if (!StringUtils.hasText(organKey)) {
            return OrganDiseaseBundleVO.builder()
                    .organKey("")
                    .organZh("")
                    .items(List.of())
                    .build();
        }
        String key = organKey.trim();
        LambdaQueryWrapper<OrganDiseaseDetail> q = new LambdaQueryWrapper<OrganDiseaseDetail>()
                .eq(OrganDiseaseDetail::getOrganKey, key)
                .orderByAsc(OrganDiseaseDetail::getSortOrder)
                .orderByAsc(OrganDiseaseDetail::getId);
        List<OrganDiseaseDetail> rows = organDiseaseDetailMapper.selectList(q);
        if (rows.isEmpty()) {
            return OrganDiseaseBundleVO.builder()
                    .organKey(key)
                    .organZh("")
                    .items(List.of())
                    .build();
        }
        String zh = rows.get(0).getOrganZh();
        List<OrganDiseaseItemVO> items = rows.stream()
                .map(r -> OrganDiseaseItemVO.builder()
                        .diseaseName(r.getDiseaseName())
                        .description(r.getDescription())
                        .treatmentMeasures(r.getTreatmentMeasures())
                        .build())
                .collect(Collectors.toList());
        return OrganDiseaseBundleVO.builder()
                .organKey(key)
                .organZh(zh != null ? zh : "")
                .items(items)
                .build();
    }
}

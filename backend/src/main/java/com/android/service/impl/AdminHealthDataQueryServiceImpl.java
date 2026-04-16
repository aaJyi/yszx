package com.android.service.impl;

import com.android.dto.HealthDataCellVO;
import com.android.dto.HealthDataOverviewRowVO;
import com.android.dto.PageResultVO;
import com.android.dto.RawHealthDataMetaVO;
import com.android.entity.RawHealthData;
import com.android.entity.TUser;
import com.android.service.IAdminHealthDataQueryService;
import com.android.service.IRawHealthDataService;
import com.android.service.ITUserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminHealthDataQueryServiceImpl implements IAdminHealthDataQueryService {

    private static final List<String> DATA_TYPES = Arrays.asList(
            "REPORT",
            "MEDICAL_RECORD",
            "SKIN",
            "LAB",
            "MEAL"
    );

    private final ITUserService tUserService;
    private final IRawHealthDataService rawHealthDataService;

    @Override
    public PageResultVO<HealthDataOverviewRowVO> pageOverview(long pageNum, long pageSize) {
        Page<TUser> p = new Page<>(Math.max(1, pageNum), Math.min(100, Math.max(1, pageSize)));
        Page<TUser> page = tUserService.page(p, Wrappers.<TUser>lambdaQuery().orderByDesc(TUser::getId));
        List<TUser> users = page.getRecords();
        if (users.isEmpty()) {
            return new PageResultVO<>(Collections.emptyList(), page.getTotal(), page.getCurrent(), page.getSize());
        }

        List<Long> userIds = users.stream().map(TUser::getId).collect(Collectors.toList());
        List<RawHealthData> all = rawHealthDataService.list(
                Wrappers.<RawHealthData>lambdaQuery()
                        .in(RawHealthData::getUserId, userIds)
                        .in(RawHealthData::getDataType, DATA_TYPES)
                        .orderByDesc(RawHealthData::getUploadTime)
        );

        Map<String, RawHealthData> latest = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (RawHealthData r : all) {
            String key = r.getUserId() + "_" + r.getDataType();
            counts.merge(key, 1, Integer::sum);
            latest.putIfAbsent(key, r);
        }

        List<HealthDataOverviewRowVO> rows = users.stream()
                .map(u -> toOverviewRow(u, latest, counts))
                .collect(Collectors.toList());
        return new PageResultVO<>(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private HealthDataOverviewRowVO toOverviewRow(TUser u, Map<String, RawHealthData> latest, Map<String, Integer> counts) {
        Long uid = u.getId();
        return HealthDataOverviewRowVO.builder()
                .userId(uid)
                .openid(u.getOpenid())
                .userName(u.getNickname() != null ? u.getNickname() : "—")
                .report(cell(uid, "REPORT", latest, counts))
                .medicalRecord(cell(uid, "MEDICAL_RECORD", latest, counts))
                .skin(cell(uid, "SKIN", latest, counts))
                .lab(cell(uid, "LAB", latest, counts))
                .meal(cell(uid, "MEAL", latest, counts))
                .build();
    }

    private HealthDataCellVO cell(Long userId, String type, Map<String, RawHealthData> latest, Map<String, Integer> counts) {
        String key = userId + "_" + type;
        RawHealthData r = latest.get(key);
        int n = counts.getOrDefault(key, 0);
        RawHealthDataMetaVO meta = r == null ? null : toMeta(r);
        return HealthDataCellVO.builder()
                .latest(meta)
                .totalCount(n)
                .build();
    }

    @Override
    public List<RawHealthDataMetaVO> listRecordsByUserAndType(Long userId, String dataType) {
        if (userId == null || dataType == null || !DATA_TYPES.contains(dataType)) {
            return Collections.emptyList();
        }
        List<RawHealthData> list = rawHealthDataService.list(
                Wrappers.<RawHealthData>lambdaQuery()
                        .eq(RawHealthData::getUserId, userId)
                        .eq(RawHealthData::getDataType, dataType)
                        .orderByDesc(RawHealthData::getUploadTime)
        );
        return list.stream().map(this::toMeta).collect(Collectors.toList());
    }

    private RawHealthDataMetaVO toMeta(RawHealthData r) {
        return RawHealthDataMetaVO.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .dataType(r.getDataType())
                .formatType(r.getFormatType())
                .fileName(r.getFileName())
                .fileSize(r.getFileSize())
                .uploadTime(r.getUploadTime())
                .build();
    }
}

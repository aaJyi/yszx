package com.android.service.impl;

import com.android.dto.SocialDiseaseItemDTO;
import com.android.dto.SocialInferredDiseaseVO;
import com.android.dto.AdminInferredDiseaseRowVO;
import com.android.entity.TUser;
import com.android.entity.UserInferredDisease;
import com.android.mapper.TUserMapper;
import com.android.mapper.UserInferredDiseaseMapper;
import com.android.service.IUserInferredDiseaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInferredDiseaseServiceImpl implements IUserInferredDiseaseService {

    private final UserInferredDiseaseMapper userInferredDiseaseMapper;
    private final TUserMapper tUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceForUser(Long userId, List<SocialDiseaseItemDTO> diseases, Long analysisId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        userInferredDiseaseMapper.delete(
                new LambdaQueryWrapper<UserInferredDisease>().eq(UserInferredDisease::getUserId, userId));
        if (diseases == null || diseases.isEmpty()) {
            return;
        }
        for (SocialDiseaseItemDTO d : diseases) {
            if (d == null || d.getDiseaseName() == null || d.getDiseaseName().trim().isEmpty()) {
                continue;
            }
            UserInferredDisease e = new UserInferredDisease();
            e.setUserId(userId);
            e.setDiseaseName(d.getDiseaseName().trim());
            e.setConfidence(d.getConfidence());
            e.setSourceType("agent");
            e.setAnalysisId(analysisId);
            e.setCreatedAt(LocalDateTime.now());
            userInferredDiseaseMapper.insert(e);
        }
    }

    @Override
    public List<SocialInferredDiseaseVO> listByUser(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<UserInferredDisease> list = userInferredDiseaseMapper.selectList(
                new LambdaQueryWrapper<UserInferredDisease>()
                        .eq(UserInferredDisease::getUserId, userId)
                        .orderByDesc(UserInferredDisease::getCreatedAt));
        return list.stream().map(r -> {
            SocialInferredDiseaseVO vo = new SocialInferredDiseaseVO();
            vo.setId(r.getId());
            vo.setDiseaseName(r.getDiseaseName());
            vo.setConfidence(r.getConfidence());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AdminInferredDiseaseRowVO> listRecentForAdmin(Integer limit) {
        int rowLimit = (limit == null || limit <= 0) ? 200 : Math.min(limit, 1000);
        List<UserInferredDisease> list = userInferredDiseaseMapper.selectList(
                new LambdaQueryWrapper<UserInferredDisease>()
                        .orderByDesc(UserInferredDisease::getCreatedAt)
                        .last("limit " + rowLimit));
        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> uidSet = list.stream().map(UserInferredDisease::getUserId).collect(Collectors.toSet());
        Map<Long, String> userNameMap = new HashMap<>();
        if (!uidSet.isEmpty()) {
            List<TUser> users = tUserMapper.selectBatchIds(uidSet);
            for (TUser u : users) {
                String name = (u.getNickname() != null && !u.getNickname().trim().isEmpty())
                        ? u.getNickname().trim()
                        : (u.getOpenid() == null ? "" : u.getOpenid());
                userNameMap.put(u.getId(), name);
            }
        }

        return list.stream().map(r -> {
            AdminInferredDiseaseRowVO vo = new AdminInferredDiseaseRowVO();
            vo.setUserId(r.getUserId());
            vo.setNickname(userNameMap.getOrDefault(r.getUserId(), ""));
            vo.setDiseaseName(r.getDiseaseName());
            vo.setConfidence(r.getConfidence());
            vo.setSourceType(r.getSourceType());
            vo.setCreatedAt(r.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }
}

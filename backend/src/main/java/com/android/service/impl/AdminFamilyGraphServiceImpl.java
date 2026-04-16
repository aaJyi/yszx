package com.android.service.impl;

import com.android.dto.FamilyGraphLinkVO;
import com.android.dto.FamilyGraphNodeVO;
import com.android.dto.FamilyGraphVO;
import com.android.entity.FamilyMember;
import com.android.entity.TUser;
import com.android.service.IAdminFamilyGraphService;
import com.android.service.IFamilyMemberService;
import com.android.service.ITUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminFamilyGraphServiceImpl implements IAdminFamilyGraphService {

    private final IFamilyMemberService familyMemberService;
    private final ITUserService tUserService;

    @Override
    public FamilyGraphVO buildGraph() {
        List<FamilyMember> members = familyMemberService.list();
        if (members.isEmpty()) {
            return FamilyGraphVO.builder()
                    .nodes(List.of())
                    .links(List.of())
                    .build();
        }

        Set<Long> userIds = new HashSet<>();
        for (FamilyMember fm : members) {
            if (fm.getOwnerUserId() != null) {
                userIds.add(fm.getOwnerUserId().longValue());
            }
            if (fm.getRegisteredUserId() != null) {
                userIds.add(fm.getRegisteredUserId().longValue());
            }
        }

        if (userIds.isEmpty()) {
            return FamilyGraphVO.builder()
                    .nodes(List.of())
                    .links(List.of())
                    .build();
        }

        Map<Long, TUser> userMap = tUserService.listByIds(new ArrayList<>(userIds)).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(TUser::getId, u -> u, (a, b) -> a));

        Map<String, FamilyGraphNodeVO> nodeMap = new LinkedHashMap<>();
        List<FamilyGraphLinkVO> links = new ArrayList<>();

        for (FamilyMember fm : members) {
            if (fm.getOwnerUserId() == null) {
                continue;
            }
            long ownerId = fm.getOwnerUserId().longValue();
            ensureUserNode(nodeMap, userMap, ownerId);

            String ownerNid = userNodeId(ownerId);
            String rel = relationLabel(fm);

            if (fm.getRegisteredUserId() != null) {
                long regId = fm.getRegisteredUserId().longValue();
                if (regId == ownerId) {
                    continue;
                }
                ensureUserNode(nodeMap, userMap, regId);
                String regNid = userNodeId(regId);
                links.add(FamilyGraphLinkVO.builder()
                        .source(ownerNid)
                        .target(regNid)
                        .relation(rel)
                        .build());
            } else {
                String fmNid = "fm_" + fm.getMemberId();
                String fname = familyDisplayName(fm);
                nodeMap.putIfAbsent(fmNid, FamilyGraphNodeVO.builder()
                        .id(fmNid)
                        .name(fname)
                        .category(1)
                        .build());
                links.add(FamilyGraphLinkVO.builder()
                        .source(ownerNid)
                        .target(fmNid)
                        .relation(rel)
                        .build());
            }
        }

        return FamilyGraphVO.builder()
                .nodes(new ArrayList<>(nodeMap.values()))
                .links(links)
                .build();
    }

    private static void ensureUserNode(Map<String, FamilyGraphNodeVO> nodeMap, Map<Long, TUser> userMap, long userId) {
        String nid = userNodeId(userId);
        nodeMap.computeIfAbsent(nid, k -> FamilyGraphNodeVO.builder()
                .id(nid)
                .name(displayName(userMap.get(userId), userId))
                .category(0)
                .build());
    }

    private static String userNodeId(long userId) {
        return "u_" + userId;
    }

    private static String displayName(TUser u, long id) {
        if (u != null && u.getNickname() != null && !u.getNickname().isBlank()) {
            return u.getNickname().trim();
        }
        if (u != null && u.getPhone() != null && !u.getPhone().isBlank()) {
            return u.getPhone().trim();
        }
        return "用户" + id;
    }

    private static String familyDisplayName(FamilyMember fm) {
        if (fm.getFullName() != null && !fm.getFullName().isBlank()) {
            return fm.getFullName().trim();
        }
        return "家人";
    }

    private static String relationLabel(FamilyMember fm) {
        if (fm.getRelation() != null && !fm.getRelation().isBlank()) {
            return fm.getRelation().trim();
        }
        return "家人";
    }
}

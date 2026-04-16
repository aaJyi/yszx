package com.android.service.impl;

import com.android.dto.SocialPeerMessageVO;
import com.android.dto.SocialPeerUserVO;
import com.android.dto.SocialRecommendUserVO;
import com.android.entity.ClubDivisionMember;
import com.android.entity.SocialPeerMessage;
import com.android.entity.SocialUserFollow;
import com.android.entity.TUser;
import com.android.entity.UserClubProfile;
import com.android.entity.UserSocialPreference;
import com.android.entity.UserInferredDisease;
import com.android.mapper.ClubDivisionMemberMapper;
import com.android.mapper.SocialPeerMessageMapper;
import com.android.mapper.SocialUserFollowMapper;
import com.android.mapper.TUserMapper;
import com.android.mapper.UserClubProfileMapper;
import com.android.mapper.UserSocialPreferenceMapper;
import com.android.mapper.UserInferredDiseaseMapper;
import com.android.service.ISocialPeerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialPeerServiceImpl implements ISocialPeerService {

    private final UserInferredDiseaseMapper userInferredDiseaseMapper;
    private final ClubDivisionMemberMapper clubDivisionMemberMapper;
    private final SocialUserFollowMapper socialUserFollowMapper;
    private final SocialPeerMessageMapper socialPeerMessageMapper;
    private final TUserMapper tUserMapper;
    private final UserClubProfileMapper userClubProfileMapper;
    private final UserSocialPreferenceMapper userSocialPreferenceMapper;
    private final ObjectMapper objectMapper;

    private Map<Long, Set<String>> loadDiseaseMap() {
        List<UserInferredDisease> all = userInferredDiseaseMapper.selectList(null);
        Map<Long, Set<String>> map = new HashMap<>();
        for (UserInferredDisease d : all) {
            if (d.getUserId() == null || d.getDiseaseName() == null || d.getDiseaseName().isBlank()) {
                continue;
            }
            map.computeIfAbsent(d.getUserId(), k -> new HashSet<>()).add(d.getDiseaseName().trim());
        }
        return map;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0;
        }
        int inter = 0;
        for (String x : a) {
            if (b.contains(x)) {
                inter++;
            }
        }
        int uni = a.size() + b.size() - inter;
        return uni == 0 ? 0 : (double) inter / uni;
    }

    /** 存在社团成员记录的最新快照（排除仅失败落库、无成员的情况） */
    private Long latestSnapshotIdWithMembers() {
        ClubDivisionMember m = clubDivisionMemberMapper.selectOne(
                new LambdaQueryWrapper<ClubDivisionMember>()
                        .orderByDesc(ClubDivisionMember::getSnapshotId)
                        .last("LIMIT 1"));
        return m == null ? null : m.getSnapshotId();
    }

    @Override
    public Map<String, Object> listRecommendations(Long userId, Integer page, Integer pageSize) {
        if (userId == null) {
            return pageResult(Collections.emptyList(), 0, 1, 20);
        }
        int p = page == null || page < 1 ? 1 : page;
        int ps = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);

        UserSocialPreference myPref = getOrCreatePreference(userId);
        if (Boolean.FALSE.equals(myPref.getAllowRecommend())) {
            return pageResult(Collections.emptyList(), 0, p, ps);
        }

        Map<Long, Set<String>> diseaseMap = loadDiseaseMap();
        Set<String> mine = diseaseMap.getOrDefault(userId, Collections.emptySet());

        Long snapId = latestSnapshotIdWithMembers();
        Integer myClub = null;
        if (snapId != null) {
            ClubDivisionMember row = clubDivisionMemberMapper.selectOne(
                    new LambdaQueryWrapper<ClubDivisionMember>()
                            .eq(ClubDivisionMember::getSnapshotId, snapId)
                            .eq(ClubDivisionMember::getUserId, userId)
                            .last("LIMIT 1"));
            if (row != null) {
                myClub = row.getClubIndex();
            }
        }

        Set<Long> sameClubCandidates = new LinkedHashSet<>();
        Set<Long> nearClubCandidates = new LinkedHashSet<>();
        if (snapId != null && myClub != null) {
            List<ClubDivisionMember> allMembers = clubDivisionMemberMapper.selectList(
                    new LambdaQueryWrapper<ClubDivisionMember>()
                            .eq(ClubDivisionMember::getSnapshotId, snapId));
            for (ClubDivisionMember m : allMembers) {
                if (m.getUserId().equals(userId)) continue;
                if (Objects.equals(m.getClubIndex(), myClub)) {
                    sameClubCandidates.add(m.getUserId());
                } else {
                    nearClubCandidates.add(m.getUserId());
                }
            }
        }

        if (sameClubCandidates.isEmpty() && nearClubCandidates.isEmpty()) {
            for (Long uid : diseaseMap.keySet()) {
                if (!uid.equals(userId)) {
                    nearClubCandidates.add(uid);
                }
            }
        }

        Map<Long, List<Double>> vectors = loadProfileVectors();
        Map<Long, UserSocialPreference> prefMap = loadPreferences();
        Map<Long, Map<String, Object>> featureMap = loadFeatureMap();
        List<Double> myVec = vectors.get(userId);
        Map<String, Object> myFeatures = featureMap.getOrDefault(userId, Collections.emptyMap());

        List<SocialRecommendUserVO> out = new ArrayList<>();
        for (Long uid : sameClubCandidates) {
            UserSocialPreference otherPref = prefMap.get(uid);
            if (!isCandidateAllowed(myPref, otherPref, uid)) continue;
            Set<String> other = diseaseMap.getOrDefault(uid, Collections.emptySet());
            if (Boolean.FALSE.equals(myPref.getAllowDiseaseBasedMatch()) || (otherPref != null && Boolean.FALSE.equals(otherPref.getAllowDiseaseBasedMatch()))) {
                // 用户关闭了疾病匹配时，疾病相似度降权到0，但仍可走行为/阶段匹配
                other = Collections.emptySet();
            }
            double jac = jaccard(mine, other);
            double profileSim = cosine(myVec, vectors.get(uid));
            String matchType = pickMatchType(myFeatures, featureMap.get(uid), jac);
            if (!passHardConstraints(myFeatures, featureMap.get(uid), matchType)) continue;
            SocialRecommendUserVO vo = buildRecommendVo(userId, uid, diseaseMap, snapId, jac, profileSim, matchType);
            out.add(vo);
        }

        List<SocialRecommendUserVO> near = new ArrayList<>();
        for (Long uid : nearClubCandidates) {
            UserSocialPreference otherPref = prefMap.get(uid);
            if (!isCandidateAllowed(myPref, otherPref, uid)) continue;
            Set<String> other = diseaseMap.getOrDefault(uid, Collections.emptySet());
            if (Boolean.FALSE.equals(myPref.getAllowDiseaseBasedMatch()) || (otherPref != null && Boolean.FALSE.equals(otherPref.getAllowDiseaseBasedMatch()))) {
                other = Collections.emptySet();
            }
            double jac = jaccard(mine, other);
            double profileSim = cosine(myVec, vectors.get(uid));
            String matchType = pickMatchType(myFeatures, featureMap.get(uid), jac);
            if (!passHardConstraints(myFeatures, featureMap.get(uid), matchType)) continue;
            near.add(buildRecommendVo(userId, uid, diseaseMap, snapId, jac, profileSim, matchType));
        }
        near.sort(Comparator.comparing(SocialRecommendUserVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));

        out.sort(Comparator.comparing(SocialRecommendUserVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));

        // 70% 同社团 + 30% 近社团
        int limit = 80;
        int sameTarget = (int) Math.ceil(limit * 0.7);
        List<SocialRecommendUserVO> merged = new ArrayList<>();
        merged.addAll(out.stream().limit(sameTarget).collect(Collectors.toList()));
        int rest = limit - merged.size();
        if (rest > 0) {
            merged.addAll(near.stream().limit(rest).collect(Collectors.toList()));
        }

        // 若仍不足，补齐其他候选（按综合分）
        if (merged.size() < limit) {
            Set<Long> used = merged.stream().map(SocialRecommendUserVO::getUserId).collect(Collectors.toSet());
            List<SocialRecommendUserVO> others = near.stream()
                    .filter(v -> !used.contains(v.getUserId()))
                    .sorted(Comparator.comparing(SocialRecommendUserVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
            for (SocialRecommendUserVO v : others) {
                if (merged.size() >= limit) break;
                merged.add(v);
            }
        }
        int total = merged.size();
        int from = Math.min((p - 1) * ps, total);
        int to = Math.min(from + ps, total);
        List<SocialRecommendUserVO> pageList = merged.subList(from, to);
        // 理由模板文案
        pageList.forEach(v -> v.setMatchType(v.getMatchType() + "：" + buildReasonText(v)));
        return pageResult(pageList, total, p, ps);
    }

    private Map<String, Object> pageResult(List<SocialRecommendUserVO> list, int total, int page, int pageSize) {
        Map<String, Object> m = new HashMap<>();
        m.put("list", list);
        m.put("total", total);
        m.put("page", page);
        m.put("pageSize", pageSize);
        return m;
    }

    private SocialRecommendUserVO buildRecommendVo(Long me, Long uid, Map<Long, Set<String>> diseaseMap,
                                                   Long snapId, double jac, double profileSim, String matchType) {
        TUser u = tUserMapper.selectById(uid);
        SocialRecommendUserVO vo = new SocialRecommendUserVO();
        vo.setUserId(uid);
        if (u != null) {
            vo.setNickname(u.getNickname());
            vo.setAvatarUrl(u.getAvatarUrl());
        }
        vo.setDiseaseSimilarity(jac);
        vo.setProfileSimilarity(profileSim);
        vo.setMatchScore(jac * 0.6 + profileSim * 0.4);
        vo.setMatchType(matchType);
        Set<String> a = diseaseMap.getOrDefault(me, Collections.emptySet());
        Set<String> b = diseaseMap.getOrDefault(uid, Collections.emptySet());
        List<String> shared = new ArrayList<>();
        for (String x : a) {
            if (b.contains(x)) {
                shared.add(x);
            }
        }
        vo.setSharedDiseaseHints(shared.stream().limit(5).collect(Collectors.toList()));

        if (snapId != null) {
            ClubDivisionMember m = clubDivisionMemberMapper.selectOne(
                    new LambdaQueryWrapper<ClubDivisionMember>()
                            .eq(ClubDivisionMember::getSnapshotId, snapId)
                            .eq(ClubDivisionMember::getUserId, uid)
                            .last("LIMIT 1"));
            if (m != null) {
                vo.setClubIndex(m.getClubIndex());
            }
        }

        boolean iFollow = socialUserFollowMapper.selectCount(
                new LambdaQueryWrapper<SocialUserFollow>()
                        .eq(SocialUserFollow::getFollowerUserId, me)
                        .eq(SocialUserFollow::getFolloweeUserId, uid)) > 0;
        boolean theyFollow = socialUserFollowMapper.selectCount(
                new LambdaQueryWrapper<SocialUserFollow>()
                        .eq(SocialUserFollow::getFollowerUserId, uid)
                        .eq(SocialUserFollow::getFolloweeUserId, me)) > 0;
        vo.setFollowed(iFollow);
        vo.setFollowsMe(theyFollow);
        vo.setMutualFollow(iFollow && theyFollow);
        return vo;
    }

    private Map<Long, List<Double>> loadProfileVectors() {
        Map<Long, List<Double>> map = new HashMap<>();
        List<UserClubProfile> all = userClubProfileMapper.selectList(null);
        for (UserClubProfile p : all) {
            if (p.getUserId() == null || p.getVectorJson() == null || p.getVectorJson().isBlank()) continue;
            try {
                List<Double> vec = objectMapper.readValue(p.getVectorJson(), new TypeReference<List<Double>>() {});
                map.put(p.getUserId(), vec);
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private Map<Long, UserSocialPreference> loadPreferences() {
        Map<Long, UserSocialPreference> map = new HashMap<>();
        List<UserSocialPreference> all = userSocialPreferenceMapper.selectList(null);
        for (UserSocialPreference p : all) {
            if (p.getUserId() == null) continue;
            map.put(p.getUserId(), p);
        }
        return map;
    }

    private Map<Long, Map<String, Object>> loadFeatureMap() {
        Map<Long, Map<String, Object>> map = new HashMap<>();
        List<UserClubProfile> all = userClubProfileMapper.selectList(null);
        for (UserClubProfile p : all) {
            if (p.getUserId() == null || p.getFeatureJson() == null || p.getFeatureJson().isBlank()) continue;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> feat = objectMapper.readValue(p.getFeatureJson(), Map.class);
                map.put(p.getUserId(), feat);
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private boolean isCandidateAllowed(UserSocialPreference me, UserSocialPreference other, Long uid) {
        if (other == null) other = getOrCreatePreference(uid);
        if (Boolean.FALSE.equals(other.getAllowRecommend())) return false;
        if (me != null && me.getBlockedUserIds() != null && me.getBlockedUserIds().contains(String.valueOf(uid))) return false;
        return true;
    }

    private String pickMatchType(Map<String, Object> mine, Map<String, Object> other, double jac) {
        if (jac >= 0.45) return "强匹配";
        String ms = getStr(mine, "stageNeed", "careStage");
        String os = getStr(other, "stageNeed", "careStage");
        if (ms != null && os != null && !ms.equals(os)) return "经验互补";
        return "行为陪伴";
    }

    private boolean passHardConstraints(Map<String, Object> mine, Map<String, Object> other, String matchType) {
        if ("经验互补".equals(matchType)) {
            // 真实约束：必须同目标且阶段不同
            List<String> myGoals = getList(mine, "stageNeed", "currentGoals");
            List<String> otGoals = getList(other, "stageNeed", "currentGoals");
            if (Collections.disjoint(myGoals, otGoals)) return false;
            String ms = getStr(mine, "stageNeed", "careStage");
            String os = getStr(other, "stageNeed", "careStage");
            return ms != null && os != null && !ms.equals(os);
        }
        if ("行为陪伴".equals(matchType)) {
            // 真实约束：作息窗口一致且运动等级差距不大
            String mt = getStr(mine, "socialTalkability", "activeTimeWindow");
            String ot = getStr(other, "socialTalkability", "activeTimeWindow");
            if (mt != null && ot != null && !mt.equals(ot)) return false;
            Double meEx = getDouble(mine, "behavior", "exerciseLevel");
            Double otEx = getDouble(other, "behavior", "exerciseLevel");
            if (meEx != null && otEx != null && Math.abs(meEx - otEx) > 3.0) return false;
        }
        return true;
    }

    private String buildReasonText(SocialRecommendUserVO v) {
        String sim = v.getDiseaseSimilarity() != null ? String.format("疾病相似度%.0f%%", v.getDiseaseSimilarity() * 100) : "行为画像匹配";
        String p = v.getProfileSimilarity() != null ? String.format("画像匹配%.0f%%", v.getProfileSimilarity() * 100) : "画像数据不足";
        return sim + "，" + p;
    }

    private String getStr(Map<String, Object> root, String sec, String key) {
        if (root == null) return null;
        Object obj = root.get(sec);
        if (!(obj instanceof Map<?, ?> m)) return null;
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private List<String> getList(Map<String, Object> root, String sec, String key) {
        if (root == null) return Collections.emptyList();
        Object obj = root.get(sec);
        if (!(obj instanceof Map<?, ?> m)) return Collections.emptyList();
        Object v = m.get(key);
        if (!(v instanceof List<?> list)) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (Object o : list) if (o != null) out.add(String.valueOf(o));
        return out;
    }

    private Double getDouble(Map<String, Object> root, String sec, String key) {
        if (root == null) return null;
        Object obj = root.get(sec);
        if (!(obj instanceof Map<?, ?> m)) return null;
        Object v = m.get(key);
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }

    @Override
    public UserSocialPreference getOrCreatePreference(Long userId) {
        if (userId == null) return null;
        UserSocialPreference one = userSocialPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserSocialPreference>().eq(UserSocialPreference::getUserId, userId).last("LIMIT 1"));
        if (one != null) return one;
        UserSocialPreference p = new UserSocialPreference();
        p.setUserId(userId);
        p.setAllowRecommend(true);
        p.setAllowDiseaseBasedMatch(true);
        p.setActiveTimeWindow("19:00-22:00");
        p.setReplyStyle("normal");
        p.setBlockedUserIds("");
        p.setUpdatedAt(LocalDateTime.now());
        userSocialPreferenceMapper.insert(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePreference(UserSocialPreference pref) {
        UserSocialPreference old = getOrCreatePreference(pref.getUserId());
        old.setAllowRecommend(pref.getAllowRecommend() != null ? pref.getAllowRecommend() : old.getAllowRecommend());
        old.setAllowDiseaseBasedMatch(pref.getAllowDiseaseBasedMatch() != null ? pref.getAllowDiseaseBasedMatch() : old.getAllowDiseaseBasedMatch());
        if (pref.getActiveTimeWindow() != null) old.setActiveTimeWindow(pref.getActiveTimeWindow());
        if (pref.getReplyStyle() != null) old.setReplyStyle(pref.getReplyStyle());
        if (pref.getBlockedUserIds() != null) old.setBlockedUserIds(pref.getBlockedUserIds());
        old.setUpdatedAt(LocalDateTime.now());
        userSocialPreferenceMapper.updateById(old);
    }

    private static double cosine(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0d;
        int n = Math.min(a.size(), b.size());
        double dot = 0d, na = 0d, nb = 0d;
        for (int i = 0; i < n; i++) {
            double x = a.get(i) == null ? 0d : a.get(i);
            double y = b.get(i) == null ? 0d : b.get(i);
            dot += x * y;
            na += x * x;
            nb += y * y;
        }
        if (na <= 0 || nb <= 0) return 0d;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long followerUserId, Long followeeUserId) {
        if (followerUserId == null || followeeUserId == null || followerUserId.equals(followeeUserId)) {
            throw new IllegalArgumentException("参数无效");
        }
        long c = socialUserFollowMapper.selectCount(
                new LambdaQueryWrapper<SocialUserFollow>()
                        .eq(SocialUserFollow::getFollowerUserId, followerUserId)
                        .eq(SocialUserFollow::getFolloweeUserId, followeeUserId));
        if (c > 0) {
            return;
        }
        SocialUserFollow f = new SocialUserFollow();
        f.setFollowerUserId(followerUserId);
        f.setFolloweeUserId(followeeUserId);
        f.setCreatedAt(LocalDateTime.now());
        socialUserFollowMapper.insert(f);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long followerUserId, Long followeeUserId) {
        if (followerUserId == null || followeeUserId == null) {
            return;
        }
        socialUserFollowMapper.delete(
                new LambdaQueryWrapper<SocialUserFollow>()
                        .eq(SocialUserFollow::getFollowerUserId, followerUserId)
                        .eq(SocialUserFollow::getFolloweeUserId, followeeUserId));
    }

    @Override
    public boolean isMutualFollow(Long a, Long b) {
        if (a == null || b == null) {
            return false;
        }
        long c1 = socialUserFollowMapper.selectCount(
                new LambdaQueryWrapper<SocialUserFollow>()
                        .eq(SocialUserFollow::getFollowerUserId, a)
                        .eq(SocialUserFollow::getFolloweeUserId, b));
        long c2 = socialUserFollowMapper.selectCount(
                new LambdaQueryWrapper<SocialUserFollow>()
                        .eq(SocialUserFollow::getFollowerUserId, b)
                        .eq(SocialUserFollow::getFolloweeUserId, a));
        return c1 > 0 && c2 > 0;
    }

    @Override
    public List<SocialPeerUserVO> listMutualPartners(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<SocialUserFollow> outs = socialUserFollowMapper.selectList(
                new LambdaQueryWrapper<SocialUserFollow>()
                        .eq(SocialUserFollow::getFollowerUserId, userId));
        List<SocialPeerUserVO> list = new ArrayList<>();
        for (SocialUserFollow o : outs) {
            Long peer = o.getFolloweeUserId();
            if (isMutualFollow(userId, peer)) {
                TUser u = tUserMapper.selectById(peer);
                SocialPeerUserVO vo = new SocialPeerUserVO();
                vo.setUserId(peer);
                if (u != null) {
                    vo.setNickname(u.getNickname());
                    vo.setAvatarUrl(u.getAvatarUrl());
                }
                list.add(vo);
            }
        }
        return list;
    }

    @Override
    public List<SocialPeerMessageVO> listConversation(Long userId, Long peerId, Long sinceId, int limit) {
        if (userId == null || peerId == null) {
            return Collections.emptyList();
        }
        int lim = Math.min(Math.max(limit, 1), 200);
        LambdaQueryWrapper<SocialPeerMessage> w = new LambdaQueryWrapper<>();
        w.and(q -> q.and(a -> a.eq(SocialPeerMessage::getFromUserId, userId).eq(SocialPeerMessage::getToUserId, peerId))
                .or(b -> b.eq(SocialPeerMessage::getFromUserId, peerId).eq(SocialPeerMessage::getToUserId, userId)));
        if (sinceId != null && sinceId > 0) {
            w.gt(SocialPeerMessage::getId, sinceId);
        }
        w.orderByAsc(SocialPeerMessage::getId);
        w.last("LIMIT " + lim);
        List<SocialPeerMessage> rows = socialPeerMessageMapper.selectList(w);
        List<SocialPeerMessageVO> vos = new ArrayList<>();
        for (SocialPeerMessage m : rows) {
            SocialPeerMessageVO vo = new SocialPeerMessageVO();
            BeanUtils.copyProperties(m, vo);
            vos.add(vo);
        }
        return vos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(Long fromUserId, Long toUserId, String content) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("参数无效");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("内容不能为空");
        }
        String text = content.trim();
        if (text.length() > 2000) {
            throw new IllegalArgumentException("内容过长");
        }
        if (!isMutualFollow(fromUserId, toUserId)) {
            throw new IllegalStateException("仅互相关注的病友可发消息");
        }
        SocialPeerMessage m = new SocialPeerMessage();
        m.setFromUserId(fromUserId);
        m.setToUserId(toUserId);
        m.setContent(text);
        m.setCreatedAt(LocalDateTime.now());
        socialPeerMessageMapper.insert(m);
    }
}

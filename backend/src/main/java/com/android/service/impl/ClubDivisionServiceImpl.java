package com.android.service.impl;

import com.android.dto.ClubDivisionAdminOverviewVO;
import com.android.dto.ClubDivisionImgManifestVO;
import com.android.dto.ClubDivisionMemberAdminVO;
import com.android.entity.ClubDivisionMember;
import com.android.entity.ClubDivisionSnapshot;
import com.android.entity.TUser;
import com.android.entity.UserClubProfile;
import com.android.entity.UserInferredDisease;
import com.android.mapper.ClubDivisionMemberMapper;
import com.android.mapper.ClubDivisionSnapshotMapper;
import com.android.mapper.TUserMapper;
import com.android.mapper.UserClubProfileMapper;
import com.android.mapper.UserInferredDiseaseMapper;
import com.android.service.IClubDivisionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ClubDivisionServiceImpl implements IClubDivisionService {

    /** 子进程 stdout/stderr 需在 waitFor 同时被消费，否则管道缓冲区满会导致 Python 侧 print 永久阻塞。 */
    private static byte[] readProcessStreamQuietly(InputStream in) {
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private final UserInferredDiseaseMapper userInferredDiseaseMapper;
    private final ClubDivisionSnapshotMapper clubDivisionSnapshotMapper;
    private final ClubDivisionMemberMapper clubDivisionMemberMapper;
    private final TUserMapper tUserMapper;
    private final UserClubProfileMapper userClubProfileMapper;
    private final ObjectMapper objectMapper;

    @Value("${club.division.python.executable:python}")
    private String pythonExecutable;

    @Value("${club.division.script.dir:}")
    private String scriptDir;

    @Value("${club.division.img.dir:}")
    private String imgDir;

    @Value("${club.division.img.public.base:/club-division-img}")
    private String imgPublicBase;

    @Value("${club.division.disease.weight:0.82}")
    private double diseaseWeight;

    /** 与 Python 默认一致：略提高以使建边更稀疏、社团更易分开 */
    @Value("${club.division.similarity.threshold.default:0.45}")
    private double defaultSimilarityThreshold;

    /** 过小社团合并：0 表示不按人数并入大社（原 0.05 在 N=200 时约 10 人） */
    @Value("${club.division.min.community.ratio:0}")
    private double minCommunityRatio;

    /**
     * 若为正数，优先于 ratio：成员数低于该值的社团并入最大社（例如 100）。
     * 与「尽量划分开」相矛盾时请保持 0。
     */
    @Value("${club.division.min.community.abs:0}")
    private int minCommunityAbs;

    @Value("${club.division.ncss.dependency.threshold:0.55}")
    private double ncssDependencyThreshold;

    @Value("${club.division.process.timeout.seconds:600}")
    private int processTimeoutSeconds;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recompute(BigDecimal similarityThreshold) throws Exception {
        if (similarityThreshold == null) {
            similarityThreshold = BigDecimal.valueOf(defaultSimilarityThreshold);
        }
        Map<Long, Set<String>> byUser = loadUserDiseasesMap();
        Map<String, List<String>> forJson = new LinkedHashMap<>();
        for (Map.Entry<Long, Set<String>> e : byUser.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }
            forJson.put(String.valueOf(e.getKey()), new ArrayList<>(e.getValue()));
        }
        if (forJson.size() < 2) {
            ClubDivisionSnapshot snap = new ClubDivisionSnapshot();
            snap.setSimilarityThreshold(similarityThreshold);
            snap.setUserCount(forJson.size());
            snap.setClubCount(0);
            snap.setResultJson("{\"ok\":false,\"error\":\"参与计算的用户数不足（至少需要2名用户且有推断疾病）\"}");
            snap.setErrorMessage("用户数不足");
            snap.setCreatedAt(LocalDateTime.now());
            clubDivisionSnapshotMapper.insert(snap);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("similarity_threshold", similarityThreshold.doubleValue());
        payload.put("user_diseases", forJson);
        payload.put("user_vectors", loadUserVectorsMap(forJson.keySet()));
        payload.put("disease_weight", diseaseWeight);
        payload.put("min_community_ratio", minCommunityRatio);
        if (minCommunityAbs > 0) {
            payload.put("min_community_abs", minCommunityAbs);
        }
        payload.put("dependency_threshold", ncssDependencyThreshold);
        String stdinJson = objectMapper.writeValueAsString(payload);

        if (scriptDir == null || scriptDir.isBlank()) {
            throw new IllegalStateException("未配置 club.division.script.dir，无法调用 NCSS 脚本");
        }
        File workDir = new File(scriptDir);
        File script = new File(workDir, "run_ncss_json.py");
        if (!script.isFile()) {
            throw new IllegalStateException("找不到脚本: " + script.getAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder(pythonExecutable, script.getName());
        pb.directory(workDir);
        pb.redirectErrorStream(false);
        Process p = pb.start();
        CompletableFuture<byte[]> stdoutFuture =
                CompletableFuture.supplyAsync(() -> readProcessStreamQuietly(p.getInputStream()));
        CompletableFuture<byte[]> stderrFuture =
                CompletableFuture.supplyAsync(() -> readProcessStreamQuietly(p.getErrorStream()));
        p.getOutputStream().write(stdinJson.getBytes(StandardCharsets.UTF_8));
        p.getOutputStream().close();
        boolean finished = p.waitFor(Math.max(processTimeoutSeconds, 60), TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            stdoutFuture.cancel(true);
            stderrFuture.cancel(true);
            throw new IllegalStateException("NCSS 脚本执行超时");
        }
        byte[] outBytes = stdoutFuture.join();
        byte[] errBytes = stderrFuture.join();
        String stdout = new String(outBytes, StandardCharsets.UTF_8).trim();
        if (p.exitValue() != 0) {
            throw new IllegalStateException("NCSS 退出码=" + p.exitValue() + " stderr=" + new String(errBytes, StandardCharsets.UTF_8));
        }

        JsonNode root = objectMapper.readTree(stdout);
        ClubDivisionSnapshot snap = new ClubDivisionSnapshot();
        snap.setSimilarityThreshold(similarityThreshold);
        snap.setCreatedAt(LocalDateTime.now());
        snap.setResultJson(stdout);

        if (!root.path("ok").asBoolean(false)) {
            snap.setUserCount(0);
            snap.setClubCount(0);
            snap.setErrorMessage(root.path("error").asText("NCSS 失败"));
            clubDivisionSnapshotMapper.insert(snap);
            return;
        }

        snap.setUserCount(root.path("user_count").asInt(0));
        snap.setClubCount(root.path("club_count").asInt(0));
        snap.setErrorMessage(null);
        clubDivisionSnapshotMapper.insert(snap);
        Long snapshotId = snap.getId();

        JsonNode users = root.path("users");
        JsonNode centrality = root.path("centrality");
        Map<String, BigDecimal> cenByUser = new HashMap<>();
        for (int i = 0; i < users.size() && i < centrality.size(); i++) {
            String uid = users.get(i).asText();
            cenByUser.put(uid, BigDecimal.valueOf(centrality.get(i).asDouble()));
        }

        JsonNode communities = root.path("communities");
        for (int ci = 0; ci < communities.size(); ci++) {
            JsonNode comm = communities.get(ci);
            for (JsonNode uidNode : comm) {
                long uid = Long.parseLong(uidNode.asText());
                ClubDivisionMember m = new ClubDivisionMember();
                m.setSnapshotId(snapshotId);
                m.setUserId(uid);
                m.setClubIndex(ci);
                m.setCentrality(cenByUser.get(String.valueOf(uid)));
                clubDivisionMemberMapper.insert(m);
            }
        }

    }

    @Override
    public ClubDivisionImgManifestVO getImgManifest() {
        ClubDivisionImgManifestVO vo = new ClubDivisionImgManifestVO();
        vo.setBaseUrl(imgPublicBase.endsWith("/") ? imgPublicBase.substring(0, imgPublicBase.length() - 1) : imgPublicBase);
        vo.setFiles(Collections.emptyList());
        vo.setUpdatedAtMs(0L);
        if (imgDir == null || imgDir.isBlank()) {
            return vo;
        }
        File dir = new File(imgDir);
        if (!dir.isDirectory()) {
            return vo;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        if (files == null || files.length == 0) {
            return vo;
        }
        long maxMtime = 0;
        for (File f : files) {
            maxMtime = Math.max(maxMtime, f.lastModified());
        }
        vo.setUpdatedAtMs(maxMtime);
        List<String> names = new ArrayList<>();
        for (File f : files) {
            names.add(f.getName());
        }
        names.sort((a, b) -> {
            int pa = manifestFilePriority(a);
            int pb = manifestFilePriority(b);
            if (pa != pb) {
                return Integer.compare(pa, pb);
            }
            return a.compareTo(b);
        });
        vo.setFiles(names);
        return vo;
    }

    private Map<Long, Set<String>> loadUserDiseasesMap() {
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

    private Map<String, List<Double>> loadUserVectorsMap(Set<String> userIds) {
        Map<String, List<Double>> out = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return out;
        }
        List<UserClubProfile> all = userClubProfileMapper.selectList(null);
        for (UserClubProfile p : all) {
            if (p.getUserId() == null) continue;
            String uid = String.valueOf(p.getUserId());
            if (!userIds.contains(uid)) continue;
            if (p.getVectorJson() == null || p.getVectorJson().isBlank()) continue;
            try {
                @SuppressWarnings("unchecked")
                List<Number> nums = objectMapper.readValue(p.getVectorJson(), List.class);
                List<Double> vec = new ArrayList<>();
                for (Number n : nums) vec.add(n == null ? 0d : n.doubleValue());
                out.put(uid, vec);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    @Override
    public ClubDivisionAdminOverviewVO getLatestOverview() {
        ClubDivisionSnapshot snap = clubDivisionSnapshotMapper.selectOne(
                new LambdaQueryWrapper<ClubDivisionSnapshot>()
                        .orderByDesc(ClubDivisionSnapshot::getId)
                        .last("LIMIT 1"));
        if (snap == null) {
            return null;
        }
        ClubDivisionAdminOverviewVO vo = new ClubDivisionAdminOverviewVO();
        vo.setSnapshotId(snap.getId());
        vo.setCreatedAt(snap.getCreatedAt());
        vo.setSimilarityThreshold(snap.getSimilarityThreshold() != null ? snap.getSimilarityThreshold().doubleValue() : null);
        vo.setUserCount(snap.getUserCount());
        vo.setClubCount(snap.getClubCount());
        try {
            JsonNode root = objectMapper.readTree(snap.getResultJson());
            JsonNode seeds = root.path("seeds");
            List<String> seedList = new ArrayList<>();
            seeds.forEach(n -> seedList.add(n.asText()));
            vo.setSeeds(seedList);

            Map<String, Integer> clubOfUser = new HashMap<>();
            LambdaQueryWrapper<ClubDivisionMember> mq = new LambdaQueryWrapper<ClubDivisionMember>()
                    .eq(ClubDivisionMember::getSnapshotId, snap.getId());
            for (ClubDivisionMember m : clubDivisionMemberMapper.selectList(mq)) {
                clubOfUser.put(String.valueOf(m.getUserId()), m.getClubIndex());
            }

            JsonNode users = root.path("users");
            List<Map<String, Object>> nodes = new ArrayList<>();
            JsonNode cen = root.path("centrality");
            for (int i = 0; i < users.size(); i++) {
                String uid = users.get(i).asText();
                TUser u = tUserMapper.selectById(Long.parseLong(uid));
                double c = i < cen.size() ? cen.get(i).asDouble() : 0;
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", uid);
                node.put("name", u != null && u.getNickname() != null ? u.getNickname() : ("用户" + uid));
                node.put("category", clubOfUser.getOrDefault(uid, 0));
                node.put("centrality", c);
                node.put("symbolSize", Math.max(8, 8 + c * 1200));
                nodes.add(node);
            }
            vo.setGraphNodes(nodes);

            List<Map<String, Object>> links = new ArrayList<>();
            JsonNode edges = root.path("edges");
            for (JsonNode e : edges) {
                Map<String, Object> link = new LinkedHashMap<>();
                link.put("source", e.path("source").asText());
                link.put("target", e.path("target").asText());
                link.put("value", e.path("similarity").asDouble());
                link.put("commonNeighbors", e.path("commonNeighbors").asInt(0));
                links.add(link);
            }
            vo.setGraphLinks(links);

            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("edgeCount", links.size());
            metrics.put("seedCount", seedList.size());
            JsonNode degrees = root.path("adjacency_degrees");
            if (degrees.isArray() && degrees.size() > 0) {
                double sum = 0;
                for (JsonNode d : degrees) {
                    sum += d.asInt();
                }
                metrics.put("avgDegree", sum / degrees.size());
            }
            metrics.put("dependencyThreshold", 0.5);
            metrics.put("dampingFactor", 0.8);

            JsonNode communitiesNode = root.path("communities");
            List<Integer> clubSizes = new ArrayList<>();
            if (communitiesNode.isArray()) {
                for (JsonNode c : communitiesNode) {
                    clubSizes.add(c.size());
                }
            }
            vo.setClubSizes(clubSizes);

            int nNodes = users.size();
            int nEdges = links.size();
            int nClubs = clubSizes.size();
            int maxSz = clubSizes.isEmpty() ? 0 : clubSizes.stream().mapToInt(Integer::intValue).max().orElse(0);
            int minSz = clubSizes.isEmpty() ? 0 : clubSizes.stream().mapToInt(Integer::intValue).min().orElse(0);
            double avgSz = clubSizes.isEmpty() ? 0 : clubSizes.stream().mapToInt(Integer::intValue).average().orElse(0);
            double density = nNodes > 1 ? (2.0 * nEdges) / (nNodes * (nNodes - 1)) : 0;
            Map<String, Object> dash = new LinkedHashMap<>();
            dash.put("totalNodes", nNodes);
            dash.put("totalEdges", nEdges);
            dash.put("totalClubs", nClubs);
            dash.put("maxClubSize", maxSz);
            dash.put("minClubSize", minSz);
            dash.put("avgClubSize", Math.round(avgSz * 10) / 10.0);
            dash.put("graphDensity", Math.round(density * 1000) / 1000.0);
            vo.setDashboardStats(dash);

            /* 疾病热力图：用 user_inferred_disease 表中的疾病名，避免 NCSS JSON 内 disease_sets 编码异常导致乱码 */
            if (!clubOfUser.isEmpty()) {
                Map<Long, Set<String>> diseaseByUser = loadUserDiseasesMap();
                TreeSet<String> allDiseases = new TreeSet<>();
                for (String uidStr : clubOfUser.keySet()) {
                    long uid;
                    try {
                        uid = Long.parseLong(uidStr);
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                    Set<String> ds = diseaseByUser.get(uid);
                    if (ds == null) {
                        continue;
                    }
                    for (String dn : ds) {
                        if (dn != null && !dn.isBlank()) {
                            allDiseases.add(dn.trim());
                        }
                    }
                }
                int maxClubIdx = clubOfUser.values().stream().mapToInt(Integer::intValue).max().orElse(0);
                int clubCols = maxClubIdx + 1;
                List<String> labels = new ArrayList<>(allDiseases);
                List<List<Integer>> matrix = new ArrayList<>();
                for (int i = 0; i < labels.size(); i++) {
                    matrix.add(new ArrayList<>(Collections.nCopies(clubCols, 0)));
                }
                Map<String, Integer> disIndex = new HashMap<>();
                for (int i = 0; i < labels.size(); i++) {
                    disIndex.put(labels.get(i), i);
                }
                for (Map.Entry<String, Integer> e : clubOfUser.entrySet()) {
                    String uidStr = e.getKey();
                    Integer ci = e.getValue();
                    if (ci == null || ci < 0 || ci >= clubCols) {
                        continue;
                    }
                    long uid;
                    try {
                        uid = Long.parseLong(uidStr);
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                    Set<String> ds = diseaseByUser.get(uid);
                    if (ds == null) {
                        continue;
                    }
                    for (String name : ds) {
                        if (name == null || name.isBlank()) {
                            continue;
                        }
                        name = name.trim();
                        Integer di = disIndex.get(name);
                        if (di != null) {
                            List<Integer> row = matrix.get(di);
                            row.set(ci, row.get(ci) + 1);
                        }
                    }
                }
                vo.setHeatmapDiseaseLabels(labels);
                vo.setHeatmapMatrix(matrix);
                Map<String, List<String>> uidToDis = new LinkedHashMap<>();
                for (String uidStr : clubOfUser.keySet()) {
                    long uid;
                    try {
                        uid = Long.parseLong(uidStr);
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                    Set<String> ds = diseaseByUser.get(uid);
                    if (ds == null || ds.isEmpty()) {
                        continue;
                    }
                    uidToDis.put(uidStr, new ArrayList<>(ds));
                }
                vo.setHeatmapUserDiseases(uidToDis);
            } else {
                vo.setHeatmapDiseaseLabels(Collections.emptyList());
                vo.setHeatmapMatrix(Collections.emptyList());
                vo.setHeatmapUserDiseases(Collections.emptyMap());
            }

            vo.setSummaryMetrics(metrics);

            JsonNode traceNode = root.path("process_trace");
            if (!traceNode.isMissingNode() && !traceNode.isNull()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> trace = objectMapper.convertValue(traceNode, Map.class);
                vo.setProcessTrace(trace);
            } else {
                vo.setProcessTrace(Collections.emptyMap());
            }
        } catch (Exception ignored) {
            vo.setGraphNodes(Collections.emptyList());
            vo.setGraphLinks(Collections.emptyList());
            vo.setSummaryMetrics(Collections.emptyMap());
            vo.setProcessTrace(Collections.emptyMap());
            vo.setClubSizes(Collections.emptyList());
            vo.setHeatmapDiseaseLabels(Collections.emptyList());
            vo.setHeatmapMatrix(Collections.emptyList());
            vo.setDashboardStats(Collections.emptyMap());
            vo.setHeatmapUserDiseases(Collections.emptyMap());
        }
        return vo;
    }

    @Override
    public List<ClubDivisionMemberAdminVO> listMembers(Long snapshotId) {
        ClubDivisionSnapshot snap;
        if (snapshotId != null) {
            snap = clubDivisionSnapshotMapper.selectById(snapshotId);
        } else {
            snap = clubDivisionSnapshotMapper.selectOne(
                    new LambdaQueryWrapper<ClubDivisionSnapshot>()
                            .orderByDesc(ClubDivisionSnapshot::getId)
                            .last("LIMIT 1"));
        }
        if (snap == null) {
            return Collections.emptyList();
        }
        Long sid = snap.getId();
        List<ClubDivisionMember> rows = clubDivisionMemberMapper.selectList(
                new LambdaQueryWrapper<ClubDivisionMember>()
                        .eq(ClubDivisionMember::getSnapshotId, sid)
                        .orderByAsc(ClubDivisionMember::getClubIndex)
                        .orderByAsc(ClubDivisionMember::getUserId));
        List<ClubDivisionMemberAdminVO> list = new ArrayList<>();
        for (ClubDivisionMember m : rows) {
            TUser u = tUserMapper.selectById(m.getUserId());
            ClubDivisionMemberAdminVO vo = new ClubDivisionMemberAdminVO();
            vo.setUserId(m.getUserId());
            vo.setClubIndex(m.getClubIndex());
            vo.setCentrality(m.getCentrality() != null ? m.getCentrality().doubleValue() : null);
            if (u != null) {
                vo.setNickname(u.getNickname());
                vo.setOpenid(u.getOpenid());
            }
            list.add(vo);
        }
        return list;
    }

    private static int manifestFilePriority(String name) {
        if ("dashboard.png".equals(name)) {
            return 0;
        }
        return 1;
    }
}

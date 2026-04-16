package com.android.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ClubDivisionAdminOverviewVO {
    private Long snapshotId;
    private LocalDateTime createdAt;
    private Double similarityThreshold;
    private Integer userCount;
    private Integer clubCount;
    private List<String> seeds;
    /** ECharts graph: nodes + links */
    private List<Map<String, Object>> graphNodes;
    private List<Map<String, Object>> graphLinks;
    /** 指标卡片：节点数、边数、平均度等 */
    private Map<String, Object> summaryMetrics;
    /** 算法过程轨迹（可选，管理端静态仪表盘可不展示） */
    private Map<String, Object> processTrace;

    /** 各社团人数（与 communities 顺序一致），用于柱状图 */
    private List<Integer> clubSizes;

    /** 疾病×社团 热力图：行=疾病标签，列=社团序号 0..n */
    private List<String> heatmapDiseaseLabels;
    private List<List<Integer>> heatmapMatrix;

    /** 用户ID字符串 -> 该用户推断疾病列表（供前端格子悬浮展示成员） */
    private Map<String, List<String>> heatmapUserDiseases;

    /** 仪表盘右下角统计：节点数、边数、社团数、密度等 */
    private Map<String, Object> dashboardStats;
}

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从 stdin 读取 JSON，向 stdout 输出 NCSS 社团划分与可视化所需数据（无调试 print，便于 Java 解析）。
输入示例：
{"similarity_threshold":0.3,"user_diseases":{"1":["高血压","糖尿病"],"2":["高血压"]}}
"""
import json
import os
import sys
from contextlib import contextmanager
from math import sqrt
from typing import Optional

from NCSS import NCSS


@contextmanager
def silence_stdout():
    with open(os.devnull, "w", encoding="utf-8") as devnull:
        old = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old


def jaccard(a: set, b: set) -> float:
    if not a and not b:
        return 0.0
    u = len(a | b)
    if u == 0:
        return 0.0
    return len(a & b) / u


def sanitize_text(s) -> str:
    """去除/替换孤立 UTF-16 代理字符，避免 json 输出或 Windows 控制台编码失败。"""
    if s is None:
        return ""
    t = str(s)
    try:
        return t.encode("utf-8", "replace").decode("utf-8")
    except Exception:
        out = []
        for ch in t:
            o = ord(ch)
            if 0xD800 <= o <= 0xDFFF:
                out.append("?")
            else:
                out.append(ch)
        return "".join(out)


def sanitize_json_obj(obj):
    """递归净化，保证可安全 json.dumps + 打印到 stdout。"""
    if obj is None or isinstance(obj, (bool, int, float)):
        return obj
    if isinstance(obj, str):
        return sanitize_text(obj)
    if isinstance(obj, list):
        return [sanitize_json_obj(x) for x in obj]
    if isinstance(obj, dict):
        return {sanitize_text(k) if isinstance(k, str) else k: sanitize_json_obj(v) for k, v in obj.items()}
    return obj


def cosine(a: list, b: list) -> float:
    if not a or not b:
        return 0.0
    n = min(len(a), len(b))
    if n <= 0:
        return 0.0
    dot = 0.0
    na = 0.0
    nb = 0.0
    for i in range(n):
        x = float(a[i] or 0.0)
        y = float(b[i] or 0.0)
        dot += x * y
        na += x * x
        nb += y * y
    if na <= 0 or nb <= 0:
        return 0.0
    return dot / (sqrt(na) * sqrt(nb))


def build_hybrid_graph(
    users: list,
    user_diseases: dict,
    user_vectors: dict,
    similarity_threshold: float,
    disease_weight: float,
):
    n = len(users)
    adjacency = [set() for _ in range(n)]
    edges = []
    vector_weight = 1.0 - disease_weight
    for i in range(n):
        for j in range(i + 1, n):
            ui = users[i]
            uj = users[j]
            ds = jaccard(user_diseases.get(ui, set()), user_diseases.get(uj, set()))
            vs = cosine(user_vectors.get(ui, []), user_vectors.get(uj, []))
            sim = disease_weight * ds + vector_weight * vs
            if sim > similarity_threshold:
                adjacency[i].add(j)
                adjacency[j].add(i)
                edges.append(
                    {
                        "source": ui,
                        "target": uj,
                        "similarity": round(sim, 6),
                        "diseaseSimilarity": round(ds, 6),
                        "vectorSimilarity": round(vs, 6),
                    }
                )
    return adjacency, edges


def _sim_lookup_from_edges(edges: list) -> dict:
    d = {}
    for e in edges:
        s, t = e["source"], e["target"]
        w = float(e["similarity"])
        d[(s, t)] = w
        d[(t, s)] = w
    return d


def _avg_sim_node_to_community(u: int, comm: set, users: list, sim_lu: dict, adjacency: list) -> float:
    """节点 u 与社团 comm 中邻接节点的平均边相似度（无邻接则退化为与任一连边的最大相似度）。"""
    vals = []
    for v in comm:
        if v == u:
            continue
        if v not in adjacency[u]:
            continue
        a, b = users[u], users[v]
        vals.append(float(sim_lu.get((a, b), sim_lu.get((b, a), 0.0))))
    if vals:
        return sum(vals) / len(vals)
    best = 0.0
    for v in comm:
        if v == u:
            continue
        a, b = users[u], users[v]
        best = max(best, float(sim_lu.get((a, b), sim_lu.get((b, a), 0.0))))
    return best


def fallback_assign_unassigned(
    communities: list,
    unassigned: set,
    users: list,
    edges: list,
    adjacency: list,
) -> tuple:
    """
    论文实现中扩张若因结构强度<=0、轮数上限等留下未分配节点，原逻辑会每人单独成社，易产生大量规模=1 的社团。
    在拆单点之前，按与已有社团的边相似度将节点并入最优社团（全零则并入当前最大社团）。
    """
    if not unassigned:
        return [], unassigned
    sim_lu = _sim_lookup_from_edges(edges)
    multi_idx = [i for i, c in enumerate(communities) if len(c) >= 2]
    if not multi_idx:
        multi_idx = list(range(len(communities)))

    events = []
    for u in sorted(unassigned):
        best_ci = multi_idx[0]
        best_s = -1.0
        for ci in multi_idx:
            s = _avg_sim_node_to_community(u, communities[ci], users, sim_lu, adjacency)
            if s > best_s:
                best_s = s
                best_ci = ci
        if best_s <= 1e-15:
            best_ci = max(multi_idx, key=lambda i: len(communities[i]))
        communities[best_ci].add(u)
        events.append(
            {
                "nodeIndex": int(u),
                "toCommunity": int(best_ci),
                "avgSimToCommunity": round(float(max(0.0, best_s)), 8),
            }
        )
    unassigned.clear()
    return events, unassigned


def compute_centrality_with_trace(ncss: NCSS, adjacency: list, common_neighbors: list):
    n = len(adjacency)
    denominator = []
    for i in range(n):
        total = 0
        for j in adjacency[i]:
            total += common_neighbors[i][j]
        denominator.append(total + 1)

    pr_cen = [1.0 / n] * n
    iteration_trace = [{"iteration": 0, "maxDiff": 0.0, "prSample": [round(x, 8) for x in pr_cen[:12]]}]
    for iteration in range(ncss.max_iterations):
        new_pr = [0.0] * n
        for i in range(n):
            contribution = 0.0
            for neighbor in adjacency[i]:
                contribution += pr_cen[neighbor] * (common_neighbors[i][neighbor] / denominator[neighbor])
            new_pr[i] = ncss.c * contribution + (1 - ncss.c) / n
        diff = max(abs(new_pr[i] - pr_cen[i]) for i in range(n))
        pr_cen = new_pr
        iteration_trace.append(
            {"iteration": iteration + 1, "maxDiff": round(float(diff), 10), "prSample": [round(x, 8) for x in pr_cen[:12]]}
        )
        if diff < ncss.epsilon:
            break
    return pr_cen, iteration_trace


def select_seeds_with_trace(adjacency: list, pr_cen: list):
    n = len(adjacency)
    avg_nodes = sum(pr_cen) / n
    avg_degree = sum(len(adjacency[i]) for i in range(n)) / n
    candidates = []
    for i in range(n):
        # 完全图/正则图上所有人度数相同，严格「>」会导致候选为空 → 无种子 → 全员单点社团
        # 对称网络上 PR 常全部等于均值，亦需「>=」
        if pr_cen[i] >= avg_nodes - 1e-15 and len(adjacency[i]) >= avg_degree - 1e-15:
            candidates.append((pr_cen[i], i, len(adjacency[i])))
    if not candidates:
        for i in range(n):
            if len(adjacency[i]) > 0:
                candidates.append((pr_cen[i], i, len(adjacency[i])))
    candidates.sort(reverse=True)
    seeds = []
    for _, node, _ in candidates:
        if not seeds or all(node not in adjacency[seed] for seed in seeds):
            seeds.append(node)
    if not seeds and n > 0:
        seeds = [max(range(n), key=lambda i: pr_cen[i])]
    cand_trace = [
        {"nodeIndex": int(node), "pr": round(float(pr), 8), "degree": int(deg)}
        for pr, node, deg in candidates
    ]
    return seeds, {"avgPr": round(float(avg_nodes), 8), "avgDegree": round(float(avg_degree), 4), "candidates": cand_trace}


EXPANSION_MAX_ROUNDS = 55


def expansion_with_trace(
    ncss: NCSS,
    adjacency: list,
    common_neighbors: list,
    seeds: list,
    users: list,
    edges: list,
):
    n = len(adjacency)
    communities = [{seed} for seed in seeds]
    node_to_community = {}
    for idx, community in enumerate(communities):
        for node in community:
            node_to_community[node] = idx
    unassigned = set(range(n)) - set(node_to_community.keys())
    rounds = []
    changed = True
    iteration = 0
    while changed and iteration < EXPANSION_MAX_ROUNDS:
        changed = False
        iteration += 1
        events = []
        for comm_idx, community in enumerate(communities):
            edge_nodes = set()
            for node in community:
                for neighbor in adjacency[node]:
                    if neighbor not in community:
                        edge_nodes.add(neighbor)
            candidates = []
            for node in edge_nodes:
                if node in unassigned or node_to_community.get(node, -1) != comm_idx:
                    strength = ncss.compute_structural_strength(node, community, adjacency, common_neighbors)
                    if strength > 0:
                        candidates.append((strength, node))
            if not candidates:
                continue
            candidates.sort(reverse=True)
            best_strength, best_node = candidates[0]
            if best_node in unassigned:
                communities[comm_idx].add(best_node)
                node_to_community[best_node] = comm_idx
                unassigned.remove(best_node)
                changed = True
                events.append(
                    {"action": "add", "nodeIndex": int(best_node), "toCommunity": int(comm_idx), "strength": round(float(best_strength), 8)}
                )
            elif node_to_community[best_node] != comm_idx:
                old_comm_idx = node_to_community[best_node]
                old_community = communities[old_comm_idx]
                dep_new = ncss.compute_dependency(best_node, community, adjacency)
                dep_old = ncss.compute_dependency(best_node, old_community, adjacency)
                if dep_new > dep_old and dep_new > ncss.dependency_threshold:
                    communities[old_comm_idx].remove(best_node)
                    communities[comm_idx].add(best_node)
                    node_to_community[best_node] = comm_idx
                    changed = True
                    events.append(
                        {
                            "action": "move",
                            "nodeIndex": int(best_node),
                            "fromCommunity": int(old_comm_idx),
                            "toCommunity": int(comm_idx),
                            "depNew": round(float(dep_new), 8),
                            "depOld": round(float(dep_old), 8),
                            "strength": round(float(best_strength), 8),
                        }
                    )
        rounds.append(
            {
                "iteration": iteration,
                "events": events,
                "communitySizes": [len(c) for c in communities],
                "unassignedCount": len(unassigned),
            }
        )

    fallback_events: list = []
    if unassigned:
        fallback_events, unassigned = fallback_assign_unassigned(communities, unassigned, users, edges, adjacency)
        for ev in fallback_events:
            u = ev["nodeIndex"]
            ci = ev["toCommunity"]
            node_to_community[u] = ci
        rounds.append(
            {
                "phase": "fallback_similarity_assign",
                "mergedCount": len(fallback_events),
                "events": fallback_events,
                "unassignedCount": len(unassigned),
                "communitySizes": [len(c) for c in communities],
            }
        )

    for node in sorted(list(unassigned)):
        communities.append({node})
        rounds.append({"iteration": iteration + 1, "events": [{"action": "singleton", "nodeIndex": int(node)}]})
    return communities, rounds


def run_pipeline(
    user_diseases: dict,
    user_vectors: dict,
    similarity_threshold: float,
    disease_weight: float,
    min_community_ratio: float = 0.0,
    min_community_abs: Optional[int] = None,
    dependency_threshold: float = 0.55,
) -> dict:
    ncss = NCSS(
        damping_factor=0.8,
        epsilon=1e-6,
        max_iterations=100,
        dependency_threshold=dependency_threshold,
        min_community_ratio=min_community_ratio,
        min_community_abs=min_community_abs,
    )
    users = list(user_diseases.keys())
    adjacency, edges = build_hybrid_graph(users, user_diseases, user_vectors, similarity_threshold, disease_weight)
    n = len(users)
    if n == 0:
        return {
            "users": [],
            "communities": [],
            "centrality": [],
            "seeds": [],
            "edges": [],
            "disease_sets": {},
        }
    with silence_stdout():
        common_neighbors = ncss.compute_common_neighbors(adjacency)
        pr_cen, pr_trace = compute_centrality_with_trace(ncss, adjacency, common_neighbors)
        seeds, seed_trace = select_seeds_with_trace(adjacency, pr_cen)
        communities, expansion_rounds = expansion_with_trace(ncss, adjacency, common_neighbors, seeds, users, edges)
        communities = ncss.filter_small_communities(communities, n)
        min_sz_used = ncss.effective_min_community_size(n)

    disease_sets = {
        sanitize_text(users[i]): [sanitize_text(x) for x in list(user_diseases.get(users[i], set()))]
        for i in range(n)
    }
    idx = {u: i for i, u in enumerate(users)}
    for edge in edges:
        i = idx[edge["source"]]
        j = idx[edge["target"]]
        edge["commonNeighbors"] = common_neighbors[i][j]

    comm_out = [[sanitize_text(users[i]) for i in sorted(comm)] for comm in communities]

    return {
        "users": [sanitize_text(u) for u in users],
        "communities": comm_out,
        "centrality": [round(float(x), 8) for x in pr_cen],
        "seeds": [sanitize_text(users[i]) for i in seeds],
        "edges": edges,
        "disease_sets": disease_sets,
        "adjacency_degrees": [len(adjacency[i]) for i in range(n)],
        "process_trace": {
            "step1_graph": {
                "userCount": n,
                "edgeCount": len(edges),
                "threshold": similarity_threshold,
                "diseaseWeight": disease_weight,
                "vectorWeight": round(1.0 - disease_weight, 6),
            },
            "step2_common_neighbors": {
                "maxCommonNeighbors": max((max(row) for row in common_neighbors), default=0),
            },
            "step3_pr_iterations": pr_trace,
            "step4_seed_selection": {
                "seeds": [sanitize_text(users[i]) for i in seeds],
                "detail": seed_trace,
            },
            "step5_expansion_rounds": expansion_rounds,
            "step6_filter": {
                "communityCountAfterFilter": len(comm_out),
                "communitySizes": [len(c) for c in comm_out],
                "minCommunitySizeThreshold": min_sz_used,
                "minCommunityRatio": min_community_ratio,
                "minCommunityAbs": min_community_abs,
                "dependencyThreshold": dependency_threshold,
            },
        },
    }


def main():
    try:
        raw = sys.stdin.read()
        if not raw.strip():
            print(json.dumps(sanitize_json_obj({"ok": False, "error": "empty stdin"}), ensure_ascii=False))
            return
        data = json.loads(raw)
        th = float(data.get("similarity_threshold", 0.45))
        disease_weight = float(data.get("disease_weight", 0.82))
        if disease_weight < 0:
            disease_weight = 0.0
        if disease_weight > 1:
            disease_weight = 1.0
        ud = data.get("user_diseases") or {}
        uv = data.get("user_vectors") or {}
        user_diseases = {
            sanitize_text(k): {sanitize_text(x) for x in (v or [])}
            for k, v in ud.items()
        }
        user_vectors = {}
        for k, v in uv.items():
            if isinstance(v, list):
                user_vectors[sanitize_text(k)] = v
            else:
                user_vectors[sanitize_text(k)] = []
        # 保留“有疾病”用户作为核心参与者
        user_diseases = {k: v for k, v in user_diseases.items() if v}
        if not user_diseases:
            print(json.dumps(sanitize_json_obj({"ok": False, "error": "no users with diseases"}), ensure_ascii=False))
            return
        min_community_ratio = float(data.get("min_community_ratio", 0.0))
        mac = data.get("min_community_abs")
        if mac is None or mac == "" or (isinstance(mac, (int, float)) and float(mac) <= 0):
            min_community_abs = None
        else:
            min_community_abs = int(mac)
        dependency_threshold = float(data.get("dependency_threshold", 0.55))
        out = run_pipeline(
            user_diseases,
            user_vectors,
            th,
            disease_weight,
            min_community_ratio=min_community_ratio,
            min_community_abs=min_community_abs,
            dependency_threshold=dependency_threshold,
        )
        out["ok"] = True
        out["similarity_threshold"] = th
        out["disease_weight"] = disease_weight
        out["min_community_ratio"] = min_community_ratio
        out["min_community_abs"] = min_community_abs
        out["dependency_threshold"] = dependency_threshold
        out["vector_weight"] = round(1.0 - disease_weight, 6)
        out["user_count"] = len(out.get("users") or [])
        out["club_count"] = len(out.get("communities") or [])
        out = sanitize_json_obj(out)
        print(json.dumps(out, ensure_ascii=False))
    except Exception as e:
        err = sanitize_json_obj({"ok": False, "error": str(e)})
        print(json.dumps(err, ensure_ascii=False))


if __name__ == "__main__":
    if sys.platform == "win32":
        try:
            sys.stdout.reconfigure(encoding="utf-8")
        except Exception:
            pass
    main()

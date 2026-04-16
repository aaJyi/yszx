import numpy as np
from collections import defaultdict, Counter
from typing import List, Dict, Set, Tuple, Optional
import heapq

"""社团划分算法"""
class NCSS:
    def __init__(self, damping_factor: float = 0.8, epsilon: float = 1e-6,
                 max_iterations: int = 100, dependency_threshold: float = 0.5,
                 min_community_ratio: float = 0.05,
                 min_community_abs: Optional[int] = None):
        """
            damping_factor: 阻尼系数 c，通常为 0.8
            epsilon: 收敛精度阈值
            max_iterations: 最大迭代次数
            dependency_threshold: 依赖度阈值，默认 0.5
            min_community_ratio: 最小社团规模占节点总数的比例（与 min_community_abs 二选一优先用 abs）
            min_community_abs: 若为正整数，则「过小」阈值为该人数（低于则并入最大社），例如 100
        """
        self.c = damping_factor
        self.epsilon = epsilon
        self.max_iterations = max_iterations
        self.dependency_threshold = dependency_threshold
        self.min_community_ratio = min_community_ratio
        self.min_community_abs = min_community_abs

    def effective_min_community_size(self, total_nodes: int) -> int:
        """成员数低于该值的社团会被解散并并入当前最大社团（若 ratio=0 且 abs 未设，则阈值为 1，等价于不合并）。"""
        if self.min_community_abs is not None and int(self.min_community_abs) > 0:
            return min(total_nodes, max(1, int(self.min_community_abs)))
        return max(1, int(total_nodes * self.min_community_ratio))

    """计算每个节点的相似度并生成社交网络"""
    def build_user_similarity_graph(self, user_diseases: Dict[str, Set[str]],
                                    similar_threshold: float = 0.3) -> Tuple[List[str], List[Set[int]]]:
        users = list(user_diseases.keys())
        n = len(users)
        diseases_sets = [user_diseases[user] for user in users]
        adjacency = [set() for _ in range(n)]
        for i in range(n):
            for j in range(i + 1, n):
                intersection = len(diseases_sets[i] & diseases_sets[j])
                union = len(diseases_sets[i] | diseases_sets[j])
                if union == 0:
                    similarity = 0
                else:
                    similarity = intersection / union
                if similarity > similar_threshold:
                    adjacency[i].add(j)
                    adjacency[j].add(i)
        return users, adjacency

    """计算所有节点对的共同邻居相似度"""
    def compute_common_neighbors(self, adjacency: List[Set[int]]) -> List[List[int]]:
        n = len(adjacency)
        common_neighbors = [[0] * n for _ in range(n)]
        for i in range(n):
            for j in range(i + 1, n):
                common = adjacency[i] & adjacency[j]
                common_neighbors[i][j] = len(common)
                common_neighbors[j][i] = len(common)
        return common_neighbors

    """计算节点中心度"""
    def compute_node_centrality(self, adjacency: List[Set[int]], common_neighbors: List[List[int]]) -> List[float]:
        n = len(adjacency)
        denominator = []
        for i in range(n):
            total = 0
            for j in adjacency[i]:
                total += common_neighbors[i][j]
            denominator.append(total + 1)

        pr_cen = [1.0 / n] * n
        for iteration in range(self.max_iterations):
            new_pr = [0.0] * n
            for i in range(n):
                contribution = 0.0
                for neighbor in adjacency[i]:
                    contribution += pr_cen[neighbor] * (common_neighbors[i][neighbor] / denominator[neighbor])
                new_pr[i] = self.c * contribution + (1 - self.c) / n
            diff = max(abs(new_pr[i] - pr_cen[i]) for i in range(n))
            pr_cen = new_pr
            if diff < self.epsilon:
                print(f"节点中心度收敛于第{iteration + 1}次迭代 实际参数为{diff} 收敛参数为{self.epsilon}")
                break
        return pr_cen

    def select_seed_nodes(self, adjacency: List[Set[int]],
                          pr_cen: List[float]) -> List[int]:
        n = len(adjacency)
        avg_nodes = sum(pr_cen) / n
        avg_degree = sum(len(adjacency[i]) for i in range(n)) / n
        candidates = []
        for i in range(n):
            # 完全图/正则图：度数全为 avg_degree，严格大于会导致无种子
            if pr_cen[i] >= avg_nodes - 1e-15 and len(adjacency[i]) >= avg_degree - 1e-15:
                candidates.append((pr_cen[i], i))
        if not candidates:
            for i in range(n):
                if len(adjacency[i]) > 0:
                    candidates.append((pr_cen[i], i))
        candidates.sort(reverse=True)
        seeds = []
        for _, node in candidates:
            if not seeds or all(node not in adjacency[seed] for seed in seeds):
                seeds.append(node)
        print(f"选择了{len(seeds)}个种子节点{seeds}")
        return seeds

    """节点对社团的依赖度"""
    def compute_dependency(self, node: int, community: Set[int], adjacency: List[Set[int]]) -> float:
        if len(adjacency[node]) == 0:
            return 0.0
        neighbors_in_community = len(adjacency[node] & community)
        return neighbors_in_community / len(adjacency[node])

    """节点对社团的结构关系强度"""
    def compute_structural_strength(self, node: int, community: Set[int],
                                    adjacency: List[Set[int]],
                                    common_neighbors: List[List[int]]) -> float:
        m = sum(len(adj) for adj in adjacency)  # 修正：使用 m 而不是 degree_sum
        if m == 0:
            return 0.0
        s_in = 0
        s_out = 0
        for u in community:
            for v in adjacency[u]:
                if v in community:
                    s_in += 1
                else:
                    s_out += 1
        s_in //= 2
        strength_sum = 0.0
        for j in community:
            if j == node:
                continue
            cn = common_neighbors[node][j]
            k_in = cn
            k_out = len(adjacency[node] & adjacency[j])
            term = 2 * cn - (k_out * s_in) / m - (k_in * s_out) / m
            strength_sum += term
        return strength_sum

    """社团扩张算法"""
    def community_expansion(self, adjacency: List[Set[int]],
                            common_neighbors: List[List[int]],
                            seeds: List[int]) -> List[Set[int]]:
        n = len(adjacency)
        communities = [{seed} for seed in seeds]
        node_to_community = {}
        for idx, community in enumerate(communities):
            for node in community:
                node_to_community[node] = idx
        unassigned = set(range(n)) - set(node_to_community.keys())
        changed = True
        iteration = 0
        while changed and iteration < 20:
            changed = False
            iteration += 1
            for comm_idx, community in enumerate(communities):
                edge_nodes = set()
                for node in community:
                    for neighbor in adjacency[node]:
                        if neighbor not in community:
                            edge_nodes.add(neighbor)
                candidates = []
                for node in edge_nodes:
                    if node in unassigned or node_to_community.get(node, -1) != comm_idx:
                        strength = self.compute_structural_strength(
                            node, community, adjacency, common_neighbors
                        )
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
                    print(f"  将节点 {best_node} 加入社团 {comm_idx}")

                elif node_to_community[best_node] != comm_idx:
                    old_comm_idx = node_to_community[best_node]
                    old_community = communities[old_comm_idx]
                    dep_new = self.compute_dependency(best_node, community, adjacency)
                    dep_old = self.compute_dependency(best_node, old_community, adjacency)
                    if dep_new > dep_old and dep_new > self.dependency_threshold:
                        communities[old_comm_idx].remove(best_node)
                        communities[comm_idx].add(best_node)
                        node_to_community[best_node] = comm_idx
                        changed = True
                        print(f"  将节点 {best_node} 从社团 {old_comm_idx} 移动到社团 {comm_idx}")
        for node in unassigned:
            communities.append({node})
            print(f"  未分配节点 {node} 自成社团")
        return communities

    """过滤小规模社团"""
    def filter_small_communities(self, communities: List[Set[int]],
                                 total_nodes: int) -> List[Set[int]]:
        min_size = self.effective_min_community_size(total_nodes)
        filtered = []
        dissolved_nodes = set()
        for comm in communities:
            if len(comm) >= min_size:
                filtered.append(comm)
            else:
                dissolved_nodes.update(comm)
                print(f"  解散小社团 (大小={len(comm)}): {comm}")
        if dissolved_nodes:
            if filtered:
                largest_comm = max(filtered, key=len)
                largest_comm.update(dissolved_nodes)
                print(f"  将 {len(dissolved_nodes)} 个解散节点加入最大社团")
            else:
                filtered.append(dissolved_nodes)
        return filtered

    """社团划分"""
    def detect_communities(self, user_diseases: Dict[str, Set[str]],
                           similarity_threshold: float = 0.3) -> Tuple[List[str], List[Set[int]]]:
        print("=" * 60)
        print("NCSS 社团划分算法开始")
        print("=" * 60)

        # Step 1: 构建用户相似度图
        print("\n[Step 1] 构建用户相似度图...")
        users, adjacency = self.build_user_similarity_graph(user_diseases, similarity_threshold)
        n = len(users)
        print(f"  节点数: {n}")
        print(f"  边数: {sum(len(adj) for adj in adjacency) // 2}")

        # Step 2: 计算共同邻居矩阵
        print("\n[Step 2] 计算共同邻居...")
        common_neighbors = self.compute_common_neighbors(adjacency)

        # Step 3: 计算节点中心度
        print("\n[Step 3] 计算节点中心度 PRcen...")
        pr_cen = self.compute_node_centrality(adjacency, common_neighbors)
        for i, (user, pr) in enumerate(zip(users, pr_cen)):
            print(f"  {user}: PRcen = {pr:.4f}")

        # Step 4: 选择种子节点
        print("\n[Step 4] 选择种子节点...")
        seeds = self.select_seed_nodes(adjacency, pr_cen)

        # Step 5: 社团扩张
        print("\n[Step 5] 社团扩张...")
        communities = self.community_expansion(adjacency, common_neighbors, seeds)

        # Step 6: 过滤小社团
        print("\n[Step 6] 过滤小社团...")
        communities = self.filter_small_communities(communities, n)

        # 输出结果
        print("\n" + "=" * 60)
        print("社团划分结果")
        print("=" * 60)
        for idx, comm in enumerate(communities):
            comm_users = [users[i] for i in comm]
            print(f"\n社团 {idx + 1}: {len(comm)} 个成员")
            print(f"  成员: {comm_users}")

        return users, communities

    """为目标用户推荐同社团的其他成员"""
    def recommend_users(self, target_user: str, users: List[str],
                        communities: List[Set[int]]) -> List[str]:
        target_idx = None
        for idx, user in enumerate(users):
            if user == target_user:
                target_idx = idx
                break
        if target_idx is None:
            return []
        for comm in communities:
            if target_idx in comm:
                recommended = [users[i] for i in comm if i != target_idx]
                return recommended
        return []


if __name__ == '__main__':
    ncss = NCSS(
        damping_factor=0.8,
        epsilon=1e-6,
        max_iterations=100,
        dependency_threshold=0.5,
        min_community_ratio=0.05
    )

    user_diseases = {
        "张三": {"高血压", "糖尿病"},
        "李四": {"高血压", "高血脂"},
        "王五": {"糖尿病", "冠心病"},
        "赵六": {"高血脂", "痛风"},
        "小明": {"糖尿病", "高血脂", "哮喘"},
        "小红": {"高血压", "糖尿病", "高血脂"},
        "小刚": {"冠心病", "哮喘"},
        "刘阿姨": {"高血压", "冠心病"},
        "陈叔叔": {"焦虑症"},
        "周奶奶": {"抑郁症"},
    }

    users, communities = ncss.detect_communities(user_diseases, similarity_threshold=0.3)

    print("\n" + "=" * 60)
    print("推荐结果")
    print("=" * 60)
    for user in users:
        recommendations = ncss.recommend_users(user, users, communities)
        if recommendations:
            print(f"\n{user} 可以交流: {recommendations}")
        else:
            print(f"\n{user} 暂无推荐对象（社团中只有自己）")
import os
import numpy as np
from collections import defaultdict, Counter
from typing import List, Dict, Set, Tuple, Optional
import heapq
import matplotlib.pyplot as plt
import matplotlib.animation as animation
import networkx as nx
from matplotlib.patches import Patch
import warnings

warnings.filterwarnings('ignore')

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'WenQuanYi Micro Hei']
plt.rcParams['axes.unicode_minus'] = False


class NCSS:
    def __init__(self, damping_factor: float = 0.8, epsilon: float = 1e-6,
                 max_iterations: int = 100, dependency_threshold: float = 0.5,
                 min_community_ratio: float = 0.05):
        """
            damping_factor: 阻尼系数 c，通常为 0.8
            epsilon: 收敛精度阈值
            max_iterations: 最大迭代次数
            dependency_threshold: 依赖度阈值，默认 0.5
            min_community_ratio: 最小社团规模占节点总数的比例
        """
        self.c = damping_factor
        self.epsilon = epsilon
        self.max_iterations = max_iterations
        self.dependency_threshold = dependency_threshold
        self.min_community_ratio = min_community_ratio
        self.history = []  # 存储历史状态用于动画

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
        history_pr = [pr_cen.copy()]

        for iteration in range(self.max_iterations):
            new_pr = [0.0] * n
            for i in range(n):
                contribution = 0.0
                for neighbor in adjacency[i]:
                    contribution += pr_cen[neighbor] * (common_neighbors[i][neighbor] / denominator[neighbor])
                new_pr[i] = self.c * contribution + (1 - self.c) / n
            diff = max(abs(new_pr[i] - pr_cen[i]) for i in range(n))
            pr_cen = new_pr
            history_pr.append(pr_cen.copy())

            if diff < self.epsilon:
                print(f"节点中心度收敛于第{iteration + 1}次迭代 实际参数为{diff} 收敛参数为{self.epsilon}")
                break

        self.history_pr = history_pr
        return pr_cen

    def select_seed_nodes(self, adjacency: List[Set[int]],
                          pr_cen: List[float]) -> List[int]:
        n = len(adjacency)
        avg_nodes = sum(pr_cen) / n
        avg_degree = sum(len(adjacency[i]) for i in range(n)) / n
        candidates = []
        for i in range(n):
            if pr_cen[i] > avg_nodes and len(adjacency[i]) > avg_degree:
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
        m = sum(len(adj) for adj in adjacency)
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

        # 记录历史状态
        self.history_communities = [self._copy_communities(communities)]

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

            self.history_communities.append(self._copy_communities(communities))

        for node in unassigned:
            communities.append({node})
            print(f"  未分配节点 {node} 自成社团")

        return communities

    def _copy_communities(self, communities):
        """深拷贝社团状态"""
        return [set(comm) for comm in communities]

    """过滤小规模社团"""

    def filter_small_communities(self, communities: List[Set[int]],
                                 total_nodes: int) -> List[Set[int]]:
        min_size = max(1, int(total_nodes * self.min_community_ratio))
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

        # 保存结果
        self.users = users
        self.adjacency = adjacency
        self.communities = communities

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

    """动态可视化社团扩张过程"""

    def animate_community_expansion(self, interval: float = 1.0):
        """动态演示社团扩张过程"""
        if not hasattr(self, 'history_communities'):
            print("没有历史记录，请先运行 detect_communities")
            return

        users = self.users
        adjacency = self.adjacency

        # 创建图
        G = nx.Graph()
        for i, user in enumerate(users):
            G.add_node(user)
        for i, neighbors in enumerate(adjacency):
            for j in neighbors:
                if i < j:
                    G.add_edge(users[i], users[j])

        # 布局
        pos = nx.spring_layout(G, k=2, seed=42)

        # 创建图形
        fig, axes = plt.subplots(2, 2, figsize=(16, 12))
        fig.suptitle('NCSS 社团划分算法动态演示', fontsize=16, fontweight='bold')

        # 颜色映射
        colors = plt.cm.tab20(np.linspace(0, 1, 20))

        def update(frame):
            # 清除所有子图
            for ax in axes.flat:
                ax.clear()

            # 获取当前帧的社团状态
            communities = self.history_communities[min(frame, len(self.history_communities) - 1)]

            # 1. 社团演化图
            ax1 = axes[0, 0]
            node_to_community = {}
            for comm_idx, comm in enumerate(communities):
                for node in comm:
                    node_to_community[node] = comm_idx

            node_colors = [colors[node_to_community.get(i, 0) % 20] for i in range(len(users))]

            nx.draw_networkx_nodes(G, pos, ax=ax1, node_color=node_colors,
                                   node_size=500, alpha=0.8, edgecolors='black', linewidths=2)
            nx.draw_networkx_edges(G, pos, ax=ax1, alpha=0.3, edge_color='gray', width=1)
            nx.draw_networkx_labels(G, pos, ax=ax1, font_size=9, font_weight='bold')
            ax1.set_title(f'社团演化 (步骤 {frame + 1}/{len(self.history_communities)})', fontsize=12)
            ax1.axis('off')

            # 2. 节点中心度变化图
            ax2 = axes[0, 1]
            if hasattr(self, 'history_pr') and frame < len(self.history_pr):
                pr_values = self.history_pr[frame]
                users_display = users[:10]  # 只显示前10个
                pr_display = pr_values[:10]
                bars = ax2.bar(users_display, pr_display, color='steelblue', alpha=0.7)
                ax2.set_ylabel('中心度值', fontsize=10)
                ax2.set_title(f'节点中心度分布 (迭代 {frame + 1})', fontsize=12)
                ax2.tick_params(axis='x', rotation=45, labelsize=8)
                # 添加数值标签
                for bar, val in zip(bars, pr_display):
                    ax2.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 0.005,
                             f'{val:.3f}', ha='center', va='bottom', fontsize=8)

            # 3. 社团大小统计图
            ax3 = axes[1, 0]
            comm_sizes = [len(comm) for comm in communities]
            if comm_sizes:
                comm_labels = [f'社团{i + 1}' for i in range(len(comm_sizes))]
                wedges, texts, autotexts = ax3.pie(comm_sizes, labels=comm_labels, autopct='%1.1f%%',
                                                   colors=colors[:len(comm_sizes)])
                ax3.set_title(f'社团大小分布 (共{len(communities)}个社团)', fontsize=12)

            # 4. 网络度数分布图
            ax4 = axes[1, 1]
            degrees = [len(adjacency[i]) for i in range(len(users))]
            ax4.hist(degrees, bins=range(max(degrees) + 2), alpha=0.7, color='coral', edgecolor='black')
            ax4.set_xlabel('度数', fontsize=10)
            ax4.set_ylabel('频数', fontsize=10)
            ax4.set_title('网络度数分布', fontsize=12)
            ax4.grid(True, alpha=0.3)

            plt.tight_layout()

        # 创建动画
        total_frames = len(self.history_communities)
        anim = animation.FuncAnimation(fig, update, frames=total_frames,
                                       interval=interval * 1000, repeat=True)

        plt.tight_layout()
        plt.show()
        return anim

    def _build_static_figure(self):
        """生成静态仪表盘 Figure（社团/规模/中心度/热力图/推荐/统计）"""
        if not hasattr(self, 'users'):
            return None

        users = self.users
        adjacency = self.adjacency
        communities = self.communities

        # 创建图
        G = nx.Graph()
        for i, user in enumerate(users):
            G.add_node(user)
        for i, neighbors in enumerate(adjacency):
            for j in neighbors:
                if i < j:
                    G.add_edge(users[i], users[j])

        # 布局
        pos = nx.spring_layout(G, k=2, seed=42)

        # 创建图形
        fig = plt.figure(figsize=(18, 10))

        # 1. 社团划分结果
        ax1 = plt.subplot(2, 3, 1)
        colors = plt.cm.tab20(np.linspace(0, 1, len(communities)))
        node_to_community = {}
        for comm_idx, comm in enumerate(communities):
            for node in comm:
                node_to_community[node] = comm_idx

        node_colors = [colors[node_to_community.get(i, 0) % len(colors)] for i in range(len(users))]
        nx.draw_networkx_nodes(G, pos, ax=ax1, node_color=node_colors,
                               node_size=600, alpha=0.8, edgecolors='black', linewidths=2)
        nx.draw_networkx_edges(G, pos, ax=ax1, alpha=0.3, edge_color='gray', width=1)
        nx.draw_networkx_labels(G, pos, ax=ax1, font_size=9, font_weight='bold')
        ax1.set_title('社团划分结果', fontsize=14, fontweight='bold')
        ax1.axis('off')

        # 添加图例
        legend_elements = []
        for comm_idx, comm in enumerate(communities):
            comm_users = [users[i] for i in comm]
            legend_elements.append(Patch(facecolor=colors[comm_idx], alpha=0.8,
                                         label=f'社团{comm_idx + 1}: {len(comm)}人'))
        ax1.legend(handles=legend_elements, loc='upper left', bbox_to_anchor=(1, 1), fontsize=8)

        # 2. 社团大小柱状图
        ax2 = plt.subplot(2, 3, 2)
        comm_sizes = [len(comm) for comm in communities]
        bars = ax2.bar(range(1, len(comm_sizes) + 1), comm_sizes, color=colors[:len(comm_sizes)], alpha=0.7)
        ax2.set_xlabel('社团编号', fontsize=11)
        ax2.set_ylabel('成员数量', fontsize=11)
        ax2.set_title('社团规模分布', fontsize=14, fontweight='bold')
        for bar, size in zip(bars, comm_sizes):
            ax2.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 0.1,
                     str(size), ha='center', va='bottom', fontsize=10)

        # 3. 节点中心度排名
        ax3 = plt.subplot(2, 3, 3)
        if hasattr(self, 'history_pr'):
            final_pr = self.history_pr[-1] if self.history_pr else []
            sorted_indices = np.argsort(final_pr)[::-1][:8]
            sorted_users = [users[i] for i in sorted_indices]
            sorted_pr = [final_pr[i] for i in sorted_indices]
            colors_bar = plt.cm.RdYlGn(np.linspace(0.2, 0.8, len(sorted_pr)))[::-1]
            ax3.barh(sorted_users, sorted_pr, color=colors_bar, alpha=0.8)
            ax3.set_xlabel('中心度值', fontsize=11)
            ax3.set_title('节点中心度排名 (Top 8)', fontsize=14, fontweight='bold')
            ax3.grid(axis='x', alpha=0.3)

        # 4. 疾病分布热力图
        ax4 = plt.subplot(2, 3, 4)
        # 构建疾病-社团矩阵
        all_diseases = set()
        for diseases in self.user_diseases.values():
            all_diseases.update(diseases)
        all_diseases = list(all_diseases)

        if len(all_diseases) == 0 or len(communities) == 0:
            ax4.text(0.5, 0.5, '无疾病—社团分布数据', ha='center', va='center', transform=ax4.transAxes, fontsize=12)
            ax4.set_axis_off()
        else:
            disease_comm_matrix = np.zeros((len(all_diseases), len(communities)))
            for comm_idx, comm in enumerate(communities):
                for node in comm:
                    user = users[node]
                    for disease in self.user_diseases.get(user, set()):
                        if disease in all_diseases:
                            disease_comm_matrix[all_diseases.index(disease), comm_idx] += 1

            im = ax4.imshow(disease_comm_matrix, cmap='YlOrRd', aspect='auto', alpha=0.8)
            ax4.set_xticks(range(len(communities)))
            ax4.set_xticklabels([f'社团{i + 1}' for i in range(len(communities))], rotation=45)
            ax4.set_yticks(range(len(all_diseases)))
            ax4.set_yticklabels(all_diseases, fontsize=9)
            ax4.set_title('疾病在各社团的分布', fontsize=14, fontweight='bold')
            plt.colorbar(im, ax=ax4, label='患者数量')

        # 5. 推荐网络图
        ax5 = plt.subplot(2, 3, 5)
        # 选择一个示例用户进行推荐展示
        example_user = users[0] if users else None
        if example_user:
            recommendations = self.recommend_users(example_user, users, communities)
            # 构建推荐子图
            rec_G = nx.Graph()
            rec_G.add_node(example_user)
            for rec in recommendations:
                rec_G.add_node(rec)
                rec_G.add_edge(example_user, rec)

            rec_pos = nx.spring_layout(rec_G, k=1, seed=42)
            nx.draw_networkx_nodes(rec_G, rec_pos, ax=ax5,
                                   node_color=['red'] + ['lightblue'] * len(recommendations),
                                   node_size=800, edgecolors='black', linewidths=2)
            nx.draw_networkx_edges(rec_G, rec_pos, ax=ax5, width=2, alpha=0.7, edge_color='gray')
            nx.draw_networkx_labels(rec_G, rec_pos, ax=ax5, font_size=10, font_weight='bold')
            ax5.set_title(f'推荐示例: {example_user} 的交流对象', fontsize=14, fontweight='bold')
            ax5.axis('off')

        # 6. 网络统计信息
        ax6 = plt.subplot(2, 3, 6)
        ax6.axis('off')
        stats_text = f"""
        网络统计信息
        ═══════════════════
        总用户数: {len(users)}
        总连接数: {sum(len(adj) for adj in adjacency) // 2}
        社团数量: {len(communities)}

        最大社团规模: {max([len(comm) for comm in communities])}
        最小社团规模: {min([len(comm) for comm in communities])}
        平均社团规模: {np.mean([len(comm) for comm in communities]):.1f}

        网络密度: {2 * sum(len(adj) for adj in adjacency) / (len(users) * (len(users) - 1)) if len(users) > 1 else 0:.3f}
        """
        ax6.text(0.1, 0.5, stats_text, transform=ax6.transAxes, fontsize=11,
                 verticalalignment='center', fontfamily='monospace',
                 bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))

        plt.suptitle('NCSS 社团划分算法可视化分析', fontsize=18, fontweight='bold', y=1.02)
        plt.tight_layout()
        return plt.gcf()

    def static_visualization(self):
        """静态可视化最终结果（弹窗）"""
        fig = self._build_static_figure()
        if fig is None:
            print("请先运行 detect_communities")
            return
        plt.show()

    def export_dashboard_png(self, path):
        """导出静态仪表盘到 PNG（供管理端展示）"""
        fig = self._build_static_figure()
        if fig is None:
            return False
        fig.savefig(path, dpi=120, bbox_inches='tight')
        plt.close(fig)
        return True

    def export_expansion_frames(self, img_dir, max_frames=48):
        """
        导出社团扩张过程各帧为 PNG，文件名 expansion_000.png ...
        返回已写入的相对文件名列表。
        """
        if not hasattr(self, 'history_communities') or not self.history_communities:
            return []
        users = self.users
        adjacency = self.adjacency
        G = nx.Graph()
        for i, user in enumerate(users):
            G.add_node(user)
        for i, neighbors in enumerate(adjacency):
            for j in neighbors:
                if i < j:
                    G.add_edge(users[i], users[j])
        pos = nx.spring_layout(G, k=2, seed=42)
        colors = plt.cm.tab20(np.linspace(0, 1, 20))

        n_frames = min(len(self.history_communities), max_frames)
        saved = []
        for frame in range(n_frames):
            fig, axes = plt.subplots(2, 2, figsize=(16, 12))
            fig.suptitle('NCSS 社团划分算法动态演示', fontsize=16, fontweight='bold')

            communities = self.history_communities[min(frame, len(self.history_communities) - 1)]

            ax1 = axes[0, 0]
            node_to_community = {}
            for comm_idx, comm in enumerate(communities):
                for node in comm:
                    node_to_community[node] = comm_idx
            node_colors = [colors[node_to_community.get(i, 0) % 20] for i in range(len(users))]
            nx.draw_networkx_nodes(G, pos, ax=ax1, node_color=node_colors,
                                   node_size=500, alpha=0.8, edgecolors='black', linewidths=2)
            nx.draw_networkx_edges(G, pos, ax=ax1, alpha=0.3, edge_color='gray', width=1)
            nx.draw_networkx_labels(G, pos, ax=ax1, font_size=9, font_weight='bold')
            ax1.set_title(f'社团演化 (步骤 {frame + 1}/{len(self.history_communities)})', fontsize=12)
            ax1.axis('off')

            ax2 = axes[0, 1]
            if hasattr(self, 'history_pr') and frame < len(self.history_pr):
                pr_values = self.history_pr[frame]
                users_display = users[:10]
                pr_display = pr_values[:10]
                bars = ax2.bar(users_display, pr_display, color='steelblue', alpha=0.7)
                ax2.set_ylabel('中心度值', fontsize=10)
                ax2.set_title(f'节点中心度分布 (迭代 {frame + 1})', fontsize=12)
                ax2.tick_params(axis='x', rotation=45, labelsize=8)
                for bar, val in zip(bars, pr_display):
                    ax2.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 0.005,
                             f'{val:.3f}', ha='center', va='bottom', fontsize=8)

            ax3 = axes[1, 0]
            comm_sizes = [len(comm) for comm in communities]
            if comm_sizes:
                comm_labels = [f'社团{i + 1}' for i in range(len(comm_sizes))]
                ax3.pie(comm_sizes, labels=comm_labels, autopct='%1.1f%%',
                        colors=colors[:len(comm_sizes)])
                ax3.set_title(f'社团大小分布 (共{len(communities)}个社团)', fontsize=12)

            ax4 = axes[1, 1]
            degrees = [len(adjacency[i]) for i in range(len(users))]
            if degrees:
                ax4.hist(degrees, bins=range(max(degrees) + 2), alpha=0.7, color='coral', edgecolor='black')
            ax4.set_xlabel('度数', fontsize=10)
            ax4.set_ylabel('频数', fontsize=10)
            ax4.set_title('网络度数分布', fontsize=12)
            ax4.grid(True, alpha=0.3)

            plt.tight_layout()
            fname = f'expansion_{frame:03d}.png'
            fig.savefig(os.path.join(img_dir, fname), dpi=100, bbox_inches='tight')
            plt.close(fig)
            saved.append(fname)
        return saved


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

    # 保存疾病数据用于可视化
    ncss.user_diseases = user_diseases

    # 执行社团划分
    users, communities = ncss.detect_communities(user_diseases, similarity_threshold=0.3)

    # 静态可视化
    print("\n生成静态可视化...")
    ncss.static_visualization()

    # 动态可视化社团扩张过程
    print("\n生成动态可视化...")
    ncss.animate_community_expansion(interval=1.0)

    # 推荐结果
    print("\n" + "=" * 60)
    print("推荐结果")
    print("=" * 60)
    for user in users:
        recommendations = ncss.recommend_users(user, users, communities)
        if recommendations:
            print(f"\n{user} 可以交流: {recommendations}")
        else:
            print(f"\n{user} 暂无推荐对象（社团中只有自己）")
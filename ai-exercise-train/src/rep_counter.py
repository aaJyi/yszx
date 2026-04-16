"""
运动次数计数模块
基于关键点角度变化检测完整动作 (上升-下降循环)
"""

import math
from typing import List, Optional, Tuple


def get_angle(p1: Tuple[float, float], p2: Tuple[float, float], p3: Tuple[float, float]) -> float:
    """计算三点形成的角度 (以 p2 为顶点)"""
    if p1[0] == p2[0] and p1[1] == p2[1]:
        return 0
    if p3[0] == p2[0] and p3[1] == p2[1]:
        return 0
    v1 = (p1[0] - p2[0], p1[1] - p2[1])
    v2 = (p3[0] - p2[0], p3[1] - p2[1])
    dot = v1[0] * v2[0] + v1[1] * v2[1]
    len1 = math.sqrt(v1[0] ** 2 + v1[1] ** 2)
    len2 = math.sqrt(v2[0] ** 2 + v2[1] ** 2)
    if len1 == 0 or len2 == 0:
        return 0
    cos_a = max(-1, min(1, dot / (len1 * len2)))
    return math.degrees(math.acos(cos_a))


def get_distance(p1: Tuple[float, float], p2: Tuple[float, float]) -> float:
    """计算两点距离"""
    return math.sqrt((p2[0] - p1[0]) ** 2 + (p2[1] - p1[1]) ** 2)


class RepCounter:
    """基于角度的次数计数器"""

    def __init__(self, up_angle: float = 145, down_angle: float = 90, kpts: List[int] = None):
        self.up_angle = up_angle
        self.down_angle = down_angle
        self.kpts = kpts or [6, 8, 10]  # 默认肩-肘-腕 (俯卧撑)
        self.state = "up"  # up / down
        self.count = 0

    def reset(self):
        self.state = "up"
        self.count = 0

    def update(self, keypoints) -> int:
        """
        根据关键点更新计数
        keypoints: shape (17, 3) 每行为 [x, y, confidence]
        """
        if keypoints is None or len(keypoints) < 3:
            return self.count

        # 根据 kpts 取点
        pts = []
        for i in self.kpts:
            if i < len(keypoints):
                k = keypoints[i]
                if len(k) >= 2 and (k[2] > 0.3 if len(k) > 2 else True):
                    pts.append((float(k[0]), float(k[1])))
                else:
                    return self.count
            else:
                return self.count

        if len(pts) == 3:
            angle = get_angle(pts[0], pts[1], pts[2])
        elif len(pts) == 2:
            # 开合跳等: 肩-髋距离变化 (手臂张开时距离大)
            dist = get_distance(pts[0], pts[1])
            if dist >= self.up_angle:
                if self.state == "down":
                    self.count += 1
                self.state = "up"
            elif dist <= self.down_angle:
                self.state = "down"
            return self.count
        else:
            return self.count

        # 三点的角度逻辑
        if angle >= self.up_angle:
            if self.state == "down":
                self.count += 1
            self.state = "up"
        elif angle <= self.down_angle:
            self.state = "down"

        return self.count

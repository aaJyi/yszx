package com.android.controller;

import com.android.common.Result;
import com.android.dto.FamilyGraphVO;
import com.android.service.IAdminFamilyGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：家人关系图谱（蜘蛛网力导向图数据）
 */
@RestController
@RequestMapping("/admin/family-graph")
@RequiredArgsConstructor
public class AdminFamilyGraphController {

    private final IAdminFamilyGraphService adminFamilyGraphService;

    @GetMapping
    public Result<FamilyGraphVO> graph() {
        return Result.success(adminFamilyGraphService.buildGraph());
    }
}

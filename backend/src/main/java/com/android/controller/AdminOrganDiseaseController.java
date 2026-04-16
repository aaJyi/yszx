package com.android.controller;

import com.android.common.Result;
import com.android.dto.OrganDiseaseBundleVO;
import com.android.service.IOrganDiseaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 / 病机图谱：按器官查询疾病详述与治疗措施
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminOrganDiseaseController {

    private final IOrganDiseaseService organDiseaseService;

    /**
     * @param organKey 与前端 organLabelMap 的 key 一致，如 brain、liver、lymph_nodes
     */
    @GetMapping("/organ-diseases")
    public Result<OrganDiseaseBundleVO> listByOrgan(@RequestParam("organKey") String organKey) {
        return Result.success(organDiseaseService.listByOrganKey(organKey));
    }
}

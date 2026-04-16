package com.android.service.impl;

import com.android.dto.AdminRagDrugRowVO;
import com.android.dto.AdminRagInstructionRowVO;
import com.android.dto.PageResultVO;
import com.android.entity.RagKbDrug;
import com.android.entity.RagKbEnDoc;
import com.android.entity.RagKbLiverCancer;
import com.android.entity.RagKbLlama;
import com.android.entity.RagKbQa;
import com.android.mapper.RagKbDrugMapper;
import com.android.mapper.RagKbEnDocMapper;
import com.android.mapper.RagKbLiverCancerMapper;
import com.android.mapper.RagKbLlamaMapper;
import com.android.mapper.RagKbQaMapper;
import com.android.service.IRagKbAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagKbAdminServiceImpl implements IRagKbAdminService {

    private final RagKbQaMapper ragKbQaMapper;
    private final RagKbLiverCancerMapper ragKbLiverCancerMapper;
    private final RagKbLlamaMapper ragKbLlamaMapper;
    private final RagKbDrugMapper ragKbDrugMapper;
    private final RagKbEnDocMapper ragKbEnDocMapper;

    @Override
    public PageResultVO<AdminRagInstructionRowVO> pageQa(long pageNum, long pageSize, String keyword) {
        long pn = Math.max(1, pageNum);
        long ps = Math.min(100, Math.max(1, pageSize));
        Page<RagKbQa> page = new Page<>(pn, ps);
        LambdaQueryWrapper<RagKbQa> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            q.and(w -> w.like(RagKbQa::getInstruction, k).or().like(RagKbQa::getOutput, k));
        }
        q.orderByDesc(RagKbQa::getId);
        Page<RagKbQa> out = ragKbQaMapper.selectPage(page, q);
        return toInstructionPage(out, pn, ps);
    }

    @Override
    public PageResultVO<AdminRagInstructionRowVO> pageLiverCancer(long pageNum, long pageSize, String keyword) {
        long pn = Math.max(1, pageNum);
        long ps = Math.min(100, Math.max(1, pageSize));
        Page<RagKbLiverCancer> page = new Page<>(pn, ps);
        LambdaQueryWrapper<RagKbLiverCancer> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            q.and(w -> w.like(RagKbLiverCancer::getInstruction, k).or().like(RagKbLiverCancer::getOutput, k));
        }
        q.orderByDesc(RagKbLiverCancer::getId);
        Page<RagKbLiverCancer> out = ragKbLiverCancerMapper.selectPage(page, q);
        List<AdminRagInstructionRowVO> records = out.getRecords().stream()
                .map(r -> new AdminRagInstructionRowVO(r.getId(), r.getInstruction(), r.getOutput(), r.getCreatedAt()))
                .collect(Collectors.toList());
        return new PageResultVO<>(records, out.getTotal(), pn, ps);
    }

    @Override
    public PageResultVO<AdminRagInstructionRowVO> pageLlama(long pageNum, long pageSize, String keyword) {
        long pn = Math.max(1, pageNum);
        long ps = Math.min(100, Math.max(1, pageSize));
        Page<RagKbLlama> page = new Page<>(pn, ps);
        LambdaQueryWrapper<RagKbLlama> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            q.and(w -> w.like(RagKbLlama::getInstruction, k).or().like(RagKbLlama::getOutput, k));
        }
        q.orderByDesc(RagKbLlama::getId);
        Page<RagKbLlama> out = ragKbLlamaMapper.selectPage(page, q);
        List<AdminRagInstructionRowVO> records = out.getRecords().stream()
                .map(r -> new AdminRagInstructionRowVO(r.getId(), r.getInstruction(), r.getOutput(), r.getCreatedAt()))
                .collect(Collectors.toList());
        return new PageResultVO<>(records, out.getTotal(), pn, ps);
    }

    @Override
    public PageResultVO<AdminRagDrugRowVO> pageDrug(long pageNum, long pageSize, String keyword) {
        long pn = Math.max(1, pageNum);
        long ps = Math.min(100, Math.max(1, pageSize));
        Page<RagKbDrug> page = new Page<>(pn, ps);
        LambdaQueryWrapper<RagKbDrug> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            q.and(w -> w.like(RagKbDrug::getDrugName, k)
                    .or().like(RagKbDrug::getIndicationText, k)
                    .or().like(RagKbDrug::getDiseasesLabeled, k));
        }
        q.orderByDesc(RagKbDrug::getId);
        Page<RagKbDrug> out = ragKbDrugMapper.selectPage(page, q);
        List<AdminRagDrugRowVO> records = out.getRecords().stream()
                .map(r -> new AdminRagDrugRowVO(
                        r.getId(),
                        r.getDrugName(),
                        r.getIndicationText(),
                        r.getDiseasesLabeled(),
                        r.getCreatedAt()))
                .collect(Collectors.toList());
        return new PageResultVO<>(records, out.getTotal(), pn, ps);
    }

    @Override
    public PageResultVO<AdminRagInstructionRowVO> pageEnglish(long pageNum, long pageSize, String keyword) {
        long pn = Math.max(1, pageNum);
        long ps = Math.min(100, Math.max(1, pageSize));
        Page<RagKbEnDoc> page = new Page<>(pn, ps);
        LambdaQueryWrapper<RagKbEnDoc> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            q.and(w -> w.like(RagKbEnDoc::getInput, k).or().like(RagKbEnDoc::getOutput, k));
        }
        q.orderByDesc(RagKbEnDoc::getId);
        Page<RagKbEnDoc> out = ragKbEnDocMapper.selectPage(page, q);
        List<AdminRagInstructionRowVO> records = out.getRecords().stream()
                .map(r -> new AdminRagInstructionRowVO(r.getId(), r.getInput(), r.getOutput(), r.getCreatedAt()))
                .collect(Collectors.toList());
        return new PageResultVO<>(records, out.getTotal(), pn, ps);
    }

    private PageResultVO<AdminRagInstructionRowVO> toInstructionPage(Page<RagKbQa> out, long pn, long ps) {
        List<AdminRagInstructionRowVO> records = out.getRecords().stream()
                .map(r -> new AdminRagInstructionRowVO(r.getId(), r.getInstruction(), r.getOutput(), r.getCreatedAt()))
                .collect(Collectors.toList());
        return new PageResultVO<>(records, out.getTotal(), pn, ps);
    }
}

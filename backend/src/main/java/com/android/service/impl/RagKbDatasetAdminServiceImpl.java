package com.android.service.impl;

import com.android.dto.RagKbDrugMutateDTO;
import com.android.dto.RagKbEnDocMutateDTO;
import com.android.dto.RagKbInstructionMutateDTO;
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
import com.android.service.IRagKbDatasetAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RagKbDatasetAdminServiceImpl implements IRagKbDatasetAdminService {

    private final RagKbQaMapper ragKbQaMapper;
    private final RagKbLiverCancerMapper ragKbLiverCancerMapper;
    private final RagKbLlamaMapper ragKbLlamaMapper;
    private final RagKbDrugMapper ragKbDrugMapper;
    private final RagKbEnDocMapper ragKbEnDocMapper;

    @Override
    public Long createQa(RagKbInstructionMutateDTO dto) {
        RagKbQa e = new RagKbQa();
        e.setInstruction(nz(dto.getInstruction()));
        e.setOutput(dto.getOutput());
        ragKbQaMapper.insert(e);
        return e.getId();
    }

    @Override
    public void updateQa(Long id, RagKbInstructionMutateDTO dto) {
        RagKbQa e = new RagKbQa();
        e.setId(id);
        e.setInstruction(nz(dto.getInstruction()));
        e.setOutput(dto.getOutput());
        ragKbQaMapper.updateById(e);
    }

    @Override
    public void deleteQa(Long id) {
        ragKbQaMapper.deleteById(id);
    }

    @Override
    public Long createLiver(RagKbInstructionMutateDTO dto) {
        RagKbLiverCancer e = new RagKbLiverCancer();
        e.setInstruction(nz(dto.getInstruction()));
        e.setOutput(dto.getOutput());
        ragKbLiverCancerMapper.insert(e);
        return e.getId();
    }

    @Override
    public void updateLiver(Long id, RagKbInstructionMutateDTO dto) {
        RagKbLiverCancer e = new RagKbLiverCancer();
        e.setId(id);
        e.setInstruction(nz(dto.getInstruction()));
        e.setOutput(dto.getOutput());
        ragKbLiverCancerMapper.updateById(e);
    }

    @Override
    public void deleteLiver(Long id) {
        ragKbLiverCancerMapper.deleteById(id);
    }

    @Override
    public Long createLlama(RagKbInstructionMutateDTO dto) {
        RagKbLlama e = new RagKbLlama();
        e.setInstruction(nz(dto.getInstruction()));
        e.setOutput(dto.getOutput());
        ragKbLlamaMapper.insert(e);
        return e.getId();
    }

    @Override
    public void updateLlama(Long id, RagKbInstructionMutateDTO dto) {
        RagKbLlama e = new RagKbLlama();
        e.setId(id);
        e.setInstruction(nz(dto.getInstruction()));
        e.setOutput(dto.getOutput());
        ragKbLlamaMapper.updateById(e);
    }

    @Override
    public void deleteLlama(Long id) {
        ragKbLlamaMapper.deleteById(id);
    }

    @Override
    public Long createDrug(RagKbDrugMutateDTO dto) {
        RagKbDrug e = new RagKbDrug();
        e.setDrugName(nz(dto.getDrugName()));
        e.setIndicationText(dto.getIndicationText());
        e.setDiseasesLabeled(dto.getDiseasesLabeled());
        ragKbDrugMapper.insert(e);
        return e.getId();
    }

    @Override
    public void updateDrug(Long id, RagKbDrugMutateDTO dto) {
        RagKbDrug e = new RagKbDrug();
        e.setId(id);
        e.setDrugName(nz(dto.getDrugName()));
        e.setIndicationText(dto.getIndicationText());
        e.setDiseasesLabeled(dto.getDiseasesLabeled());
        ragKbDrugMapper.updateById(e);
    }

    @Override
    public void deleteDrug(Long id) {
        ragKbDrugMapper.deleteById(id);
    }

    @Override
    public Long createEnglish(RagKbEnDocMutateDTO dto) {
        RagKbEnDoc e = new RagKbEnDoc();
        e.setInput(nz(dto.getInput()));
        e.setOutput(dto.getOutput());
        ragKbEnDocMapper.insert(e);
        return e.getId();
    }

    @Override
    public void updateEnglish(Long id, RagKbEnDocMutateDTO dto) {
        RagKbEnDoc e = new RagKbEnDoc();
        e.setId(id);
        e.setInput(nz(dto.getInput()));
        e.setOutput(dto.getOutput());
        ragKbEnDocMapper.updateById(e);
    }

    @Override
    public void deleteEnglish(Long id) {
        ragKbEnDocMapper.deleteById(id);
    }

    private static String nz(String s) {
        return StringUtils.hasText(s) ? s.trim() : "";
    }
}

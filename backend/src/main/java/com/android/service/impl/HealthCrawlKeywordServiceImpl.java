package com.android.service.impl;

import com.android.dto.HealthCrawlKeywordSaveDTO;
import com.android.dto.HealthCrawlKeywordVO;
import com.android.entity.HealthCrawlKeyword;
import com.android.mapper.HealthCrawlKeywordMapper;
import com.android.service.IHealthCrawlKeywordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HealthCrawlKeywordServiceImpl implements IHealthCrawlKeywordService {

    private final HealthCrawlKeywordMapper healthCrawlKeywordMapper;

    @Override
    public List<HealthCrawlKeywordVO> listAll() {
        List<HealthCrawlKeyword> rows = healthCrawlKeywordMapper.selectList(
                new LambdaQueryWrapper<HealthCrawlKeyword>()
                        .orderByAsc(HealthCrawlKeyword::getSortOrder)
                        .orderByAsc(HealthCrawlKeyword::getId));
        List<HealthCrawlKeywordVO> out = new ArrayList<>();
        for (HealthCrawlKeyword e : rows) {
            HealthCrawlKeywordVO vo = new HealthCrawlKeywordVO();
            BeanUtils.copyProperties(e, vo);
            out.add(vo);
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(HealthCrawlKeywordSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getKeyword())) {
            throw new IllegalArgumentException("关键词不能为空");
        }
        String kw = dto.getKeyword().trim();
        if (kw.length() > 128) {
            throw new IllegalArgumentException("关键词过长");
        }
        long dup = healthCrawlKeywordMapper.selectCount(
                new LambdaQueryWrapper<HealthCrawlKeyword>().eq(HealthCrawlKeyword::getKeyword, kw));
        if (dup > 0) {
            throw new IllegalArgumentException("关键词已存在");
        }
        HealthCrawlKeyword e = new HealthCrawlKeyword();
        e.setKeyword(kw);
        e.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        e.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : Boolean.TRUE);
        e.setRemark(trimOrNull(dto.getRemark()));
        LocalDateTime now = LocalDateTime.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        healthCrawlKeywordMapper.insert(e);
        return e.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, HealthCrawlKeywordSaveDTO dto) {
        if (id == null || dto == null) {
            throw new IllegalArgumentException("参数无效");
        }
        HealthCrawlKeyword e = healthCrawlKeywordMapper.selectById(id);
        if (e == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            String kw = dto.getKeyword().trim();
            if (kw.length() > 128) {
                throw new IllegalArgumentException("关键词过长");
            }
            long dup = healthCrawlKeywordMapper.selectCount(
                    new LambdaQueryWrapper<HealthCrawlKeyword>()
                            .eq(HealthCrawlKeyword::getKeyword, kw)
                            .ne(HealthCrawlKeyword::getId, id));
            if (dup > 0) {
                throw new IllegalArgumentException("关键词已存在");
            }
            e.setKeyword(kw);
        }
        if (dto.getSortOrder() != null) {
            e.setSortOrder(dto.getSortOrder());
        }
        if (dto.getEnabled() != null) {
            e.setEnabled(dto.getEnabled());
        }
        if (dto.getRemark() != null) {
            e.setRemark(trimOrNull(dto.getRemark()));
        }
        e.setUpdatedAt(LocalDateTime.now());
        healthCrawlKeywordMapper.updateById(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            return;
        }
        healthCrawlKeywordMapper.deleteById(id);
    }

    @Override
    public List<String> resolveSeedKeywordsForCrawl() {
        List<HealthCrawlKeyword> rows = healthCrawlKeywordMapper.selectList(
                new LambdaQueryWrapper<HealthCrawlKeyword>()
                        .eq(HealthCrawlKeyword::getEnabled, true)
                        .orderByAsc(HealthCrawlKeyword::getSortOrder)
                        .orderByAsc(HealthCrawlKeyword::getId));
        Set<String> seen = new LinkedHashSet<>();
        for (HealthCrawlKeyword r : rows) {
            if (r.getKeyword() != null) {
                String k = r.getKeyword().trim();
                if (!k.isEmpty()) {
                    seen.add(k);
                }
            }
        }
        return new ArrayList<>(seen);
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

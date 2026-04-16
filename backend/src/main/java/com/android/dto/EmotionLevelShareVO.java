package com.android.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 情绪区间占比（展示用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionLevelShareVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 区间说明，如 100–80 */
    private String rangeLabel;
    /** 区间名称 */
    private String name;
    /** 该区间记录条数 */
    private long count;
    /** 占有效情绪记录总数的百分比 0–100 */
    private BigDecimal percent;

    public static BigDecimal pct(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}

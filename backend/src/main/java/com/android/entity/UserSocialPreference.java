package com.android.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户社交授权/隐私偏好
 */
@Data
@TableName("user_social_preference")
public class UserSocialPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 是否允许进入推荐池 */
    private Boolean allowRecommend;

    /** 是否允许基于疾病画像匹配 */
    private Boolean allowDiseaseBasedMatch;

    /** 活跃时间窗，如 19:00-22:00 */
    private String activeTimeWindow;

    /** 回复风格，如 fast/normal/slow */
    private String replyStyle;

    /** 逗号分隔的屏蔽用户ID列表 */
    private String blockedUserIds;

    private LocalDateTime updatedAt;
}


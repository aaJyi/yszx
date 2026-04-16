package com.android.dto;

import lombok.Data;

@Data
public class ClubDivisionMemberAdminVO {
    private Long userId;
    private String nickname;
    private String openid;
    private Integer clubIndex;
    private Double centrality;
}

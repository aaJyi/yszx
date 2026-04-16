package com.android.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SocialPeerMessageVO {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private LocalDateTime createdAt;
}

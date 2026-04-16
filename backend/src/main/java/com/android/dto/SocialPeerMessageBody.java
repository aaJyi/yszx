package com.android.dto;

import lombok.Data;

@Data
public class SocialPeerMessageBody {
    private Long fromUserId;
    private Long toUserId;
    private String content;
}

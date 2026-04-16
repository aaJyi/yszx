package com.android.dto;

import lombok.Data;

@Data
public class SocialFollowBody {
    private Long followerUserId;
    private Long followeeUserId;
}

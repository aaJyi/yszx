package com.android.service;

import com.android.dto.SocialPeerMessageVO;
import com.android.dto.SocialPeerUserVO;
import com.android.entity.UserSocialPreference;

import java.util.Map;
import java.util.List;

public interface ISocialPeerService {

    Map<String, Object> listRecommendations(Long userId, Integer page, Integer pageSize);

    UserSocialPreference getOrCreatePreference(Long userId);

    void savePreference(UserSocialPreference pref);

    void follow(Long followerUserId, Long followeeUserId);

    void unfollow(Long followerUserId, Long followeeUserId);

    boolean isMutualFollow(Long a, Long b);

    List<SocialPeerUserVO> listMutualPartners(Long userId);

    List<SocialPeerMessageVO> listConversation(Long userId, Long peerId, Long sinceId, int limit);

    void sendMessage(Long fromUserId, Long toUserId, String content);
}

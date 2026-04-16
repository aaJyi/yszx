package com.android.service;

import com.android.dto.SocialArticleItemVO;

import java.util.List;

public interface ISocialArticleFeedService {

    List<SocialArticleItemVO> listArticles(Long userId);

    /** 按用户当前推断疾病重新抓取并替换资讯列表，返回写入条数 */
    int refreshArticles(Long userId);
}

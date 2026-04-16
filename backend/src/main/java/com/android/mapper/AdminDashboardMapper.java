package com.android.mapper;

import com.android.dto.EmotionBucketRawCounts;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 管理端控制台统计（跨表聚合）
 */
@Mapper
public interface AdminDashboardMapper {

    @Select("SELECT COUNT(*) FROM t_user")
    long countUsers();

    @Select("SELECT COUNT(*) FROM raw_health_data")
    long countRawHealthData();

    @Select("SELECT COUNT(*) FROM family_member")
    long countFamilyMembers();

    @Select("SELECT COUNT(DISTINCT owner_user_id) FROM family_member")
    long countDistinctFamilyOwners();

    @Select("SELECT COUNT(*) FROM emotion_monitoring")
    long countEmotionRecords();

    @Select("SELECT COUNT(DISTINCT user_id) FROM emotion_monitoring")
    long countDistinctEmotionUsers();

    /**
     * 按 COALESCE(average_emotion, today_emotion) 分值落到区间：
     * (80,100] 非常开心；(60,80] 开心；(40,60] 平静；(20,40] 低落；[0,20] 很难过
     */
    @Select("SELECT " +
            "COALESCE(SUM(CASE WHEN s > 80 AND s <= 100 THEN 1 ELSE 0 END), 0) AS veryHappy, " +
            "COALESCE(SUM(CASE WHEN s > 60 AND s <= 80 THEN 1 ELSE 0 END), 0) AS happy, " +
            "COALESCE(SUM(CASE WHEN s > 40 AND s <= 60 THEN 1 ELSE 0 END), 0) AS calm, " +
            "COALESCE(SUM(CASE WHEN s > 20 AND s <= 40 THEN 1 ELSE 0 END), 0) AS low, " +
            "COALESCE(SUM(CASE WHEN s >= 0 AND s <= 20 THEN 1 ELSE 0 END), 0) AS verySad " +
            "FROM ( " +
            "  SELECT CAST(COALESCE(average_emotion, today_emotion) AS DECIMAL(14, 4)) AS s " +
            "  FROM emotion_monitoring " +
            "  WHERE COALESCE(average_emotion, today_emotion) IS NOT NULL " +
            ") t")
    EmotionBucketRawCounts selectEmotionBucketCounts();
}

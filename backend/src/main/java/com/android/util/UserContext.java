package com.android.util;

/**
 * 用户上下文工具类，使用ThreadLocal保存当前登录用户ID
 * 
 * @author sjt
 * @since 2026-01-09
 */
public class UserContext {
    
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    
    /**
     * 设置当前登录用户ID
     * 
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }
    
    /**
     * 获取当前登录用户ID
     * 
     * @return 用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }
    
    /**
     * 清除当前线程的用户ID
     */
    public static void clear() {
        USER_ID.remove();
    }
}

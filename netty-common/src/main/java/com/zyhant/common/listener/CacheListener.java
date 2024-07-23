package com.zyhant.common.listener;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 缓存事件回调
 * @author zyhant
 * @date 2024/7/22 01:27
 */
public interface CacheListener {

    <T> void setCacheObject(String key, T value);

    <T> void setCacheObject(String key, T value, long timeout, TimeUnit timeUnit);

    <T> T getCacheObject(String key);

    void deleteObject(String key);

    void refreshCacheObject(String key);

    Collection<String> keys(String key);

}

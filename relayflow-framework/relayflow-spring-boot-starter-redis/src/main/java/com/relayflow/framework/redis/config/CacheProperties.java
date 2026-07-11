package com.relayflow.framework.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "relayflow.cache")
public class CacheProperties {

    /** 是否启用 Redis 缓存（关闭时所有缓存操作降级为直查）。 */
    private boolean enabled = true;

    /** 默认缓存 TTL（秒）。 */
    private long defaultTtlSeconds = 1800L;

    /** 空值缓存 TTL（秒），用于缓解缓存穿透。 */
    private long nullValueTtlSeconds = 60L;

    /** TTL 随机抖动上限（秒），用于缓解缓存雪崩。 */
    private long ttlJitterSeconds = 300L;

    /** 击穿锁等待时间（毫秒）。 */
    private long lockWaitMillis = 3000L;

    /** 击穿锁租约时间（毫秒）。 */
    private long lockLeaseMillis = 10000L;

    /** 权限码缓存 TTL（秒）。 */
    private long permissionTtlSeconds = 1800L;
}

package com.playtab.cloudgateservice.service;

import com.playtab.cloudgateservice.domain.tag.TagEventRepository;
import com.playtab.cloudgateservice.domain.tag.TagEventType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OccupancyService {

    private static final String KEY_PREFIX = "cloudgate:stage:";
    private static final String KEY_SUFFIX = ":count";

    private final StringRedisTemplate redisTemplate;
    private final TagEventRepository tagEventRepository;

    public OccupancyService(StringRedisTemplate redisTemplate,
                            TagEventRepository tagEventRepository) {
        this.redisTemplate = redisTemplate;
        this.tagEventRepository = tagEventRepository;
    }

    public long update(Long stageId, TagEventType eventType) {
        String key = buildKey(stageId);

        if (eventType == TagEventType.ENTER || eventType == TagEventType.REENTER) {
            Long count = redisTemplate.opsForValue().increment(key);
            return count != null ? count : 0;
        } else {
            Long count = redisTemplate.opsForValue().decrement(key);
            if (count != null && count < 0) {
                redisTemplate.opsForValue().set(key, "0");
                return 0;
            }
            return count != null ? count : 0;
        }
    }

    public long getCount(Long stageId) {
        String key = buildKey(stageId);
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return Long.parseLong(value);
        }

        long count = tagEventRepository.countCurrentOccupancy(stageId);
        redisTemplate.opsForValue().set(key, String.valueOf(count));
        return count;
    }

    public void setCount(Long stageId, long count) {
        redisTemplate.opsForValue().set(buildKey(stageId), String.valueOf(count));
    }

    private String buildKey(Long stageId) {
        return KEY_PREFIX + stageId + KEY_SUFFIX;
    }
}

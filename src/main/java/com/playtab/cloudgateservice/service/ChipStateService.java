package com.playtab.cloudgateservice.service;

import com.playtab.cloudgateservice.domain.tag.TagEventRepository;
import com.playtab.cloudgateservice.domain.tag.TagEventType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class ChipStateService {

    private static final String KEY_PREFIX = "cloudgate:stage:";
    private static final String KEY_INFIX = ":chip:";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final StringRedisTemplate redisTemplate;
    private final TagEventRepository tagEventRepository;

    public ChipStateService(StringRedisTemplate redisTemplate,
                            TagEventRepository tagEventRepository) {
        this.redisTemplate = redisTemplate;
        this.tagEventRepository = tagEventRepository;
    }

    public String getLastEventType(Long stageId, String chipSerial) {
        String cached = redisTemplate.opsForValue().get(buildKey(stageId, chipSerial));
        if (cached != null) return cached;

        LocalDateTime startOfDay = LocalDate.now(ZONE).atStartOfDay();
        return tagEventRepository.findLastEventToday(chipSerial, stageId, startOfDay)
                .map(event -> {
                    setLastEventType(stageId, chipSerial, event.getEventType());
                    return event.getEventType().name();
                })
                .orElse(null);
    }

    public void setLastEventType(Long stageId, String chipSerial, TagEventType eventType) {
        String key = buildKey(stageId, chipSerial);
        redisTemplate.opsForValue().set(key, eventType.name());
        redisTemplate.expireAt(key, toMidnightInstant());
    }

    private String buildKey(Long stageId, String chipSerial) {
        return KEY_PREFIX + stageId + KEY_INFIX + chipSerial;
    }

    private Instant toMidnightInstant() {
        return LocalDate.now(ZONE).plusDays(1).atStartOfDay(ZONE).toInstant();
    }
}

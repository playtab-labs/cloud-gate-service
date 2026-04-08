package com.playtab.cloudgateservice.service;

import com.playtab.cloudgateservice.domain.wristband.WristbandOwnershipRepository;
import com.playtab.cloudgateservice.domain.wristband.WristbandRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class WristbandCacheService {

    private static final String WRISTBAND_PREFIX = "cloudgate:wristband:";
    private static final String LINKED_SUFFIX = ":linked";

    private final StringRedisTemplate redisTemplate;
    private final WristbandRepository wristbandRepository;
    private final WristbandOwnershipRepository ownershipRepository;

    public WristbandCacheService(StringRedisTemplate redisTemplate,
                                 WristbandRepository wristbandRepository,
                                 WristbandOwnershipRepository ownershipRepository) {
        this.redisTemplate = redisTemplate;
        this.wristbandRepository = wristbandRepository;
        this.ownershipRepository = ownershipRepository;
    }

    // --- 팔찌 목록 캐시 ---

    public void cacheWristband(String rfid, LocalDate activeDate) {
        redisTemplate.opsForValue().set(WRISTBAND_PREFIX + rfid, activeDate.toString());
    }

    public Optional<String> getActiveDate(String rfid) {
        String cached = redisTemplate.opsForValue().get(WRISTBAND_PREFIX + rfid);
        if (cached != null) return Optional.of(cached);

        return wristbandRepository.findByRfid(rfid)
                .map(w -> {
                    cacheWristband(w.getRfid(), w.getActiveDate());
                    return w.getActiveDate().toString();
                });
    }

    // --- 퍼스널라이징 캐시 ---

    public void cacheLinked(String rfid, LocalDate activeDate) {
        redisTemplate.opsForValue().set(
                WRISTBAND_PREFIX + rfid + LINKED_SUFFIX, activeDate.toString());
    }

    public Optional<String> getLinkedActiveDate(String rfid) {
        String cached = redisTemplate.opsForValue().get(
                WRISTBAND_PREFIX + rfid + LINKED_SUFFIX);
        if (cached != null) return Optional.of(cached);

        return ownershipRepository.findByWristbandRfid(rfid)
                .map(o -> {
                    LocalDate activeDate = o.getWristband().getActiveDate();
                    cacheLinked(rfid, activeDate);
                    return activeDate.toString();
                });
    }
}

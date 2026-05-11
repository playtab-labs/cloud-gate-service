package com.playtab.cloudgateservice.config;

import com.playtab.cloudgateservice.domain.stage.StageRepository;
import com.playtab.cloudgateservice.domain.tag.TagEventRepository;
import com.playtab.cloudgateservice.service.OccupancyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RedisWarmUp implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisWarmUp.class);
    private static final String WRISTBAND_KEY_PATTERN = "cloudgate:wristband:*";
    private static final String LINKED_SUFFIX = ":linked";

    private final StageRepository stageRepository;
    private final TagEventRepository tagEventRepository;
    private final OccupancyService occupancyService;
    private final StringRedisTemplate redisTemplate;

    public RedisWarmUp(StageRepository stageRepository,
                       TagEventRepository tagEventRepository,
                       OccupancyService occupancyService,
                       StringRedisTemplate redisTemplate) {
        this.stageRepository = stageRepository;
        this.tagEventRepository = tagEventRepository;
        this.occupancyService = occupancyService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        evictWristbandCache();

        stageRepository.findAll().forEach(stage -> {
            long count = tagEventRepository.countCurrentOccupancy(stage.getId());
            occupancyService.setCount(stage.getId(), count);
            log.info("Redis warm-up: stage '{}' (id={}) → count={}", stage.getName(), stage.getId(), count);
        });
    }

    private void evictWristbandCache() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(WRISTBAND_KEY_PATTERN)
                .count(1000)
                .build();

        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (!key.endsWith(LINKED_SUFFIX)) {
                    keys.add(key);
                }
            }
        }

        if (keys.isEmpty()) {
            log.info("Redis warm-up: no wristband cache to evict");
            return;
        }

        Long deleted = redisTemplate.delete(keys);
        log.info("Redis warm-up: evicted {} wristband cache entries", deleted);
    }
}

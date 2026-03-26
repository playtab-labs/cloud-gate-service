package com.playtab.cloudgateservice.config;

import com.playtab.cloudgateservice.domain.stage.StageRepository;
import com.playtab.cloudgateservice.domain.tag.TagEventRepository;
import com.playtab.cloudgateservice.service.OccupancyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RedisWarmUp implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisWarmUp.class);

    private final StageRepository stageRepository;
    private final TagEventRepository tagEventRepository;
    private final OccupancyService occupancyService;

    public RedisWarmUp(StageRepository stageRepository,
                       TagEventRepository tagEventRepository,
                       OccupancyService occupancyService) {
        this.stageRepository = stageRepository;
        this.tagEventRepository = tagEventRepository;
        this.occupancyService = occupancyService;
    }

    @Override
    public void run(ApplicationArguments args) {
        stageRepository.findAll().forEach(stage -> {
            long count = tagEventRepository.countCurrentOccupancy(stage.getId());
            occupancyService.setCount(stage.getId(), count);
            log.info("Redis warm-up: stage '{}' (id={}) → count={}", stage.getName(), stage.getId(), count);
        });
    }
}

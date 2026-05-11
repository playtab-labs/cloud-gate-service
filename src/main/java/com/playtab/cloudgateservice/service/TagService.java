package com.playtab.cloudgateservice.service;

import com.playtab.cloudgateservice.domain.reader.Reader;
import com.playtab.cloudgateservice.domain.reader.ReaderRepository;
import com.playtab.cloudgateservice.domain.reader.ReaderStatus;
import com.playtab.cloudgateservice.domain.tag.TagEvent;
import com.playtab.cloudgateservice.domain.tag.TagEventRepository;
import com.playtab.cloudgateservice.domain.tag.TagEventType;
import com.playtab.cloudgateservice.dto.OccupancyResponse;
import com.playtab.cloudgateservice.dto.TagRequest;
import com.playtab.cloudgateservice.dto.TagResponse;
import com.playtab.cloudgateservice.websocket.OccupancyBroadcaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class TagService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final TagEventRepository tagEventRepository;
    private final ReaderRepository readerRepository;
    private final OccupancyService occupancyService;
    private final OccupancyBroadcaster occupancyBroadcaster;
    private final WristbandCacheService wristbandCacheService;
    private final ChipStateService chipStateService;

    public TagService(TagEventRepository tagEventRepository,
                      ReaderRepository readerRepository,
                      OccupancyService occupancyService,
                      OccupancyBroadcaster occupancyBroadcaster,
                      WristbandCacheService wristbandCacheService,
                      ChipStateService chipStateService) {
        this.tagEventRepository = tagEventRepository;
        this.readerRepository = readerRepository;
        this.occupancyService = occupancyService;
        this.occupancyBroadcaster = occupancyBroadcaster;
        this.wristbandCacheService = wristbandCacheService;
        this.chipStateService = chipStateService;
    }

    @Transactional
    public TagResponse processTag(TagRequest request) {
        Reader reader = findActiveReader(request.readerSerial());
        String chipSerial = request.chipSerial().toUpperCase();
        Long stageId = reader.getStage().getId();

        TagEventType eventType = switch (reader.getDirection()) {
            case IN    -> processEntry(chipSerial, stageId);
            case OUT   -> processExit();
            case RE_IN -> processReentry(chipSerial, stageId);
        };

        TagEvent tagEvent = new TagEvent(chipSerial, reader, eventType);
        tagEventRepository.save(tagEvent);

        chipStateService.setLastEventType(stageId, chipSerial, eventType);

        long newCount = occupancyService.update(stageId, eventType);

        occupancyBroadcaster.broadcast(new OccupancyResponse(
                stageId,
                reader.getStage().getName(),
                newCount,
                reader.getStage().getMaxCapacity(),
                LocalDateTime.now()
        ));

        return new TagResponse(
                tagEvent.getId(),
                chipSerial,
                reader.getSerialNumber(),
                eventType.name(),
                reader.getStage().getName(),
                tagEvent.getTaggedAt()
        );
    }

    private Reader findActiveReader(String readerSerial) {
        Reader reader = readerRepository.findBySerialNumber(readerSerial)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reader not found: " + readerSerial));

        if (reader.getStatus() == ReaderStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Reader is inactive: " + readerSerial);
        }

        return reader;
    }

    private void validateWristband(String chipSerial) {
        String activeDate = wristbandCacheService.getActiveDate(chipSerial)
                .orElseThrow(() -> new IllegalStateException(
                        "Wristband not registered: " + chipSerial));
        if (!activeDate.equals(LocalDate.now(ZONE).toString())) {
            throw new IllegalStateException("Wristband not valid for today: " + chipSerial);
        }
    }

    private TagEventType processEntry(String chipSerial, Long stageId) {
        validateWristband(chipSerial);

        String lastType = chipStateService.getLastEventType(stageId, chipSerial);
        if (lastType != null) {
            if (lastType.equals("ENTER") || lastType.equals("REENTER")) {
                throw new IllegalStateException("Already entered: " + chipSerial);
            }
        }
        return TagEventType.ENTER;
    }

    private TagEventType processExit() {
        return TagEventType.EXIT;
    }

    private TagEventType processReentry(String chipSerial, Long stageId) {
        validateWristband(chipSerial);

        String lastType = chipStateService.getLastEventType(stageId, chipSerial);
        if (lastType == null) {
            throw new IllegalStateException(
                    "No entry/exit record found for today: " + chipSerial);
        }
        if (lastType.equals("ENTER") || lastType.equals("REENTER")) {
            throw new IllegalStateException("Already entered: " + chipSerial);
        }
        return TagEventType.REENTER;
    }
}

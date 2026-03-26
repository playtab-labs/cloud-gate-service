package com.playtab.cloudgateservice.service;

import com.playtab.cloudgateservice.domain.reader.Reader;
import com.playtab.cloudgateservice.domain.reader.ReaderDirection;
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

import java.time.LocalDateTime;

@Service
public class TagService {

    private final TagEventRepository tagEventRepository;
    private final ReaderRepository readerRepository;
    private final OccupancyService occupancyService;
    private final OccupancyBroadcaster occupancyBroadcaster;

    public TagService(TagEventRepository tagEventRepository,
                      ReaderRepository readerRepository,
                      OccupancyService occupancyService,
                      OccupancyBroadcaster occupancyBroadcaster) {
        this.tagEventRepository = tagEventRepository;
        this.readerRepository = readerRepository;
        this.occupancyService = occupancyService;
        this.occupancyBroadcaster = occupancyBroadcaster;
    }

    @Transactional
    public TagResponse processTag(TagRequest request) {
        Reader reader = readerRepository.findBySerialNumber(request.readerSerial())
                .orElseThrow(() -> new IllegalArgumentException(
                        "등록되지 않은 리더기입니다: " + request.readerSerial()));

        if (reader.getStatus() == ReaderStatus.INACTIVE) {
            throw new IllegalStateException(
                    "비활성 상태의 리더기입니다: " + request.readerSerial());
        }

        TagEventType eventType = reader.getDirection() == ReaderDirection.IN
                ? TagEventType.ENTER
                : TagEventType.EXIT;

        String chipSerial = request.chipSerial().toUpperCase();

        TagEvent tagEvent = new TagEvent(chipSerial, reader, eventType);
        tagEventRepository.save(tagEvent);

        Long stageId = reader.getStage().getId();
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
}

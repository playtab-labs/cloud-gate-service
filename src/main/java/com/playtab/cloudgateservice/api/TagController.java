package com.playtab.cloudgateservice.api;

import com.playtab.cloudgateservice.domain.tag.TagEventRepository;
import com.playtab.cloudgateservice.dto.TagRequest;
import com.playtab.cloudgateservice.dto.TagResponse;
import com.playtab.cloudgateservice.service.TagService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;
    private final TagEventRepository tagEventRepository;

    public TagController(TagService tagService, TagEventRepository tagEventRepository) {
        this.tagService = tagService;
        this.tagEventRepository = tagEventRepository;
    }

    @PostMapping
    public ResponseEntity<TagResponse> receiveTag(@Valid @RequestBody TagRequest request) {
        TagResponse response = tagService.processTag(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<TagResponse>> getTags(
            @RequestParam Long stageId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {

        Page<TagResponse> page = tagEventRepository.findByStageAndPeriod(stageId, from, to, pageable)
                .map(e -> new TagResponse(
                        e.getId(),
                        e.getChipSerial(),
                        e.getReader().getSerialNumber(),
                        e.getEventType().name(),
                        e.getReader().getStage().getName(),
                        e.getTaggedAt()
                ));

        return ResponseEntity.ok(page);
    }
}

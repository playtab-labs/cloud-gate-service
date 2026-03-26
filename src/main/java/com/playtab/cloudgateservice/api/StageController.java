package com.playtab.cloudgateservice.api;

import com.playtab.cloudgateservice.domain.stage.Stage;
import com.playtab.cloudgateservice.domain.stage.StageRepository;
import com.playtab.cloudgateservice.dto.OccupancyResponse;
import com.playtab.cloudgateservice.dto.StageRequest;
import com.playtab.cloudgateservice.dto.StageResponse;
import com.playtab.cloudgateservice.service.OccupancyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stages")
public class StageController {

    private final StageRepository stageRepository;
    private final OccupancyService occupancyService;

    public StageController(StageRepository stageRepository, OccupancyService occupancyService) {
        this.stageRepository = stageRepository;
        this.occupancyService = occupancyService;
    }

    @PostMapping
    public ResponseEntity<StageResponse> createStage(@Valid @RequestBody StageRequest request) {
        Stage stage = new Stage(request.name(), request.maxCapacity());
        stageRepository.save(stage);
        return ResponseEntity
                .created(URI.create("/api/v1/stages/" + stage.getId()))
                .body(toResponse(stage));
    }

    @GetMapping
    public ResponseEntity<List<StageResponse>> getAllStages() {
        List<StageResponse> result = stageRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{stageId}")
    public ResponseEntity<StageResponse> getStage(@PathVariable Long stageId) {
        Stage stage = findStage(stageId);
        return ResponseEntity.ok(toResponse(stage));
    }

    @PutMapping("/{stageId}")
    public ResponseEntity<StageResponse> updateStage(@PathVariable Long stageId,
                                                     @Valid @RequestBody StageRequest request) {
        Stage stage = findStage(stageId);
        stage.updateName(request.name());
        stage.updateMaxCapacity(request.maxCapacity());
        stageRepository.save(stage);
        return ResponseEntity.ok(toResponse(stage));
    }

    @DeleteMapping("/{stageId}")
    public ResponseEntity<Void> deleteStage(@PathVariable Long stageId) {
        Stage stage = findStage(stageId);
        stageRepository.delete(stage);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{stageId}/occupancy")
    public ResponseEntity<OccupancyResponse> getOccupancy(@PathVariable Long stageId) {
        Stage stage = findStage(stageId);
        long count = occupancyService.getCount(stageId);
        return ResponseEntity.ok(new OccupancyResponse(
                stage.getId(), stage.getName(), count, stage.getMaxCapacity(), LocalDateTime.now()));
    }

    @GetMapping("/occupancy")
    public ResponseEntity<List<OccupancyResponse>> getAllOccupancy() {
        List<OccupancyResponse> result = stageRepository.findAll().stream()
                .map(stage -> new OccupancyResponse(
                        stage.getId(),
                        stage.getName(),
                        occupancyService.getCount(stage.getId()),
                        stage.getMaxCapacity(),
                        LocalDateTime.now()))
                .toList();
        return ResponseEntity.ok(result);
    }

    private Stage findStage(Long stageId) {
        return stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));
    }

    private StageResponse toResponse(Stage stage) {
        return new StageResponse(stage.getId(), stage.getName(), stage.getMaxCapacity(), stage.getCreatedAt());
    }
}

package com.playtab.cloudgateservice.api;

import com.playtab.cloudgateservice.domain.reader.Reader;
import com.playtab.cloudgateservice.domain.reader.ReaderDirection;
import com.playtab.cloudgateservice.domain.reader.ReaderRepository;
import com.playtab.cloudgateservice.domain.stage.Stage;
import com.playtab.cloudgateservice.domain.stage.StageRepository;
import com.playtab.cloudgateservice.dto.ReaderRequest;
import com.playtab.cloudgateservice.dto.ReaderResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/readers")
public class ReaderController {

    private final ReaderRepository readerRepository;
    private final StageRepository stageRepository;

    public ReaderController(ReaderRepository readerRepository, StageRepository stageRepository) {
        this.readerRepository = readerRepository;
        this.stageRepository = stageRepository;
    }

    @PostMapping
    public ResponseEntity<ReaderResponse> createReader(@Valid @RequestBody ReaderRequest request) {
        Stage stage = stageRepository.findById(request.stageId())
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + request.stageId()));

        ReaderDirection direction = ReaderDirection.valueOf(request.direction().toUpperCase());
        Reader reader = new Reader(request.serialNumber(), stage, direction);
        readerRepository.save(reader);

        return ResponseEntity
                .created(URI.create("/api/v1/readers/" + reader.getId()))
                .body(toResponse(reader));
    }

    @GetMapping
    public ResponseEntity<List<ReaderResponse>> getAllReaders() {
        List<ReaderResponse> result = readerRepository.findAllWithStage().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{readerId}")
    public ResponseEntity<ReaderResponse> getReader(@PathVariable Long readerId) {
        Reader reader = findReader(readerId);
        return ResponseEntity.ok(toResponse(reader));
    }

    @PatchMapping("/{readerId}/activate")
    public ResponseEntity<ReaderResponse> activateReader(@PathVariable Long readerId) {
        Reader reader = findReader(readerId);
        reader.activate();
        readerRepository.save(reader);
        return ResponseEntity.ok(toResponse(reader));
    }

    @PatchMapping("/{readerId}/deactivate")
    public ResponseEntity<ReaderResponse> deactivateReader(@PathVariable Long readerId) {
        Reader reader = findReader(readerId);
        reader.deactivate();
        readerRepository.save(reader);
        return ResponseEntity.ok(toResponse(reader));
    }

    @DeleteMapping("/{readerId}")
    public ResponseEntity<Void> deleteReader(@PathVariable Long readerId) {
        Reader reader = findReader(readerId);
        readerRepository.delete(reader);
        return ResponseEntity.noContent().build();
    }

    private Reader findReader(Long readerId) {
        return readerRepository.findByIdWithStage(readerId)
                .orElseThrow(() -> new IllegalArgumentException("Reader not found: " + readerId));
    }

    private ReaderResponse toResponse(Reader reader) {
        return new ReaderResponse(
                reader.getId(),
                reader.getSerialNumber(),
                reader.getStage().getId(),
                reader.getStage().getName(),
                reader.getDirection().name(),
                reader.getStatus().name(),
                reader.getCreatedAt());
    }
}

package com.playtab.cloudgateservice.api;

import com.playtab.cloudgateservice.domain.wristband.Wristband;
import com.playtab.cloudgateservice.domain.wristband.WristbandRepository;
import com.playtab.cloudgateservice.service.WristbandCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wristbands")
public class WristbandController {

    private final WristbandRepository wristbandRepository;
    private final WristbandCacheService wristbandCacheService;

    public WristbandController(WristbandRepository wristbandRepository,
                               WristbandCacheService wristbandCacheService) {
        this.wristbandRepository = wristbandRepository;
        this.wristbandCacheService = wristbandCacheService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        List<Wristband> wristbands = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "CSV file is empty"));
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length < 2) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Line " + lineNumber + ": invalid format"));
                }

                String rfid = parts[0].trim().toUpperCase();
                if (!rfid.matches("^[0-9A-F]{14}$")) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Line " + lineNumber + ": invalid RFID format: " + rfid));
                }

                LocalDate activeDate;
                try {
                    activeDate = LocalDate.parse(parts[1].trim());
                } catch (Exception e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Line " + lineNumber + ": invalid date format: " + parts[1].trim()));
                }

                wristbands.add(new Wristband(rfid, activeDate));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to read CSV file: " + e.getMessage()));
        }

        if (wristbands.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No wristband data to register"));
        }

        wristbandRepository.saveAll(wristbands);

        for (Wristband w : wristbands) {
            wristbandCacheService.cacheWristband(w.getRfid(), w.getActiveDate());
        }

        return ResponseEntity.ok(Map.of(
                "message", wristbands.size() + " wristbands registered successfully",
                "count", wristbands.size()
        ));
    }
}

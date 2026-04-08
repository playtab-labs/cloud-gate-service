package com.playtab.cloudgateservice.domain.tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TagEventRepository extends JpaRepository<TagEvent, Long> {

    @Query("""
        SELECT t FROM TagEvent t
        WHERE t.reader.stage.id = :stageId
        AND t.taggedAt BETWEEN :from AND :to
        ORDER BY t.taggedAt DESC
        """)
    Page<TagEvent> findByStageAndPeriod(
            @Param("stageId") Long stageId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("""
        SELECT COUNT(DISTINCT t.chipSerial)
        FROM TagEvent t
        WHERE t.reader.stage.id = :stageId
        AND (t.eventType = com.playtab.cloudgateservice.domain.tag.TagEventType.ENTER
             OR t.eventType = com.playtab.cloudgateservice.domain.tag.TagEventType.REENTER)
        AND t.chipSerial NOT IN (
            SELECT t2.chipSerial FROM TagEvent t2
            WHERE t2.reader.stage.id = :stageId
            AND t2.eventType = com.playtab.cloudgateservice.domain.tag.TagEventType.EXIT
            AND t2.taggedAt > t.taggedAt
        )
        """)
    long countCurrentOccupancy(@Param("stageId") Long stageId);

    @Query("""
        SELECT t FROM TagEvent t
        WHERE t.chipSerial = :chipSerial
        AND t.reader.stage.id = :stageId
        AND t.taggedAt >= :startOfDay
        ORDER BY t.taggedAt DESC
        LIMIT 1
        """)
    Optional<TagEvent> findLastEventToday(
            @Param("chipSerial") String chipSerial,
            @Param("stageId") Long stageId,
            @Param("startOfDay") LocalDateTime startOfDay);
}

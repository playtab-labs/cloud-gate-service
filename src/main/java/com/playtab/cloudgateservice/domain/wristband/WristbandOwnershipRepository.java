package com.playtab.cloudgateservice.domain.wristband;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WristbandOwnershipRepository extends JpaRepository<WristbandOwnership, Long> {

    @Query("SELECT o FROM WristbandOwnership o JOIN FETCH o.wristband WHERE o.identityId = :identityId ORDER BY o.wristband.activeDate ASC")
    List<WristbandOwnership> findByIdentityIdWithWristband(@Param("identityId") UUID identityId);

    long countByIdentityId(UUID identityId);

    boolean existsByWristbandId(Long wristbandId);

    @Query("""
        SELECT o FROM WristbandOwnership o
        JOIN FETCH o.wristband
        WHERE o.wristband.rfid = :rfid
        """)
    Optional<WristbandOwnership> findByWristbandRfid(@Param("rfid") String rfid);

    @Query("""
        SELECT COUNT(o) > 0 FROM WristbandOwnership o
        WHERE o.identityId = :identityId
        AND o.wristband.activeDate = :activeDate
        """)
    boolean existsByIdentityIdAndActiveDate(@Param("identityId") UUID identityId,
                                            @Param("activeDate") LocalDate activeDate);
}

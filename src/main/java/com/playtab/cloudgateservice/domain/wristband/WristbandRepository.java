package com.playtab.cloudgateservice.domain.wristband;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WristbandRepository extends JpaRepository<Wristband, Long> {

    Optional<Wristband> findByRfid(String rfid);
}

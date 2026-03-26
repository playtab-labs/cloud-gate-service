package com.playtab.cloudgateservice.domain.reader;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReaderRepository extends JpaRepository<Reader, Long> {

    @Query("SELECT r FROM Reader r JOIN FETCH r.stage WHERE r.serialNumber = :serialNumber")
    Optional<Reader> findBySerialNumber(@Param("serialNumber") String serialNumber);

    @Query("SELECT r FROM Reader r JOIN FETCH r.stage WHERE r.id = :id")
    Optional<Reader> findByIdWithStage(@Param("id") Long id);

    @Query("SELECT r FROM Reader r JOIN FETCH r.stage")
    List<Reader> findAllWithStage();
}

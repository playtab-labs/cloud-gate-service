package com.playtab.cloudgateservice.domain.wristband;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "wristband")
public class Wristband {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 14)
    private String rfid;

    @Column(nullable = false)
    private LocalDate activeDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Wristband() {}

    public Wristband(String rfid, LocalDate activeDate) {
        this.rfid = rfid;
        this.activeDate = activeDate;
    }

    public Long getId() { return id; }
    public String getRfid() { return rfid; }
    public LocalDate getActiveDate() { return activeDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

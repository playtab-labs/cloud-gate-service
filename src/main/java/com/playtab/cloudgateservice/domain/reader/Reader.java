package com.playtab.cloudgateservice.domain.reader;

import com.playtab.cloudgateservice.domain.stage.Stage;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reader")
public class Reader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReaderDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReaderStatus status = ReaderStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Reader() {}

    public Reader(String serialNumber, Stage stage, ReaderDirection direction) {
        this.serialNumber = serialNumber;
        this.stage = stage;
        this.direction = direction;
    }

    public Long getId() { return id; }
    public String getSerialNumber() { return serialNumber; }
    public Stage getStage() { return stage; }
    public ReaderDirection getDirection() { return direction; }
    public ReaderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void deactivate() { this.status = ReaderStatus.INACTIVE; }
    public void activate() { this.status = ReaderStatus.ACTIVE; }
}

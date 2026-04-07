package com.playtab.cloudgateservice.domain.wristband;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wristband_ownership")
public class WristbandOwnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID identityId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wristband_id", nullable = false, unique = true)
    private Wristband wristband;

    @Column(nullable = false, updatable = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    protected WristbandOwnership() {}

    public WristbandOwnership(UUID identityId, Wristband wristband) {
        this.identityId = identityId;
        this.wristband = wristband;
    }

    public Long getId() { return id; }
    public UUID getIdentityId() { return identityId; }
    public Wristband getWristband() { return wristband; }
    public LocalDateTime getLinkedAt() { return linkedAt; }
}

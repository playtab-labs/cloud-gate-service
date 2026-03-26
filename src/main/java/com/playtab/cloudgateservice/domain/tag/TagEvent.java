package com.playtab.cloudgateservice.domain.tag;

import com.playtab.cloudgateservice.domain.reader.Reader;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tag_event")
public class TagEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 14)
    private String chipSerial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TagEventType eventType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime taggedAt = LocalDateTime.now();

    protected TagEvent() {}

    public TagEvent(String chipSerial, Reader reader, TagEventType eventType) {
        this.chipSerial = chipSerial;
        this.reader = reader;
        this.eventType = eventType;
    }

    public Long getId() { return id; }
    public String getChipSerial() { return chipSerial; }
    public Reader getReader() { return reader; }
    public TagEventType getEventType() { return eventType; }
    public LocalDateTime getTaggedAt() { return taggedAt; }
}

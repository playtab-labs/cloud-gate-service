package com.playtab.cloudgateservice.domain.stage;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stage")
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    private Integer maxCapacity;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Stage() {}

    public Stage(String name, Integer maxCapacity) {
        this.name = name;
        this.maxCapacity = maxCapacity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getMaxCapacity() { return maxCapacity; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateName(String name) { this.name = name; }
    public void updateMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
}

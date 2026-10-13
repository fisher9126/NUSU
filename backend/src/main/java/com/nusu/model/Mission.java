package com.nusu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "missions")
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionPriority priority = MissionPriority.MEDIUM;

    private String launchDate;

    private String returnDate;

    private double budgetMillions = 150.0;

    @Column(length = 1000)
    private String primaryObjective;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "spaceship_id")
    private Spaceship spaceship;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public MissionStatus getStatus() { return status; }
    public void setStatus(MissionStatus status) { this.status = status; }
    public MissionPriority getPriority() { return priority; }
    public void setPriority(MissionPriority priority) { this.priority = priority; }
    public String getLaunchDate() { return launchDate; }
    public void setLaunchDate(String launchDate) { this.launchDate = launchDate; }
    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }
    public double getBudgetMillions() { return budgetMillions; }
    public void setBudgetMillions(double budgetMillions) { this.budgetMillions = budgetMillions; }
    public String getPrimaryObjective() { return primaryObjective; }
    public void setPrimaryObjective(String primaryObjective) { this.primaryObjective = primaryObjective; }
    public Spaceship getSpaceship() { return spaceship; }
    public void setSpaceship(Spaceship spaceship) { this.spaceship = spaceship; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

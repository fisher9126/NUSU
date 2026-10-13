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

@Entity
@Table(name = "astronauts")
public class Astronaut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String rankName;

    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AstronautStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "spaceship_id")
    private Spaceship spaceship;

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRankName() { return rankName; }
    public void setRankName(String rankName) { this.rankName = rankName; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public AstronautStatus getStatus() { return status; }
    public void setStatus(AstronautStatus status) { this.status = status; }
    public Spaceship getSpaceship() { return spaceship; }
    public void setSpaceship(Spaceship spaceship) { this.spaceship = spaceship; }
}

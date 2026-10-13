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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    private int spaceHours = 120; // Giờ ngoài không gian

    private int heartRate = 72; // BPM nhịp tim sinh tồn

    private int oxygenSaturation = 98; // SpO2 (%)

    private double radiationLevel = 1.2; // Bức xạ phơi nhiễm (mSv)

    private String bloodType = "O+";

    @Column(length = 1000)
    private String bio;

    private String badges = "CREW_CERTIFIED"; // Phân tách dấu phẩy

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
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }
    public int getSpaceHours() { return spaceHours; }
    public void setSpaceHours(int spaceHours) { this.spaceHours = spaceHours; }
    public int getHeartRate() { return heartRate; }
    public void setHeartRate(int heartRate) { this.heartRate = heartRate; }
    public int getOxygenSaturation() { return oxygenSaturation; }
    public void setOxygenSaturation(int oxygenSaturation) { this.oxygenSaturation = oxygenSaturation; }
    public double getRadiationLevel() { return radiationLevel; }
    public void setRadiationLevel(double radiationLevel) { this.radiationLevel = radiationLevel; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getBadges() { return badges; }
    public void setBadges(String badges) { this.badges = badges; }
}

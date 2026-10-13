package com.nusu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "spaceships")
public class Spaceship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipStatus status;

    private Integer fuelLevel = 100; // 0 - 100%

    private Integer oxygenLevel = 100; // 0 - 100%

    private Double speedKmS = 7.8; // km/s

    private Double cargoCapacityTons = 10.0; // Tấn tải trọng

    private String manufacturer = "NUSU Aerospace";

    private Integer launchYear = 2024;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public ShipStatus getStatus() { return status; }
    public void setStatus(ShipStatus status) { this.status = status; }

    public int getFuelLevel() { return fuelLevel != null ? fuelLevel : 100; }
    public void setFuelLevel(Integer fuelLevel) { this.fuelLevel = fuelLevel; }

    public int getOxygenLevel() { return oxygenLevel != null ? oxygenLevel : 100; }
    public void setOxygenLevel(Integer oxygenLevel) { this.oxygenLevel = oxygenLevel; }

    public double getSpeedKmS() { return speedKmS != null ? speedKmS : 7.8; }
    public void setSpeedKmS(Double speedKmS) { this.speedKmS = speedKmS; }

    public double getCargoCapacityTons() { return cargoCapacityTons != null ? cargoCapacityTons : 10.0; }
    public void setCargoCapacityTons(Double cargoCapacityTons) { this.cargoCapacityTons = cargoCapacityTons; }

    public String getManufacturer() { return manufacturer != null ? manufacturer : "NUSU Aerospace"; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public Integer getLaunchYear() { return launchYear != null ? launchYear : 2024; }
    public void setLaunchYear(Integer launchYear) { this.launchYear = launchYear; }
}

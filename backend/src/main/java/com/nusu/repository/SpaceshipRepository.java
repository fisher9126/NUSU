package com.nusu.repository;

import com.nusu.model.ShipStatus;
import com.nusu.model.Spaceship;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceshipRepository extends JpaRepository<Spaceship, Long> {
    long countByStatus(ShipStatus status);
}

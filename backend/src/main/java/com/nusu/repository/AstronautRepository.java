package com.nusu.repository;

import com.nusu.model.Astronaut;
import com.nusu.model.AstronautStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AstronautRepository extends JpaRepository<Astronaut, Long> {
    List<Astronaut> findBySpaceshipId(Long spaceshipId);
    long countBySpaceshipId(Long spaceshipId);
    long countByStatus(AstronautStatus status);
}

package com.nusu.repository;

import com.nusu.model.Mission;
import com.nusu.model.MissionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    Optional<Mission> findByCode(String code);
    long countByStatus(MissionStatus status);
    List<Mission> findBySpaceshipId(Long spaceshipId);
}

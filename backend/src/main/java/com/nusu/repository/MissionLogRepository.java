package com.nusu.repository;

import com.nusu.model.MissionLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionLogRepository extends JpaRepository<MissionLog, Long> {
    List<MissionLog> findByMissionIdOrderByTimestampDesc(Long missionId);
    List<MissionLog> findTop20ByOrderByTimestampDesc();
}

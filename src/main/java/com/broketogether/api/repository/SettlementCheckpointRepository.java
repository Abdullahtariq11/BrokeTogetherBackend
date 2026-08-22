package com.broketogether.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.broketogether.api.model.SettlementCheckpoint;

@Repository
public interface SettlementCheckpointRepository extends JpaRepository<SettlementCheckpoint, Long> {

  Optional<SettlementCheckpoint> findByUserIdAndHomeId(Long userId, Long homeId);

}

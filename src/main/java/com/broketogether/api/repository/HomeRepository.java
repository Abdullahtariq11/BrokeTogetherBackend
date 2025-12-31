package com.broketogether.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.broketogether.api.model.Home;

@Repository
public interface HomeRepository extends JpaRepository<Home, Long> {

  /**
   * @param inviteCode is used to find a home
   * @return a home or a null value
   */
  Optional<Home> findByInviteCode(String inviteCode);

}

package com.broketogether.api.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;

@Repository
public interface HomeRepository extends JpaRepository<Home, Long> {

  /**
   * @param inviteCode is used to find a home
   * @return a home or a null value
   */
  Optional<Home> findByInviteCode(String inviteCode);

  // Find homes where user is a member
  Set<Home> findByMembersContaining(User user);

  // Find homes created by a specific user
  Set<Home> findByCreatorId(Long creatorId);

}

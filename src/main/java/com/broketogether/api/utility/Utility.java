package com.broketogether.api.utility;

import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.security.auth.login.AccountNotFoundException;
import java.util.Objects;

public abstract class Utility {
    /**
     * @return User logged in
     */
    protected User getUserDetails() throws AccountNotFoundException {
        User userDetails = (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        if (userDetails == null) {
            throw new AccountNotFoundException("User not found");
        }
        return userDetails;
    }

    /**
     * @return true/false depend on if user is part of home
     *
     */
    protected Boolean checkUserMemberOfHome(Home home, User userDetails) throws RuntimeException{
        boolean isMember= home.getMembers().stream()
                .anyMatch(member -> member.getId().equals(userDetails.getId()));
        if (!isMember) {
            throw new RuntimeException("You are not a member of this home");
        }
        return true;
    }

}

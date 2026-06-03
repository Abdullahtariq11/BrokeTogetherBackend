package com.broketogether.api.utility;

import com.broketogether.api.exception.ForbiddenException;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ForbiddenException("You are not a member of this home");
        }
        return true;
    }

    protected void requirePremium(User user){
        if(!user.getPremium()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This feature requires a Premium subscription."
            );
        }
    }

}

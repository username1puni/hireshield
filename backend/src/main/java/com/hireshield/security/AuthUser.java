package com.hireshield.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.hireshield.entity.Role;
import com.hireshield.entity.User;

/**
 * The logged-in user as Spring Security sees it. Keeps the id so controllers
 * do not need another database call.
 */
public class AuthUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String name;
    private final String passwordHash;
    private final Role role;

    public AuthUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}

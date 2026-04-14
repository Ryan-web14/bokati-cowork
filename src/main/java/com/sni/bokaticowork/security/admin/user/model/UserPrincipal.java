package com.sni.bokaticowork.security.admin.user.model;

import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;



public class UserPrincipal implements UserDetails, CredentialsContainer {

    public UserPrincipal() {}

    public UserPrincipal(Users user) {
        this.user = user;
    }

    @Getter
    private Users user;




    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return !Boolean.TRUE.equals(user.getIsAccountExpired());
    }

    @Override
    public boolean isAccountNonLocked() {
        return !Boolean.TRUE.equals(user.getIsAccountLocked());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getIsAccountEnabled());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        if(user.getRoleUsers() == null){
            return List.of();
        }
        return user.getRoleUsers().stream()
                .map(roleUser -> new SimpleGrantedAuthority("ROLE_" + roleUser.getRole().getName()))
                .collect(Collectors.toList());
    }

    @Override
    public void eraseCredentials() {
        user.setPasswordHash("");
    }
}



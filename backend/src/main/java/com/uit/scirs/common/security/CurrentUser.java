package com.uit.scirs.common.security;

import com.uit.scirs.user.entity.RoleName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class CurrentUser {

    private final Long id;
    private final String email;
    private final RoleName role;
    private final Long departmentId;

    public CurrentUser(Long id, String email, RoleName role, Long departmentId) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.departmentId = departmentId;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public RoleName getRole() {
        return role;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}

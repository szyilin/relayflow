package com.relayflow.framework.security.core;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class LoginUser implements UserDetails {

    public static final String CLAIM_TENANT_ID = "tenant_id";
    public static final String CLAIM_USER_TYPE = "user_type";

    private final Long userId;
    private final String username;
    private final Long tenantId;
    private final String userType;
    private final List<GrantedAuthority> authorities;

    public LoginUser(Long userId, String username, Long tenantId, String userType,
                     Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.userType = userType;
        this.authorities = authorities == null || authorities.isEmpty()
                ? Collections.emptyList()
                : List.copyOf(authorities);
    }

    public static List<GrantedAuthority> toAuthorities(Set<String> permissionCodes) {
        if (CollectionUtils.isEmpty(permissionCodes)) {
            return Collections.emptyList();
        }
        return permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

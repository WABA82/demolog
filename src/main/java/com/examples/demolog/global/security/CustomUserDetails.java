package com.examples.demolog.global.security;

import com.examples.demolog.domains.auth.model.AppUser;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;
import java.util.UUID;

@Getter
public class CustomUserDetails implements UserDetails, CredentialsContainer {

    private final UUID userId;

    private final String email;

    private String password;

    private final String nickname;

    private final Set<GrantedAuthority> authorities;

    private final boolean enabled;

    private final boolean accountNonExpired;

    private final boolean accountNonLocked;

    private final boolean credentialsNonExpired;

    // Builder 패턴 사용 권장
    @Builder
    private CustomUserDetails(
            UUID userId,
            String email,
            String password,
            String nickname,
            Set<GrantedAuthority> authorities,
            boolean enabled,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired
    ) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.authorities = authorities;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public static CustomUserDetails from(AppUser user) {
        return CustomUserDetails.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

}

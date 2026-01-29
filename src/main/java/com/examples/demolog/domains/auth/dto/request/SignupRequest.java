package com.examples.demolog.domains.auth.dto.request;

import com.examples.demolog.domains.auth.model.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @Email
        @NotBlank
        String email,
        @NotBlank
        String password,
        @NotBlank
        String nickname
) {

    public AppUser toEntity() {
        return AppUser.create(email, password, nickname);
    }

}

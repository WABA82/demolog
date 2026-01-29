package com.examples.demolog.domains.auth.dto.response;


import com.examples.demolog.domains.auth.model.AppUser;

public record AppUserResponse(
        String email,
        String nickname
) {

    public static AppUserResponse from(AppUser user) {
        return new AppUserResponse(
                user.getEmail(),
                user.getNickname()
        );
    }

}

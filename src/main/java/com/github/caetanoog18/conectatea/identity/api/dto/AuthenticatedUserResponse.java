package com.github.caetanoog18.conectatea.identity.api.dto;

import java.util.List;

public record AuthenticatedUserResponse(
        String email,
        List<String> roles) {
}

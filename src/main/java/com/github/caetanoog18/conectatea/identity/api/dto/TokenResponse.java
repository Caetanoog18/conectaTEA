package com.github.caetanoog18.conectatea.identity.api.dto;

public record TokenResponse(
        String accessToken, String tokenType, long expiresIn) {

}

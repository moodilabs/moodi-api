package com.moodi.member.application.dto;

/**
 * @param audience id_token의 {@code aud} — 토큰을 발급받은 client_id. 제공자 토큰 교환·철회 때 같은 값을 써야 한다.
 */
public record OidcPayload(String providerId, String email, String audience) {

    public OidcPayload(String providerId, String email) {
        this(providerId, email, null);
    }
}

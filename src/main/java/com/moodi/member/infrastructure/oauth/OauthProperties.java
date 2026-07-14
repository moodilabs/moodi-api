package com.moodi.member.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("oauth")
public record OauthProperties(Provider google, Provider apple) {

    public record Provider(String issuer, String audience, String jwksUri) {
    }
}

package com.moodi.member.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("oauth")
public record OauthProperties(Provider google, Provider apple) {

    public record Provider(String issuer, List<String> audiences, String jwksUri) {
    }
}

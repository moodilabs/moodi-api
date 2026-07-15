package com.moodi.member.infrastructure.oauth;

import com.moodi.member.application.OAuthClient;
import com.moodi.member.application.dto.OidcPayload;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OidcTokenVerifier implements OAuthClient {

    private final Map<OAuthProvider, DefaultJWTProcessor<SecurityContext>> processors;

    public OidcTokenVerifier(OauthProperties properties) {
        this.processors = Map.of(
                OAuthProvider.GOOGLE, buildProcessor(properties.google()),
                OAuthProvider.APPLE, buildProcessor(properties.apple())
        );
    }

    @Override
    public OidcPayload verify(OAuthProvider provider, String idToken) {
        try {
            JWTClaimsSet claims = processors.get(provider).process(idToken, null);
            return new OidcPayload(claims.getSubject(), claims.getStringClaim("email"));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
    }

    private DefaultJWTProcessor<SecurityContext> buildProcessor(OauthProperties.Provider provider) {
        JWKSource<SecurityContext> jwkSource = createJwkSource(provider.jwksUri());
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
        Set<String> audiences = acceptedAudiences(provider.audiences());
        processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<SecurityContext>(
                audiences,
                new JWTClaimsSet.Builder().issuer(provider.issuer()).build(),
                Set.of("sub", "exp"),
                null
        ));
        return processor;
    }

    private Set<String> acceptedAudiences(List<String> audiences) {
        if (audiences == null) {
            return null;
        }
        Set<String> cleaned = audiences.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());
        return cleaned.isEmpty() ? null : cleaned;
    }

    private JWKSource<SecurityContext> createJwkSource(String jwksUri) {
        try {
            return JWKSourceBuilder.create(URI.create(jwksUri).toURL()).build();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid JWKS URI: " + jwksUri, e);
        }
    }
}

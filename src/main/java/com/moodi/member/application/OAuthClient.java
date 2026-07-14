package com.moodi.member.application;

import com.moodi.member.application.dto.OidcPayload;
import com.moodi.member.domain.OAuthProvider;

public interface OAuthClient {

    OidcPayload verify(OAuthProvider provider, String idToken);
}

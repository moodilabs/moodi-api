package com.moodi.member.domain;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(UUID id);

    Optional<Member> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    boolean existsByEmail(String email);
}

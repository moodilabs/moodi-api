package com.moodi.member.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberAgreement extends BaseEntity {

    private UUID id;
    private UUID memberId;
    private AgreementType type;
    private boolean agreed;
    private LocalDateTime agreedAt;

    private MemberAgreement(UUID memberId, AgreementType type, boolean agreed, LocalDateTime agreedAt) {
        this.memberId = memberId;
        this.type = type;
        this.agreed = agreed;
        this.agreedAt = agreedAt;
    }

    public static MemberAgreement of(UUID memberId, AgreementType type, boolean agreed, LocalDateTime now) {
        return new MemberAgreement(memberId, type, agreed, agreed ? now : null);
    }
}

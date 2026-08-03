package com.moodi.member.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.mood.MoodTag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPreferredMood extends BaseEntity {

    private UUID id;
    private UUID memberId;
    private MoodTag mood;

    private MemberPreferredMood(UUID memberId, MoodTag mood) {
        this.memberId = memberId;
        this.mood = mood;
    }

    public static MemberPreferredMood of(UUID memberId, MoodTag mood) {
        return new MemberPreferredMood(memberId, mood);
    }
}

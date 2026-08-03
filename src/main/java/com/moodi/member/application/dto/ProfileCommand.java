package com.moodi.member.application.dto;

import com.moodi.member.domain.Gender;

public record ProfileCommand(String nickname, String country, Integer birthYear, Gender gender) {
}

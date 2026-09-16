package com.moodi.member.infrastructure.persistence;

import com.moodi.member.domain.SurveyImage;
import com.moodi.member.domain.SurveyImageRepository;
import org.springframework.data.repository.Repository;
public interface SurveyImageJpaRepository extends SurveyImageRepository, Repository<SurveyImage, Long> {}

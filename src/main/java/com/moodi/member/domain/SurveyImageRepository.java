package com.moodi.member.domain;

import java.util.List;
import java.util.Optional;
public interface SurveyImageRepository {
    SurveyImage save(SurveyImage entity);
    Optional<SurveyImage> findById(Long id);
    List<SurveyImage> findAllByOrderBySortOrderAscIdAsc();
    void delete(SurveyImage entity);
}

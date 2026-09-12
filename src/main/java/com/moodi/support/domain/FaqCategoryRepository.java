package com.moodi.support.domain;

import java.util.List;
import java.util.Optional;

public interface FaqCategoryRepository {

    FaqCategory save(FaqCategory category);

    Optional<FaqCategory> findById(Long id);

    List<FaqCategory> findAllByOrderBySortOrderAscIdAsc();

    boolean existsById(Long id);

    void delete(FaqCategory category);
}

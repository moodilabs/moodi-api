package com.moodi.support.domain;

import java.util.List;
import java.util.Optional;

public interface FaqRepository {

    Faq save(Faq faq);

    Optional<Faq> findById(Long id);

    List<Faq> findAllByOrderByCategoryIdAscSortOrderAscIdAsc();

    List<Faq> findByCategoryIdOrderBySortOrderAscIdAsc(Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    void delete(Faq faq);
}

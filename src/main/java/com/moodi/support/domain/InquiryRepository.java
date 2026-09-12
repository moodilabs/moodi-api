package com.moodi.support.domain;

import java.util.Optional;
import java.util.UUID;

public interface InquiryRepository {

    Inquiry save(Inquiry inquiry);

    Optional<Inquiry> findById(UUID id);

    long countByStatus(InquiryStatus status);
}

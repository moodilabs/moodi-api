package com.moodi.support.infrastructure.persistence;

import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface InquiryJpaRepository extends InquiryRepository, Repository<Inquiry, UUID> {
}

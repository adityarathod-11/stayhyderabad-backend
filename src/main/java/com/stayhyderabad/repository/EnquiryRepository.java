package com.stayhyderabad.repository;

import com.stayhyderabad.entity.Enquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    Page<Enquiry> findByPropertyOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
    long countByPropertyOwnerId(Long ownerId);
}

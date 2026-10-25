package com.stayhyderabad.repository;

import com.stayhyderabad.entity.Booking;
import com.stayhyderabad.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);
    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Booking> findByPropertyOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
    Page<Booking> findByPropertyIdOrderByCreatedAtDesc(Long propertyId, Pageable pageable);
    long countByPropertyOwnerIdAndStatus(Long ownerId, BookingStatus status);
    long countByPropertyOwnerId(Long ownerId);
    long countByStatus(BookingStatus status);
}

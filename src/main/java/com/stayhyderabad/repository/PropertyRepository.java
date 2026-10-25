package com.stayhyderabad.repository;

import com.stayhyderabad.entity.Property;
import com.stayhyderabad.entity.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    Optional<Property> findBySlug(String slug);
    Optional<Property> findBySlugAndStatus(String slug, PropertyStatus status);
    Page<Property> findByOwnerId(Long ownerId, Pageable pageable);
    Optional<Property> findByIdAndOwnerId(Long id, Long ownerId);
    
    List<Property> findByStatusAndIsFeaturedTrueOrderByUpdatedAtDesc(PropertyStatus status, Pageable pageable);

    @Query("SELECT DISTINCT p.locality FROM Property p WHERE p.status = 'PUBLISHED' ORDER BY p.locality ASC")
    List<String> findDistinctPublishedLocalities();

    long countByStatus(PropertyStatus status);
    long countByOwnerId(Long ownerId);

    @Query("SELECT COALESCE(SUM(p.availableBeds), 0) FROM Property p WHERE p.owner.id = :ownerId")
    long sumAvailableBedsByOwnerId(@Param("ownerId") Long ownerId);
}

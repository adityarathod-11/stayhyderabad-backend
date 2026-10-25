package com.stayhyderabad.service;

import com.stayhyderabad.dto.PropertyDto.PropertySummaryDto;
import com.stayhyderabad.entity.Favourite;
import com.stayhyderabad.entity.Property;
import com.stayhyderabad.entity.User;
import com.stayhyderabad.exception.ResourceNotFoundException;
import com.stayhyderabad.repository.FavouriteRepository;
import com.stayhyderabad.repository.PropertyRepository;
import com.stayhyderabad.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavouriteService {

    private final FavouriteRepository favouriteRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyService propertyService;

    public FavouriteService(
            FavouriteRepository favouriteRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            PropertyService propertyService) {
        this.favouriteRepository = favouriteRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.propertyService = propertyService;
    }

    @Transactional
    public boolean toggleFavourite(Long userId, Long propertyId) {
        if (favouriteRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            favouriteRepository.deleteByUserIdAndPropertyId(userId, propertyId);
            return false;
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Property property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
            favouriteRepository.save(new Favourite(user, property));
            return true;
        }
    }

    @Transactional(readOnly = true)
    public boolean isFavourite(Long userId, Long propertyId) {
        if (userId == null) return false;
        return favouriteRepository.existsByUserIdAndPropertyId(userId, propertyId);
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryDto> getUserFavourites(Long userId, Pageable pageable) {
        return favouriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(f -> propertyService.mapToSummary(f.getProperty()));
    }
}

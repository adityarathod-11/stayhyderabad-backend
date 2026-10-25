package com.stayhyderabad.service;

import com.stayhyderabad.dto.PropertyDto.*;
import com.stayhyderabad.entity.*;
import com.stayhyderabad.exception.BadRequestException;
import com.stayhyderabad.exception.ForbiddenException;
import com.stayhyderabad.exception.ResourceNotFoundException;
import com.stayhyderabad.repository.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final AmenityRepository amenityRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    public PropertyService(
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            PropertyImageRepository propertyImageRepository,
            AmenityRepository amenityRepository,
            UserRepository userRepository,
            ImageStorageService imageStorageService) {
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.amenityRepository = amenityRepository;
        this.userRepository = userRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryDto> searchProperties(PropertySearchCriteria criteria, Pageable pageable) {
        Specification<Property> spec = buildSearchSpecification(criteria);
        Page<Property> page = propertyRepository.findAll(spec, pageable);
        return page.map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    public List<PropertySummaryDto> getFeaturedProperties() {
        Pageable limit = PageRequest.of(0, 6);
        return propertyRepository.findByStatusAndIsFeaturedTrueOrderByUpdatedAtDesc(PropertyStatus.PUBLISHED, limit)
                .stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getPublishedLocalities() {
        return propertyRepository.findDistinctPublishedLocalities();
    }

    @Transactional(readOnly = true)
    public PropertyDetailDto getPropertyBySlug(String slug) {
        Property property = propertyRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with slug: " + slug));
        return mapToDetail(property);
    }

    @Transactional(readOnly = true)
    public PropertyDetailDto getPropertyById(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
        return mapToDetail(property);
    }

    @Transactional(readOnly = true)
    public List<PropertySummaryDto> getSimilarProperties(String locality, Long excludePropertyId) {
        PropertySearchCriteria criteria = new PropertySearchCriteria(
                locality, null, null, null, null, null, null, null, null, null
        );
        Pageable limit = PageRequest.of(0, 4);
        return propertyRepository.findAll(buildSearchSpecification(criteria), limit)
                .stream()
                .filter(p -> !p.getId().equals(excludePropertyId))
                .limit(3)
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    // --- OWNER ACTIONS ---

    @Transactional(readOnly = true)
    public Page<PropertySummaryDto> getOwnerProperties(Long ownerId, Pageable pageable) {
        return propertyRepository.findByOwnerId(ownerId, pageable).map(this::mapToSummary);
    }

    @Transactional
    public PropertyDetailDto createProperty(Long ownerId, PropertyCreateUpdateRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        Property property = new Property();
        property.setOwner(owner);
        applyPropertyDetails(property, request);

        // Generate unique slug
        String baseSlug = generateSlug(request.name() + "-" + request.locality());
        String slug = baseSlug;
        int counter = 1;
        while (propertyRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter++;
        }
        property.setSlug(slug);
        property.setStatus(PropertyStatus.PENDING_APPROVAL);

        Property saved = propertyRepository.save(property);

        // Add room types if provided
        if (request.roomTypes() != null && !request.roomTypes().isEmpty()) {
            int totalBeds = 0;
            int availBeds = 0;
            for (RoomTypeRequest rt : request.roomTypes()) {
                RoomType roomType = new RoomType();
                roomType.setProperty(saved);
                roomType.setSharingType(rt.sharingType());
                roomType.setMonthlyRent(rt.monthlyRent());
                roomType.setSecurityDeposit(rt.securityDeposit());
                roomType.setTotalBeds(rt.totalBeds());
                roomType.setAvailableBeds(rt.availableBeds());
                roomType.setHasAc(Boolean.TRUE.equals(rt.hasAc()));
                roomType.setHasAttachedBathroom(Boolean.TRUE.equals(rt.hasAttachedBathroom()));
                roomType.setHasBalcony(Boolean.TRUE.equals(rt.hasBalcony()));
                saved.getRoomTypes().add(roomType);
                totalBeds += rt.totalBeds();
                availBeds += rt.availableBeds();
            }
            saved.setTotalBeds(totalBeds);
            saved.setAvailableBeds(availBeds);
        }

        // Add amenities
        if (request.amenityIds() != null && !request.amenityIds().isEmpty()) {
            saved.getAmenities().addAll(amenityRepository.findByIdIn(request.amenityIds()));
        }

        return mapToDetail(propertyRepository.save(saved));
    }

    @Transactional
    public PropertyDetailDto updateProperty(Long ownerId, Long propertyId, PropertyCreateUpdateRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        validateOwnership(property, ownerId);

        applyPropertyDetails(property, request);

        // Refresh amenities
        if (request.amenityIds() != null) {
            property.getAmenities().clear();
            property.getAmenities().addAll(amenityRepository.findByIdIn(request.amenityIds()));
        }

        return mapToDetail(propertyRepository.save(property));
    }

    @Transactional
    public PropertyDetailDto togglePublishStatus(Long ownerId, Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        validateOwnership(property, ownerId);

        if (property.getStatus() == PropertyStatus.PUBLISHED) {
            property.setStatus(PropertyStatus.DRAFT);
        } else {
            // Check if minimum requirements met to publish
            if (property.getRoomTypes().isEmpty()) {
                throw new BadRequestException("Must add at least one room category before publishing");
            }
            property.setStatus(PropertyStatus.PUBLISHED);
        }

        return mapToDetail(propertyRepository.save(property));
    }

    @Transactional
    public PropertyImageDto uploadPropertyImage(Long ownerId, Long propertyId, MultipartFile file, String caption, String category, Boolean isPrimary) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        validateOwnership(property, ownerId);

        ImageStorageService.UploadResult result = imageStorageService.uploadImage(file, "properties/" + propertyId);

        PropertyImage image = new PropertyImage();
        image.setProperty(property);
        image.setImageUrl(result.imageUrl());
        image.setThumbnailUrl(result.thumbnailUrl());
        image.setPublicId(result.publicId());
        image.setCaption(caption != null ? caption : property.getName());
        image.setCategory(category != null ? category : "GENERAL");

        boolean makePrimary = Boolean.TRUE.equals(isPrimary) || property.getImages().isEmpty();
        if (makePrimary) {
            property.getImages().forEach(img -> img.setIsPrimary(false));
            image.setIsPrimary(true);
        } else {
            image.setIsPrimary(false);
        }

        image.setDisplayOrder(property.getImages().size());
        PropertyImage saved = propertyImageRepository.save(image);

        return new PropertyImageDto(
                saved.getId(),
                saved.getImageUrl(),
                saved.getThumbnailUrl(),
                saved.getCaption(),
                saved.getCategory(),
                saved.getIsPrimary(),
                saved.getDisplayOrder()
        );
    }

    @Transactional
    public void deletePropertyImage(Long ownerId, Long propertyId, Long imageId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        validateOwnership(property, ownerId);

        PropertyImage image = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (!image.getProperty().getId().equals(propertyId)) {
            throw new BadRequestException("Image does not belong to this property");
        }

        imageStorageService.deleteImage(image.getPublicId());
        propertyImageRepository.delete(image);
    }

    private void validateOwnership(Property property, Long ownerId) {
        User user = userRepository.findById(ownerId).orElseThrow();
        if (user.getRole() == Role.ROLE_ADMIN) {
            return; // Admins bypass ownership check
        }
        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not have permission to manage this property");
        }
    }

    private void applyPropertyDetails(Property property, PropertyCreateUpdateRequest req) {
        property.setName(req.name().trim());
        property.setGenderCategory(req.genderCategory());
        property.setLocality(req.locality().trim());
        property.setAddressLine(req.addressLine().trim());
        property.setLandmark(req.landmark());
        property.setLatitude(req.latitude());
        property.setLongitude(req.longitude());
        property.setDescription(req.description());
        property.setHouseRules(req.houseRules());
        if (req.gateClosingTime() != null) property.setGateClosingTime(req.gateClosingTime());
        if (req.foodIncluded() != null) property.setFoodIncluded(req.foodIncluded());
        if (req.foodType() != null) property.setFoodType(req.foodType());
        property.setStartingMonthlyRent(req.startingMonthlyRent());
        property.setSecurityDeposit(req.securityDeposit());
        if (req.maintenanceCharge() != null) property.setMaintenanceCharge(req.maintenanceCharge());
        if (req.noticePeriodDays() != null) property.setNoticePeriodDays(req.noticePeriodDays());
        property.setContactPhone(req.contactPhone());
        property.setWhatsappNumber(req.whatsappNumber());
    }

    private Specification<Property> buildSearchSpecification(PropertySearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only published properties for public search
            predicates.add(cb.equal(root.get("status"), PropertyStatus.PUBLISHED));

            if (criteria != null) {
                if (criteria.locality() != null && !criteria.locality().isBlank()) {
                    predicates.add(cb.equal(cb.lower(root.get("locality")), criteria.locality().trim().toLowerCase()));
                }

                if (criteria.genderCategory() != null) {
                    predicates.add(cb.equal(root.get("genderCategory"), criteria.genderCategory()));
                }

                if (criteria.minRent() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("startingMonthlyRent"), criteria.minRent()));
                }

                if (criteria.maxRent() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("startingMonthlyRent"), criteria.maxRent()));
                }

                if (Boolean.TRUE.equals(criteria.foodIncluded())) {
                    predicates.add(cb.equal(root.get("foodIncluded"), true));
                }

                if (Boolean.TRUE.equals(criteria.availableBedsOnly())) {
                    predicates.add(cb.greaterThan(root.get("availableBeds"), 0));
                }

                // If filtering by room characteristics (sharing, AC, attached bathroom)
                if (criteria.sharingType() != null || Boolean.TRUE.equals(criteria.hasAc()) || Boolean.TRUE.equals(criteria.hasAttachedBathroom())) {
                    Join<Property, RoomType> roomJoin = root.join("roomTypes", JoinType.INNER);
                    if (criteria.sharingType() != null) {
                        predicates.add(cb.equal(roomJoin.get("sharingType"), criteria.sharingType()));
                    }
                    if (Boolean.TRUE.equals(criteria.hasAc())) {
                        predicates.add(cb.equal(roomJoin.get("hasAc"), true));
                    }
                    if (Boolean.TRUE.equals(criteria.hasAttachedBathroom())) {
                        predicates.add(cb.equal(roomJoin.get("hasAttachedBathroom"), true));
                    }
                    query.distinct(true);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public PropertySummaryDto mapToSummary(Property p) {
        String primaryImg = p.getImages().stream()
                .filter(PropertyImage::getIsPrimary)
                .findFirst()
                .map(PropertyImage::getImageUrl)
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

        List<SharingType> sharing = p.getRoomTypes().stream()
                .map(RoomType::getSharingType)
                .distinct()
                .collect(Collectors.toList());

        List<String> keyAmenities = p.getAmenities().stream()
                .map(Amenity::getName)
                .limit(4)
                .collect(Collectors.toList());

        return new PropertySummaryDto(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getGenderCategory(),
                p.getLocality(),
                p.getLandmark(),
                p.getStartingMonthlyRent(),
                p.getSecurityDeposit(),
                p.getAvailableBeds(),
                p.getTotalBeds(),
                p.getFoodIncluded(),
                p.getFoodType(),
                primaryImg,
                p.getImages().size(),
                sharing,
                keyAmenities,
                p.getStatus(),
                p.getIsFeatured(),
                p.getLastAvailabilityUpdate()
        );
    }

    public PropertyDetailDto mapToDetail(Property p) {
        List<PropertyImageDto> images = p.getImages().stream()
                .map(img -> new PropertyImageDto(
                        img.getId(),
                        img.getImageUrl(),
                        img.getThumbnailUrl(),
                        img.getCaption(),
                        img.getCategory(),
                        img.getIsPrimary(),
                        img.getDisplayOrder()
                ))
                .collect(Collectors.toList());

        List<RoomTypeDto> roomTypes = p.getRoomTypes().stream()
                .map(rt -> new RoomTypeDto(
                        rt.getId(),
                        rt.getSharingType(),
                        rt.getMonthlyRent(),
                        rt.getSecurityDeposit(),
                        rt.getTotalBeds(),
                        rt.getAvailableBeds(),
                        rt.getHasAc(),
                        rt.getHasAttachedBathroom(),
                        rt.getHasBalcony()
                ))
                .collect(Collectors.toList());

        Set<AmenityDto> amenities = p.getAmenities().stream()
                .map(a -> new AmenityDto(a.getId(), a.getName(), a.getIconName(), a.getCategory(), a.getDescription()))
                .collect(Collectors.toSet());

        String ownerBusinessName = p.getOwner().getOwnerProfile() != null
                ? p.getOwner().getOwnerProfile().getBusinessName() : null;

        return new PropertyDetailDto(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getGenderCategory(),
                p.getLocality(),
                p.getAddressLine(),
                p.getLandmark(),
                p.getLatitude(),
                p.getLongitude(),
                p.getDescription(),
                p.getHouseRules(),
                p.getGateClosingTime(),
                p.getFoodIncluded(),
                p.getFoodType(),
                p.getStartingMonthlyRent(),
                p.getSecurityDeposit(),
                p.getMaintenanceCharge(),
                p.getNoticePeriodDays(),
                p.getStatus(),
                p.getIsFeatured(),
                p.getTotalBeds(),
                p.getAvailableBeds(),
                p.getContactPhone(),
                p.getWhatsappNumber(),
                p.getLastAvailabilityUpdate(),
                images,
                roomTypes,
                amenities,
                p.getOwner().getId(),
                p.getOwner().getFullName(),
                ownerBusinessName
        );
    }

    private String generateSlug(String input) {
        String nowhitespace = input.trim().replaceAll("\\s+", "-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        return normalized.replaceAll("[^\\w-]", "").toLowerCase();
    }
}

package com.stayhyderabad.service;

import com.stayhyderabad.dto.BookingDto.*;
import com.stayhyderabad.entity.Enquiry;
import com.stayhyderabad.entity.Property;
import com.stayhyderabad.entity.User;
import com.stayhyderabad.exception.ResourceNotFoundException;
import com.stayhyderabad.repository.EnquiryRepository;
import com.stayhyderabad.repository.PropertyRepository;
import com.stayhyderabad.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public EnquiryService(
            EnquiryRepository enquiryRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository) {
        this.enquiryRepository = enquiryRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EnquiryResponse submitEnquiry(Long maybeUserId, EnquiryRequest request) {
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        Enquiry enquiry = new Enquiry();
        enquiry.setProperty(property);
        if (maybeUserId != null) {
            userRepository.findById(maybeUserId).ifPresent(enquiry::setUser);
        }
        enquiry.setName(request.name().trim());
        enquiry.setPhone(request.phone().trim());
        enquiry.setEmail(request.email());
        enquiry.setMessage(request.message().trim());
        enquiry.setIsWhatsapp(Boolean.TRUE.equals(request.isWhatsapp()));

        Enquiry saved = enquiryRepository.save(enquiry);
        return new EnquiryResponse(
                saved.getId(),
                property.getId(),
                property.getName(),
                saved.getName(),
                saved.getPhone(),
                saved.getEmail(),
                saved.getMessage(),
                saved.getIsWhatsapp(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<EnquiryResponse> getOwnerEnquiries(Long ownerId, Pageable pageable) {
        return enquiryRepository.findByPropertyOwnerIdOrderByCreatedAtDesc(ownerId, pageable)
                .map(e -> new EnquiryResponse(
                        e.getId(),
                        e.getProperty().getId(),
                        e.getProperty().getName(),
                        e.getName(),
                        e.getPhone(),
                        e.getEmail(),
                        e.getMessage(),
                        e.getIsWhatsapp(),
                        e.getCreatedAt()
                ));
    }
}

package com.stayhyderabad.service;

import com.stayhyderabad.dto.BookingDto.*;
import com.stayhyderabad.entity.*;
import com.stayhyderabad.exception.BadRequestException;
import com.stayhyderabad.exception.ForbiddenException;
import com.stayhyderabad.exception.ResourceNotFoundException;
import com.stayhyderabad.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final UserRepository userRepository;

    public BookingService(
            BookingRepository bookingRepository,
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBookingRequest(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (property.getStatus() != PropertyStatus.PUBLISHED) {
            throw new BadRequestException("Cannot book an unpublished or inactive property");
        }

        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Room category not found"));

        if (!roomType.getProperty().getId().equals(property.getId())) {
            throw new BadRequestException("Selected room category does not belong to this property");
        }

        // Quick check that beds exist, but beds are NOT decremented yet until owner accepts!
        if (roomType.getAvailableBeds() < request.numPersons()) {
            throw new BadRequestException("Requested number of beds (" + request.numPersons() + ") exceeds current available vacancy (" + roomType.getAvailableBeds() + ")");
        }

        Booking booking = new Booking();
        booking.setBookingReference(generateBookingReference());
        booking.setProperty(property);
        booking.setRoomType(roomType);
        booking.setUser(user);
        booking.setPreferredMoveInDate(request.preferredMoveInDate());
        booking.setNumPersons(request.numPersons() != null ? request.numPersons() : 1);
        booking.setApplicantName(request.applicantName().trim());
        booking.setApplicantPhone(request.applicantPhone().trim());
        booking.setApplicantEmail(request.applicantEmail().trim());
        booking.setMessage(request.message());
        booking.setMonthlyRentLocked(roomType.getMonthlyRent());
        booking.setDepositLocked(roomType.getSecurityDeposit());
        booking.setStatus(BookingStatus.PENDING);
        booking.setBedsAllocated(false);

        Booking saved = bookingRepository.save(booking);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getOwnerBookings(Long ownerId, Pageable pageable) {
        return bookingRepository.findByPropertyOwnerIdOrderByCreatedAtDesc(ownerId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(Long userId, String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + reference));

        User user = userRepository.findById(userId).orElseThrow();
        boolean isOwner = booking.getProperty().getOwner().getId().equals(userId);
        boolean isUser = booking.getUser().getId().equals(userId);
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isUser && !isAdmin) {
            throw new ForbiddenException("You do not have permission to view this booking");
        }

        return mapToResponse(booking);
    }

    /**
     * User cancellation: can cancel their own booking if PENDING or ACCEPTED.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BookingResponse cancelBookingByUser(Long userId, Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only cancel your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Booking cannot be cancelled in its current state: " + booking.getStatus());
        }

        // Release beds if they were allocated upon prior acceptance
        if (Boolean.TRUE.equals(booking.getBedsAllocated())) {
            releaseBeds(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setOwnerNote(booking.getOwnerNote() != null
                ? booking.getOwnerNote() + " | Cancelled by user: " + reason
                : "Cancelled by user: " + reason);

        return mapToResponse(bookingRepository.save(booking));
    }

    /**
     * Owner updates status: ACCEPT, REJECT, or COMPLETE.
     * Uses pessimistic locking on room_types to prevent race conditions when multiple bookings are accepted.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BookingResponse updateBookingStatusByOwner(Long ownerId, Long bookingId, BookingStatusUpdate request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        User user = userRepository.findById(ownerId).orElseThrow();
        if (user.getRole() != Role.ROLE_ADMIN && !booking.getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You are not authorized to manage bookings for this property");
        }

        BookingStatus newStatus = request.status();
        BookingStatus currentStatus = booking.getStatus();

        if (newStatus == BookingStatus.ACCEPTED && currentStatus != BookingStatus.ACCEPTED) {
            // Lock room_type and allocate bed
            allocateBedsWithLock(booking);
        } else if ((newStatus == BookingStatus.REJECTED || newStatus == BookingStatus.CANCELLED) && Boolean.TRUE.equals(booking.getBedsAllocated())) {
            // Release beds
            releaseBeds(booking);
        }

        booking.setStatus(newStatus);
        if (request.ownerNote() != null) {
            booking.setOwnerNote(request.ownerNote().trim());
        }

        return mapToResponse(bookingRepository.save(booking));
    }

    private void allocateBedsWithLock(Booking booking) {
        // Pessimistic write lock on room_type row
        RoomType roomType = roomTypeRepository.findWithLockById(booking.getRoomType().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room type no longer exists"));

        if (roomType.getAvailableBeds() < booking.getNumPersons()) {
            throw new BadRequestException("Cannot accept booking: only " + roomType.getAvailableBeds()
                    + " beds available in room, requested " + booking.getNumPersons());
        }

        // Decrement room type vacancy
        roomType.setAvailableBeds(roomType.getAvailableBeds() - booking.getNumPersons());
        roomTypeRepository.save(roomType);

        // Update overall property available beds count
        Property property = booking.getProperty();
        int newPropertyAvail = Math.max(0, property.getAvailableBeds() - booking.getNumPersons());
        property.setAvailableBeds(newPropertyAvail);
        property.setLastAvailabilityUpdate(OffsetDateTime.now());
        propertyRepository.save(property);

        booking.setBedsAllocated(true);
    }

    private void releaseBeds(Booking booking) {
        RoomType roomType = roomTypeRepository.findWithLockById(booking.getRoomType().getId())
                .orElse(null);

        if (roomType != null) {
            roomType.setAvailableBeds(roomType.getAvailableBeds() + booking.getNumPersons());
            roomTypeRepository.save(roomType);

            Property property = booking.getProperty();
            property.setAvailableBeds(property.getAvailableBeds() + booking.getNumPersons());
            property.setLastAvailabilityUpdate(OffsetDateTime.now());
            propertyRepository.save(property);
        }

        booking.setBedsAllocated(false);
    }

    private String generateBookingReference() {
        int year = LocalDate.now().getYear();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "SH-" + year + "-" + randomSuffix;
    }

    public BookingResponse mapToResponse(Booking b) {
        String primaryImg = b.getProperty().getImages().stream()
                .filter(PropertyImage::getIsPrimary)
                .findFirst()
                .map(PropertyImage::getImageUrl)
                .orElse(b.getProperty().getImages().isEmpty() ? null : b.getProperty().getImages().get(0).getImageUrl());

        return new BookingResponse(
                b.getId(),
                b.getBookingReference(),
                b.getProperty().getId(),
                b.getProperty().getName(),
                b.getProperty().getSlug(),
                b.getProperty().getLocality(),
                primaryImg,
                b.getRoomType().getId(),
                b.getRoomType().getSharingType(),
                b.getPreferredMoveInDate(),
                b.getNumPersons(),
                b.getApplicantName(),
                b.getApplicantPhone(),
                b.getApplicantEmail(),
                b.getMessage(),
                b.getMonthlyRentLocked(),
                b.getDepositLocked(),
                b.getStatus(),
                b.getOwnerNote(),
                b.getBedsAllocated(),
                b.getCreatedAt()
        );
    }
}

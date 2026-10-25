package com.stayhyderabad;

import com.stayhyderabad.dto.BookingDto.BookingStatusUpdate;
import com.stayhyderabad.entity.*;
import com.stayhyderabad.exception.BadRequestException;
import com.stayhyderabad.repository.BookingRepository;
import com.stayhyderabad.repository.PropertyRepository;
import com.stayhyderabad.repository.RoomTypeRepository;
import com.stayhyderabad.repository.UserRepository;
import com.stayhyderabad.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private UserRepository userRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, propertyRepository, roomTypeRepository, userRepository);
    }

    @Test
    void updateBookingStatus_AcceptShouldAllocateBedAtomically() {
        Long ownerId = 1L;
        Long bookingId = 100L;

        User owner = new User();
        owner.setId(ownerId);
        owner.setRole(Role.ROLE_OWNER);

        Property property = new Property();
        property.setId(50L);
        property.setName("Test PG");
        property.setSlug("test-pg");
        property.setLocality("Madhapur");
        property.setOwner(owner);
        property.setAvailableBeds(5);

        RoomType roomType = new RoomType();
        roomType.setId(10L);
        roomType.setProperty(property);
        roomType.setSharingType(SharingType.DOUBLE);
        roomType.setAvailableBeds(2);
        roomType.setMonthlyRent(new BigDecimal("10000.00"));
        roomType.setSecurityDeposit(new BigDecimal("5000.00"));

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setBookingReference("SH-2026-TEST01");
        booking.setProperty(property);
        booking.setRoomType(roomType);
        booking.setNumPersons(1);
        booking.setStatus(BookingStatus.PENDING);
        booking.setBedsAllocated(false);
        booking.setMonthlyRentLocked(new BigDecimal("10000.00"));
        booking.setDepositLocked(new BigDecimal("5000.00"));

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(roomTypeRepository.findWithLockById(10L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingStatusUpdate updateReq = new BookingStatusUpdate(BookingStatus.ACCEPTED, "Welcome to the PG");
        var response = bookingService.updateBookingStatusByOwner(ownerId, bookingId, updateReq);

        assertNotNull(response);
        assertEquals(BookingStatus.ACCEPTED, response.status());
        assertTrue(response.bedsAllocated());
        assertEquals(1, roomType.getAvailableBeds()); // Decremented by 1
        assertEquals(4, property.getAvailableBeds()); // Decremented by 1
        verify(roomTypeRepository).save(roomType);
        verify(propertyRepository).save(property);
    }

    @Test
    void updateBookingStatus_AcceptShouldFailWhenInsufficientBeds() {
        Long ownerId = 1L;
        Long bookingId = 100L;

        User owner = new User();
        owner.setId(ownerId);
        owner.setRole(Role.ROLE_OWNER);

        Property property = new Property();
        property.setId(50L);
        property.setOwner(owner);

        RoomType roomType = new RoomType();
        roomType.setId(10L);
        roomType.setProperty(property);
        roomType.setAvailableBeds(0); // No beds available!

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setProperty(property);
        booking.setRoomType(roomType);
        booking.setNumPersons(1);
        booking.setStatus(BookingStatus.PENDING);
        booking.setBedsAllocated(false);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(roomTypeRepository.findWithLockById(10L)).thenReturn(Optional.of(roomType));

        BookingStatusUpdate updateReq = new BookingStatusUpdate(BookingStatus.ACCEPTED, null);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.updateBookingStatusByOwner(ownerId, bookingId, updateReq));

        assertTrue(ex.getMessage().contains("Cannot accept booking: only 0 beds available"));
    }
}

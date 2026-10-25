package com.stayhyderabad.config;

import com.stayhyderabad.entity.*;
import com.stayhyderabad.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Component
@Profile("!prod") // Safe dev seed: never executes in production environment
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PropertyRepository propertyRepository;
    private final AmenityRepository amenityRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            OwnerProfileRepository ownerProfileRepository,
            PropertyRepository propertyRepository,
            AmenityRepository amenityRepository,
            BookingRepository bookingRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.propertyRepository = propertyRepository;
        this.amenityRepository = amenityRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data. Skipping initial demo seed.");
            return;
        }

        log.info("Seeding realistic Hyderabad accommodation demo data for local development...");

        // 1. Create Default Admin
        User admin = new User();
        admin.setEmail("admin@stayhyderabad.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setFullName("Platform Administrator");
        admin.setPhone("9876500001");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin = userRepository.save(admin);

        // 2. Create Default Owner
        User owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPasswordHash(passwordEncoder.encode("Owner@123"));
        owner.setFullName("K. Venkat Rao");
        owner.setPhone("9849012345");
        owner.setRole(Role.ROLE_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        owner = userRepository.save(owner);

        OwnerProfile ownerProfile = new OwnerProfile();
        ownerProfile.setUser(owner);
        ownerProfile.setBusinessName("Sri Balaji Hospitality Solutions");
        ownerProfile.setGovtIdType("AADHAAR");
        ownerProfile.setGovtIdNumber("XXXX-XXXX-8921");
        ownerProfile.setIsVerified(true);
        ownerProfile.setAlternatePhone("9849054321");
        ownerProfile.setAddress("Plot 42, Silicon Valley, Madhapur, Hyderabad, Telangana 500081");
        ownerProfileRepository.save(ownerProfile);

        // 3. Create Default Regular User
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordEncoder.encode("User@123"));
        user.setFullName("Rahul Sharma");
        user.setPhone("9988776655");
        user.setRole(Role.ROLE_USER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        // Fetch Amenities
        List<Amenity> allAmenities = amenityRepository.findAll();

        // 4. Create Demonstration Properties
        // Property 1: Madhapur Co-living
        Property p1 = createProperty(
                owner,
                "Sai Balaji Luxury Co-Living & PG",
                "sai-balaji-luxury-co-living-madhapur",
                GenderCategory.CO_LIVING,
                "Madhapur",
                "Plot 88, Kavuri Hills, Near Image Hospitals, Madhapur",
                "Near Cyber Towers & Durgam Cheruvu Metro",
                new BigDecimal("17.4399"),
                new BigDecimal("78.3908"),
                "Premium co-living space designed for tech professionals and students. High-speed 300 Mbps Wi-Fi, 3 times hygienic homestyle food (North and South Indian), daily housekeeping, power backup, and rooftop chill-out lounge. Walking distance from HITEC City metro.",
                "1. Gate closes at 11:00 PM on weekdays, 12:00 AM on weekends.\n2. Non-veg served 3 days a week.\n3. Smoking prohibited inside bedrooms.",
                "11:00 PM",
                true,
                FoodType.VEG_AND_NON_VEG,
                new BigDecimal("9500.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("500.00"),
                30,
                PropertyStatus.PUBLISHED,
                true,
                "9849012345",
                "9849012345",
                allAmenities
        );
        addRoomType(p1, SharingType.SINGLE, new BigDecimal("16000.00"), new BigDecimal("8000.00"), 4, 1, true, true, true);
        addRoomType(p1, SharingType.DOUBLE, new BigDecimal("11500.00"), new BigDecimal("6000.00"), 12, 4, true, true, false);
        addRoomType(p1, SharingType.TRIPLE, new BigDecimal("9500.00"), new BigDecimal("5000.00"), 18, 6, true, true, false);
        addImage(p1, "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=1200&q=80", "Modern Bedroom Setup", "BEDROOM", true, 0);
        addImage(p1, "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?auto=format&fit=crop&w=1200&q=80", "Spacious Double Sharing Room", "BEDROOM", false, 1);
        addImage(p1, "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=1200&q=80", "Attached Modern Washroom", "BATHROOM", false, 2);
        addImage(p1, "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1200&q=80", "Dining Area with Meal Service", "DINING", false, 3);
        propertyRepository.save(p1);

        // Property 2: Gachibowli Boys PG
        Property p2 = createProperty(
                owner,
                "Sri Venkateshwara Executive Boys PG",
                "sri-venkateshwara-executive-boys-pg-gachibowli",
                GenderCategory.MALE,
                "Gachibowli",
                "Lane 4, Telecom Nagar, Gachibowli",
                "Near DLF Cyber City Gate 2",
                new BigDecimal("17.4401"),
                new BigDecimal("78.3489"),
                "Ideal PG for freshers and IT folks working in DLF, Microsoft, and Infosys. Hot Andhra meals, high-speed fiber internet, washing machines on each floor, and secure two-wheeler parking.",
                "1. Alcohol strictly prohibited on premises.\n2. Visitors allowed in lounge only.\n3. Notice period 30 days mandatory.",
                "10:30 PM",
                true,
                FoodType.VEG_AND_NON_VEG,
                new BigDecimal("8000.00"),
                new BigDecimal("4000.00"),
                BigDecimal.ZERO,
                30,
                PropertyStatus.PUBLISHED,
                true,
                "9849012345",
                "9849012345",
                allAmenities
        );
        addRoomType(p2, SharingType.DOUBLE, new BigDecimal("10000.00"), new BigDecimal("5000.00"), 10, 2, true, true, false);
        addRoomType(p2, SharingType.TRIPLE, new BigDecimal("8000.00"), new BigDecimal("4000.00"), 15, 5, false, true, false);
        addRoomType(p2, SharingType.FOUR_SHARING, new BigDecimal("7000.00"), new BigDecimal("3500.00"), 20, 8, false, true, false);
        addImage(p2, "https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?auto=format&fit=crop&w=1200&q=80", "Executive Double Sharing Room", "BEDROOM", true, 0);
        addImage(p2, "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80", "Comfortable Bed and Study Desks", "BEDROOM", false, 1);
        addImage(p2, "https://images.unsplash.com/photo-1540518614846-7ede433c4ef4?auto=format&fit=crop&w=1200&q=80", "Lounge & Indoor Games Zone", "COMMON_AREA", false, 2);
        propertyRepository.save(p2);

        // Property 3: HITEC City Girls PG
        Property p3 = createProperty(
                owner,
                "Ananya Elite Girls PG & Hostel",
                "ananya-elite-girls-pg-hitec-city",
                GenderCategory.FEMALE,
                "HITEC City",
                "Plot 14, Siddiq Nagar, HITEC City",
                "Behind Mindspace IT Park & Inorbit Mall",
                new BigDecimal("17.4375"),
                new BigDecimal("78.3812"),
                "Safe, modern, and biometric-gated women's luxury PG. 24/7 CCTV surveillance, female security guards, tasty nutritious meals with sweet on weekends, AC rooms with personal lockers and work desks.",
                "1. Gate closes at 10:00 PM.\n2. Biometric entry required.\n3. Late entry requires prior warden notification.",
                "10:00 PM",
                true,
                FoodType.VEG_AND_NON_VEG,
                new BigDecimal("9000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("300.00"),
                30,
                PropertyStatus.PUBLISHED,
                true,
                "9849012345",
                "9849012345",
                allAmenities
        );
        addRoomType(p3, SharingType.SINGLE, new BigDecimal("15000.00"), new BigDecimal("7000.00"), 5, 2, true, true, true);
        addRoomType(p3, SharingType.DOUBLE, new BigDecimal("11000.00"), new BigDecimal("5500.00"), 14, 3, true, true, false);
        addRoomType(p3, SharingType.TRIPLE, new BigDecimal("9000.00"), new BigDecimal("4500.00"), 12, 4, true, true, false);
        addImage(p3, "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80", "Deluxe Room Interior", "BEDROOM", true, 0);
        addImage(p3, "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80", "Study Desk and Wardrobe Area", "BEDROOM", false, 1);
        addImage(p3, "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=1200&q=80", "Clean Sparkling Bathrooms", "BATHROOM", false, 2);
        propertyRepository.save(p3);

        // Property 4: Kondapur Co-Living
        Property p4 = createProperty(
                owner,
                "UrbanNest Premium Co-Living",
                "urbannest-premium-co-living-kondapur",
                GenderCategory.CO_LIVING,
                "Kondapur",
                "Survey 45, Raghavendra Colony, Kondapur",
                "Near Botanical Garden & Chirec Public School",
                new BigDecimal("17.4615"),
                new BigDecimal("78.3619"),
                "Boutique co-living residence for working professionals. High-speed dual-band Wi-Fi, fully furnished rooms with orthopaedic mattresses, community events, gym access, and delicious vegetarian and non-vegetarian food options.",
                "1. Self-service laundry available round the clock.\n2. Quiet hours from 11:30 PM to 6:30 AM.",
                "11:30 PM",
                true,
                FoodType.VEG_AND_NON_VEG,
                new BigDecimal("10500.00"),
                new BigDecimal("6000.00"),
                new BigDecimal("500.00"),
                30,
                PropertyStatus.PUBLISHED,
                false,
                "9849012345",
                "9849012345",
                allAmenities
        );
        addRoomType(p4, SharingType.SINGLE, new BigDecimal("18000.00"), new BigDecimal("9000.00"), 6, 2, true, true, true);
        addRoomType(p4, SharingType.DOUBLE, new BigDecimal("12500.00"), new BigDecimal("6500.00"), 10, 3, true, true, true);
        addRoomType(p4, SharingType.TRIPLE, new BigDecimal("10500.00"), new BigDecimal("5500.00"), 15, 5, true, true, false);
        addImage(p4, "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80", "Modern Studio Setup", "BEDROOM", true, 0);
        addImage(p4, "https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=1200&q=80", "Co-working Lounge and Coffee Corner", "COMMON_AREA", false, 1);
        propertyRepository.save(p4);

        // Property 5: Ameerpet Techie PG
        Property p5 = createProperty(
                owner,
                "Sri Rama Krishna Techie PG",
                "sri-rama-krishna-techie-pg-ameerpet",
                GenderCategory.MALE,
                "Ameerpet",
                "Near Maitrivanam, Ameerpet",
                "Opposite Ameerpet Metro Interchange",
                new BigDecimal("17.4375"),
                new BigDecimal("78.4483"),
                "Budget-friendly hostel strategically located right next to Ameerpet software institutes and metro interchange. Unlimited Wi-Fi for practice sessions, 3 times food, inverter power backup, and round-the-clock water supply.",
                "1. Strict study hours discipline.\n2. Monthly rent payable before 5th of every month.",
                "10:30 PM",
                true,
                FoodType.VEG_AND_NON_VEG,
                new BigDecimal("6500.00"),
                new BigDecimal("2500.00"),
                BigDecimal.ZERO,
                15,
                PropertyStatus.PUBLISHED,
                false,
                "9849012345",
                "9849012345",
                allAmenities
        );
        addRoomType(p5, SharingType.DOUBLE, new BigDecimal("8500.00"), new BigDecimal("3500.00"), 10, 2, false, true, false);
        addRoomType(p5, SharingType.TRIPLE, new BigDecimal("7200.00"), new BigDecimal("3000.00"), 15, 4, false, true, false);
        addRoomType(p5, SharingType.FOUR_SHARING, new BigDecimal("6500.00"), new BigDecimal("2500.00"), 24, 7, false, true, false);
        addImage(p5, "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80", "Ameerpet Student Hostel Room", "BEDROOM", true, 0);
        propertyRepository.save(p5);

        // Property 6: Kukatpally Deluxe PG
        Property p6 = createProperty(
                owner,
                "Kakatiya Residency Deluxe PG",
                "kakatiya-residency-deluxe-pg-kukatpally",
                GenderCategory.CO_LIVING,
                "Kukatpally",
                "KPHB 6th Phase, Kukatpally",
                "Near JNTU & Nexus Hyderabad Mall",
                new BigDecimal("17.4938"),
                new BigDecimal("78.4018"),
                "Centrally located near JNTU and KPHB metro station. Spacious ventilated rooms, South & North Indian cuisine, lift, solar water heaters, and dedicated bike parking.",
                "1. Main gate closed after 10:45 PM.\n2. Keep your personal locker secured.",
                "10:45 PM",
                true,
                FoodType.VEG_AND_NON_VEG,
                new BigDecimal("7800.00"),
                new BigDecimal("3500.00"),
                BigDecimal.ZERO,
                30,
                PropertyStatus.PUBLISHED,
                false,
                "9849012345",
                "9849012345",
                allAmenities
        );
        addRoomType(p6, SharingType.DOUBLE, new BigDecimal("9800.00"), new BigDecimal("4500.00"), 8, 2, true, true, false);
        addRoomType(p6, SharingType.TRIPLE, new BigDecimal("7800.00"), new BigDecimal("3500.00"), 15, 6, false, true, false);
        addImage(p6, "https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?auto=format&fit=crop&w=1200&q=80", "Kukatpally Deluxe Room", "BEDROOM", true, 0);
        propertyRepository.save(p6);

        // 5. Create a Sample Booking for the user
        Booking sampleBooking = new Booking();
        sampleBooking.setBookingReference("SH-2026-DEMO01");
        sampleBooking.setProperty(p1);
        sampleBooking.setRoomType(p1.getRoomTypes().get(1)); // Double sharing
        sampleBooking.setUser(user);
        sampleBooking.setPreferredMoveInDate(LocalDate.now().plusDays(5));
        sampleBooking.setNumPersons(1);
        sampleBooking.setApplicantName(user.getFullName());
        sampleBooking.setApplicantPhone(user.getPhone());
        sampleBooking.setApplicantEmail(user.getEmail());
        sampleBooking.setMessage("Hi, I am joining an IT firm in Cyber Towers next Monday. Requesting double sharing room with AC.");
        sampleBooking.setMonthlyRentLocked(new BigDecimal("11500.00"));
        sampleBooking.setDepositLocked(new BigDecimal("6000.00"));
        sampleBooking.setStatus(BookingStatus.PENDING);
        sampleBooking.setBedsAllocated(false);
        bookingRepository.save(sampleBooking);

        log.info("Demo data seeding completed successfully! Test accounts available:");
        log.info("User: user@example.com / User@123");
        log.info("Owner: owner@example.com / Owner@123");
        log.info("Admin: admin@stayhyderabad.com / Admin@123");
    }

    private Property createProperty(
            User owner, String name, String slug, GenderCategory gender, String locality,
            String address, String landmark, BigDecimal lat, BigDecimal lng,
            String desc, String rules, String gateClose, boolean foodInc, FoodType foodType,
            BigDecimal startRent, BigDecimal deposit, BigDecimal maintenance, int noticeDays,
            PropertyStatus status, boolean isFeatured, String phone, String wa, List<Amenity> amenities) {

        Property p = new Property();
        p.setOwner(owner);
        p.setName(name);
        p.setSlug(slug);
        p.setGenderCategory(gender);
        p.setLocality(locality);
        p.setAddressLine(address);
        p.setLandmark(landmark);
        p.setLatitude(lat);
        p.setLongitude(lng);
        p.setDescription(desc);
        p.setHouseRules(rules);
        p.setGateClosingTime(gateClose);
        p.setFoodIncluded(foodInc);
        p.setFoodType(foodType);
        p.setStartingMonthlyRent(startRent);
        p.setSecurityDeposit(deposit);
        p.setMaintenanceCharge(maintenance);
        p.setNoticePeriodDays(noticeDays);
        p.setStatus(status);
        p.setIsFeatured(isFeatured);
        p.setContactPhone(phone);
        p.setWhatsappNumber(wa);
        p.setAmenities(Set.copyOf(amenities));
        return p;
    }

    private void addRoomType(Property p, SharingType sharing, BigDecimal rent, BigDecimal deposit, int totalBeds, int availBeds, boolean ac, boolean bath, boolean balcony) {
        RoomType rt = new RoomType();
        rt.setProperty(p);
        rt.setSharingType(sharing);
        rt.setMonthlyRent(rent);
        rt.setSecurityDeposit(deposit);
        rt.setTotalBeds(totalBeds);
        rt.setAvailableBeds(availBeds);
        rt.setHasAc(ac);
        rt.setHasAttachedBathroom(bath);
        rt.setHasBalcony(balcony);
        p.getRoomTypes().add(rt);
        p.setTotalBeds(p.getTotalBeds() + totalBeds);
        p.setAvailableBeds(p.getAvailableBeds() + availBeds);
    }

    private void addImage(Property p, String url, String caption, String category, boolean isPrimary, int order) {
        PropertyImage img = new PropertyImage();
        img.setProperty(p);
        img.setImageUrl(url);
        img.setThumbnailUrl(url);
        img.setCaption(caption);
        img.setCategory(category);
        img.setIsPrimary(isPrimary);
        img.setDisplayOrder(order);
        p.getImages().add(img);
    }
}

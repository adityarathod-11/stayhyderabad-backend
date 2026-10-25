-- V1__init_schema.sql: StayHyderabad Initial Database Schema

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL, -- ROLE_USER, ROLE_OWNER, ROLE_ADMIN
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, PENDING_VERIFICATION
    avatar_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS owner_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    business_name VARCHAR(150),
    govt_id_type VARCHAR(50),
    govt_id_number VARCHAR(100),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    alternate_phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS properties (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    gender_category VARCHAR(20) NOT NULL, -- MALE, FEMALE, CO_LIVING
    locality VARCHAR(100) NOT NULL, -- Madhapur, Gachibowli, HITEC City, etc.
    address_line TEXT NOT NULL,
    landmark VARCHAR(200),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    description TEXT,
    house_rules TEXT,
    gate_closing_time VARCHAR(30) DEFAULT '10:30 PM',
    food_included BOOLEAN NOT NULL DEFAULT TRUE,
    food_type VARCHAR(50) NOT NULL DEFAULT 'VEG_AND_NON_VEG', -- VEG_ONLY, VEG_AND_NON_VEG, NONE
    starting_monthly_rent NUMERIC(10, 2) NOT NULL,
    security_deposit NUMERIC(10, 2) NOT NULL,
    maintenance_charge NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    notice_period_days INT NOT NULL DEFAULT 30,
    status VARCHAR(25) NOT NULL DEFAULT 'PENDING_APPROVAL', -- DRAFT, PENDING_APPROVAL, PUBLISHED, REJECTED, ARCHIVED
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    total_beds INT NOT NULL DEFAULT 0,
    available_beds INT NOT NULL DEFAULT 0,
    contact_phone VARCHAR(20),
    whatsapp_number VARCHAR(20),
    last_availability_update TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS room_types (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    sharing_type VARCHAR(30) NOT NULL, -- SINGLE, DOUBLE, TRIPLE, FOUR_SHARING
    monthly_rent NUMERIC(10, 2) NOT NULL,
    security_deposit NUMERIC(10, 2) NOT NULL,
    total_beds INT NOT NULL DEFAULT 1,
    available_beds INT NOT NULL DEFAULT 1,
    has_ac BOOLEAN NOT NULL DEFAULT FALSE,
    has_attached_bathroom BOOLEAN NOT NULL DEFAULT TRUE,
    has_balcony BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS property_images (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    public_id VARCHAR(200),
    caption VARCHAR(150),
    category VARCHAR(50) DEFAULT 'GENERAL', -- GENERAL, BEDROOM, BATHROOM, DINING, COMMON_AREA
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS amenities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    icon_name VARCHAR(50) NOT NULL,
    category VARCHAR(50) DEFAULT 'GENERAL',
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS property_amenities (
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (property_id, amenity_id)
);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(30) NOT NULL UNIQUE,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE RESTRICT,
    room_type_id BIGINT NOT NULL REFERENCES room_types(id) ON DELETE RESTRICT,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preferred_move_in_date DATE NOT NULL,
    num_persons INT NOT NULL DEFAULT 1,
    applicant_name VARCHAR(100) NOT NULL,
    applicant_phone VARCHAR(20) NOT NULL,
    applicant_email VARCHAR(150) NOT NULL,
    message TEXT,
    monthly_rent_locked NUMERIC(10, 2) NOT NULL,
    deposit_locked NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED
    owner_note TEXT,
    beds_allocated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS enquiries (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(150),
    message TEXT NOT NULL,
    is_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS favourites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_property UNIQUE (user_id, property_id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT REFERENCES properties(id) ON DELETE CASCADE,
    reporter_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reason VARCHAR(100) NOT NULL,
    details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RESOLVED, DISMISSED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_properties_locality ON properties(locality);
CREATE INDEX IF NOT EXISTS idx_properties_gender ON properties(gender_category);
CREATE INDEX IF NOT EXISTS idx_properties_status ON properties(status);
CREATE INDEX IF NOT EXISTS idx_properties_rent ON properties(starting_monthly_rent);
CREATE INDEX IF NOT EXISTS idx_room_types_property ON room_types(property_id);
CREATE INDEX IF NOT EXISTS idx_bookings_user ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_property ON bookings(property_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_favourites_user ON favourites(user_id);

-- Seed standard amenities
INSERT INTO amenities (name, icon_name, category, description) VALUES
('High-speed Wi-Fi', 'Wifi', 'CONNECTIVITY', 'Fiber internet connectivity across all rooms'),
('3-Time Homestyle Food', 'Utensils', 'FOOD', 'Healthy breakfast, lunch, and dinner included'),
('Daily Housekeeping', 'Sparkles', 'CLEANING', 'Daily cleaning of rooms and bathrooms'),
('Washing Machine', 'Shirt', 'LAUNDRY', 'Automatic washing machines available for self-service'),
('24/7 Power Backup', 'Zap', 'UTILITY', 'Uninterrupted power with generator support'),
('RO Purified Drinking Water', 'Droplets', 'HEALTH', 'Continuous cold and regular purified water dispenser'),
('Air Conditioning (AC)', 'Wind', 'COMFORT', 'Efficient split AC units installed in room'),
('Attached Bathroom', 'Bath', 'COMFORT', 'Private western bathroom with modern sanitary fittings'),
('Geyser / Hot Water', 'Flame', 'COMFORT', 'Instant hot water available in all bathrooms'),
('CCTV Surveillance & Security', 'ShieldCheck', 'SECURITY', '24/7 camera monitoring in common areas and biometric gate'),
('Refrigerator', 'Refrigerator', 'KITCHEN', 'Shared refrigerator for food and beverages'),
('Two-Wheeler Parking', 'Bike', 'PARKING', 'Secure shaded parking area for motorbikes and scooters'),
('Four-Wheeler Parking', 'Car', 'PARKING', 'Car parking space subject to prior allocation'),
('Gym & Fitness Zone', 'Dumbbell', 'LEISURE', 'Basic workout equipment and open terrace fitness area');

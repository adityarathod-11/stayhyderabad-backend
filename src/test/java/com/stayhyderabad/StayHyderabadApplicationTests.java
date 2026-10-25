package com.stayhyderabad;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class StayHyderabadApplicationTests {

    @Test
    void contextLoads() {
        // Confirms Spring context, Flyway migration, and security beans load cleanly
    }
}

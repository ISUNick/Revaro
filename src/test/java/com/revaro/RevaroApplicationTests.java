package com.revaro;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RevaroApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context starts without errors
    }
}

package com.sealhackathon.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "security.jwt.enabled=false",
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:mysql://localhost:3306/SealHackathon",
        "spring.datasource.username=root",
        "spring.datasource.password=12345",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
})
class BeApplicationTests {

    @Test
    void contextLoads() {
    }

}

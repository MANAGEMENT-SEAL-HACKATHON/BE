package com.sealhackathon.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "security.jwt.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:becontext;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.storage.type=local",
        "app.storage.local-dir=target/test-uploads",
        "app.submission.github-public-check-enabled=false"
})
class BeApplicationTests {

    @Test
    void contextLoads() {
    }

}

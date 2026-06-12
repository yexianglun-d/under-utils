package com.undernine.utils.samples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.main.banner-mode=off",
        "spring.main.log-startup-info=false",
        "logging.level.root=WARN"
})
class UnderUtilsSamplesApplicationTest {

    @Test
    void contextLoads() {
    }
}

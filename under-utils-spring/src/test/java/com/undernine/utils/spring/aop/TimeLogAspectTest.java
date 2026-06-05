package com.undernine.utils.spring.aop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TimeLogAspect} 单元测试
 *
 * @author Under-Utils Team
 */
@SpringJUnitConfig(TimeLogAspectTest.TestConfig.class)
@SuppressWarnings("deprecation")
class TimeLogAspectTest {

    @Autowired
    private TestService testService;

    @Test
    void testTimeLogWithDefaultSettings() {
        String result = testService.fastMethod();
        assertThat(result).isEqualTo("fast");
    }

    @Test
    void testTimeLogWithCustomDescription() {
        String result = testService.methodWithDescription();
        assertThat(result).isEqualTo("described");
    }

    @Test
    void testTimeLogWithSlowMethod() throws InterruptedException {
        String result = testService.slowMethod();
        assertThat(result).isEqualTo("slow");
    }

    @Test
    void testTimeLogWithException() {
        assertThatThrownBy(() -> testService.methodWithException())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("测试异常");
    }

    @Test
    void testTimeLogWithCustomThreshold() throws InterruptedException {
        String result = testService.methodWithCustomThreshold();
        assertThat(result).isEqualTo("custom");
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        public TimeLogAspect timeLogAspect() {
            return new TimeLogAspect();
        }

        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    static class TestService {

        @TimeLog
        public String fastMethod() {
            return "fast";
        }

        @TimeLog("自定义描述")
        public String methodWithDescription() {
            return "described";
        }

        @TimeLog(value = "慢方法", slowThreshold = 50)
        public String slowMethod() throws InterruptedException {
            Thread.sleep(100);
            return "slow";
        }

        @TimeLog
        public String methodWithException() {
            throw new RuntimeException("测试异常");
        }

        @TimeLog(value = "自定义阈值", slowThreshold = 200)
        public String methodWithCustomThreshold() throws InterruptedException {
            Thread.sleep(50);
            return "custom";
        }
    }
}

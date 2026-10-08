package vn.ticketscenter.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ApplicationConfigTest {
    private Map<String, String> valid() {
        Map<String, String> env = new HashMap<>();
        env.put("TC_SQL_HOST", "localhost");
        env.put("TC_DATABASE", "ticketscenter_test");
        env.put("TC_APP_BASE_URL", "http://localhost:8080/ticketscenter");
        env.put("TC_OTP_HMAC_SECRET", Base64.getEncoder().encodeToString(new byte[32]));
        return env;
    }

    @Test
    void validatesDefaults() {
        var config = ApplicationConfig.from(valid());
        assertEquals(5, config.getDbPoolSize());
        assertEquals(30, config.getSessionTimeoutMinutes());
        assertFalse(config.isDemoMode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TC_SQL_HOST", "TC_DATABASE", "TC_APP_BASE_URL", "TC_OTP_HMAC_SECRET"})
    void missingRequiredValueFails(String key) {
        var env = valid();
        env.remove(key);
        assertTrue(
                assertThrows(IllegalStateException.class, () -> ApplicationConfig.from(env))
                        .getMessage()
                        .contains(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "6", "secret-value", "2147483648"})
    void poolBudgetCannotExceedFiveOrExposeValue(String value) {
        var env = valid();
        env.put("TC_DB_POOL_SIZE", value);
        var failure = assertThrows(IllegalStateException.class, () -> ApplicationConfig.from(env));
        assertEquals(
                "Missing or invalid environment variable: TC_DB_POOL_SIZE", failure.getMessage());
    }

    @Test
    void productionRequiresHttpsAndRejectsDemo() {
        var env = valid();
        env.put("TC_APP_ENV", "production");
        assertThrows(IllegalStateException.class, () -> ApplicationConfig.from(env));
        env.put("TC_APP_BASE_URL", "https://tickets.example.test");
        env.put("TC_DEMO_ADMIN_ENABLED", "true");
        assertThrows(IllegalStateException.class, () -> ApplicationConfig.from(env));
    }

    @Test
    void passwordsAreNeverTrimmedOrPrinted() {
        var env = valid();
        env.put("TC_DB_USER", "broker");
        env.put("TC_DB_PASSWORD", " secret ");
        assertEquals(" secret ", ApplicationConfig.from(env).getDbPassword());
    }

    @Test
    void rejectsShortHmacAndUnknownBoolean() {
        var env = valid();
        env.put("TC_OTP_HMAC_SECRET", "c2hvcnQ=");
        assertThrows(IllegalStateException.class, () -> ApplicationConfig.from(env));
        var invalid = valid();
        invalid.put("TC_DEMO_ADMIN_ENABLED", "maybe");
        assertThrows(IllegalStateException.class, () -> ApplicationConfig.from(invalid));
    }
}

package com.optical.net.sisplus;

import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.entity.Configuration;
import com.optical.net.sisplus.app.infrastructure.repository.ConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.TimeZone;

@Slf4j
@SpringBootApplication
public class SisPlusApplication implements CommandLineRunner {

    private final ConfigurationRepository configurationRepository;

    public SisPlusApplication(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(SisPlusApplication.class, args);
    }

    @Override
    public void run(String... args) {

        String timeZone = getOrCreate("TIME_ZONE", "America/Bogota");

        UserDomain.REGULAR_HOUR_RATE =
                Double.parseDouble(getOrCreate("REGULAR_HOUR_RATE", "7959"));

        UserDomain.DAY_OVERTIME_RATE =
                Double.parseDouble(getOrCreate("DAY_OVERTIME_RATE", "9948"));

        UserDomain.NIGHT_SURCHARGE_RATE =
                Double.parseDouble(getOrCreate("NIGHT_SURCHARGE_RATE", "2786"));

        UserDomain.NIGHT_OVERTIME_RATE =
                Double.parseDouble(getOrCreate("NIGHT_OVERTIME_RATE", "13928.25"));

        UserDomain.NIGHT_START_HOUR =
                Integer.parseInt(getOrCreate("NIGHT_START_HOUR", "19"));

        UserDomain.NIGHT_END_HOUR =
                Integer.parseInt(getOrCreate("NIGHT_END_HOUR", "6"));

        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
    }

    private String getOrCreate(String key, String defaultValue) {
        return configurationRepository.findByKey(key)
                .map(Configuration::getValue)
                .orElseGet(() -> {
                    Configuration config = Configuration.builder()
                            .key(key)
                            .value(defaultValue)
                            .build();
                    configurationRepository.save(config);
                    return defaultValue;
                });
    }
}


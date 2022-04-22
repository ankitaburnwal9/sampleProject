package net.apmoller.crb.ohm.microservices.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The application class for the sample spring boot service.
 */
@SpringBootApplication
public class SampleApplication {

    /**
     * Standalone spring boot starter.
     *
     * @param args
     *            arguments for the spring boot app run.
     */
    public static void main(String... args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}

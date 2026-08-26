package gg.duo.crew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"gg.duo.crew", "gg.duo.common"})
public class CrewApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrewApplication.class, args);
    }
}
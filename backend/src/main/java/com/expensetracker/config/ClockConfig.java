package com.expensetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Uses the computer's time zone, so "today" matches the user's calendar. Tests replace this bean. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

package com.fynxt.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OrderBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderBookingApplication.class, args);
    }
}

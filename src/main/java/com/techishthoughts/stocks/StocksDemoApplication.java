package com.techishthoughts.stocks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.techishthoughts.stocks")
public class StocksDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(StocksDemoApplication.class, args);
    }
}

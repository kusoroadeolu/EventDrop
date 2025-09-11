package com.victor.EventDrop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableRedisRepositories
public class EventDropApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventDropApplication.class, args);
	}

}

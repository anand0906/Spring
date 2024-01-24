package com.anand;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@Scope("singleton")
public class Airtel implements Sim {
	
	@PostConstruct
	public void init() {
		System.out.println("Airtel Sim is Created");
	}
	
	@PreDestroy
	public void destroy() {
		System.out.println("Jio Sim is getting deleted");
	}

	@Override
	public void makeCall() {
		System.out.println("Calling Using Airtel Sim !");
	}

}

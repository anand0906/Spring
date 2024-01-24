package com.anand;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ComponentScan()
public class JavaConfig {
	
//	@Bean(name="airtel")
//	@Scope("singleton")
//	Airtel airtel() {
//		return new Airtel();
//	}
//	
//	@Bean(name="jio")
//	@Scope("prototype")
//	Jio jio() {
//		return new Jio();
//	}
//	
//	@Bean
//	Phone phone() {
//		return new Phone();
//	}
	
	

}

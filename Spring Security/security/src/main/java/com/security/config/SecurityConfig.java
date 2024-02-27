package com.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@SuppressWarnings("deprecation")
@Configuration
public class SecurityConfig {
	
//	@Bean
//	UserDetailsService userDetailsService() {
//		var uds=new InMemoryUserDetailsManager();
//		
//		var user=User.withUsername("anand").password("anand").authorities("read").build();
//		
//		uds.createUser(user);
//		return uds;
//	}
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return NoOpPasswordEncoder.getInstance();
	}

}

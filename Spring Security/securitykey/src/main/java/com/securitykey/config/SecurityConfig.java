package com.securitykey.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	CustomAuthenticationFilter customAuthenticationFilter;

	public SecurityConfig(CustomAuthenticationFilter customAuthenticationFilter) {
		super();
		this.customAuthenticationFilter = customAuthenticationFilter;
	}

	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		return httpSecurity
				.addFilterAt(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(request -> request.anyRequest().authenticated())
				.build();
	}

}

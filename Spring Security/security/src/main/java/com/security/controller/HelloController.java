package com.security.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
	
	
	@GetMapping("/hello")
	public String getHello() {
		Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
		System.out.println(authentication.getPrincipal());
		return "Hello";
	}

}

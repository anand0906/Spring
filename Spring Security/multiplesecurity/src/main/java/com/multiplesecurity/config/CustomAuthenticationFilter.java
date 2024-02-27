package com.multiplesecurity.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomAuthenticationFilter extends OncePerRequestFilter {

	private final String key;

	public CustomAuthenticationFilter(String key) {
		super();
		this.key = key;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		System.out.println("Calling CustomAuthenticationFilter");

		CustomAuthenticationManager manager = new CustomAuthenticationManager(key);
		
		String requestKey=request.getHeader("key");
		
		if(requestKey==null || "null".equals(requestKey)) {
			filterChain.doFilter(request, response);
		}else {

		CustomAuthentication authentication = new CustomAuthentication(requestKey, false);
		try {
			Authentication auth = manager.authenticate(authentication);
			if (authentication.isAuthenticated()) {
				SecurityContextHolder.getContext().setAuthentication(auth);
				filterChain.doFilter(request, response);
			} else {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			}
		} catch (AuthenticationException exception) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}

	}
	}

}

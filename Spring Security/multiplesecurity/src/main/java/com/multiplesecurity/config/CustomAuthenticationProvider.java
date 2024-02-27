package com.multiplesecurity.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class CustomAuthenticationProvider implements AuthenticationProvider {
	
	private final String key;

	public CustomAuthenticationProvider(String key) {
		super();
		this.key = key;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		CustomAuthentication auth=(CustomAuthentication)authentication;
		
		if(key.equals(auth.getKey())) {
			auth.setAuthenticated(true);
			return auth;
		}
		
		throw new BadCredentialsException("Invalid key");
		
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return CustomAuthentication.class.equals(authentication);
	}

}

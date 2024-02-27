package com.multiplesecurity.config;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class CustomAuthenticationManager implements AuthenticationManager {

	private final String key;

	public CustomAuthenticationManager(String key) {
		super();
		this.key = key;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		CustomAuthenticationProvider provider = new CustomAuthenticationProvider(key);

		if (provider.supports(authentication.getClass())) {
			return provider.authenticate(authentication);
		}

		return authentication;

	}

}

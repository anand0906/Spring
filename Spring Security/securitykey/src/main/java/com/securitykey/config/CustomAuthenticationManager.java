package com.securitykey.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationManager implements AuthenticationManager {
	
	@Autowired
	CustomAuthenticationProvider authenticationProvider;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		if(authenticationProvider.supports(authentication.getClass())) {
			return authenticationProvider.authenticate(authentication);
		}
		
		throw new BadCredentialsException("key is invalid!");
	}

}

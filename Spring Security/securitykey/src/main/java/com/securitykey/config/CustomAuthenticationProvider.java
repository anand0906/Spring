package com.securitykey.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
	
	@Value("${mykey}")
	private String key;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		
		CustomAuthentication customAuthentication=(CustomAuthentication)authentication;
		
		if(key.equals(customAuthentication.getKey())) {
			return new CustomAuthentication(true,null);
		}
		
		throw new BadCredentialsException("Key is invalid");
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return CustomAuthentication.class.equals(authentication);
	}

}

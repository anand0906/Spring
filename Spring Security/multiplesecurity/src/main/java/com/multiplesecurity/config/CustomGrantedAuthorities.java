package com.multiplesecurity.config;

import org.springframework.security.core.GrantedAuthority;

import com.multiplesecurity.domain.Authorities;

public class CustomGrantedAuthorities implements GrantedAuthority {
	

	private static final long serialVersionUID = 1L;
	
	private final Authorities authorities;


	public CustomGrantedAuthorities(Authorities authorities) {
		super();
		this.authorities = authorities;
	}

	@Override
	public String getAuthority() {
		return authorities.getName();
	}

}

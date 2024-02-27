package com.security.config;

import org.springframework.security.core.GrantedAuthority;

import com.security.domain.Authorities;

public class SecurityAuthority implements GrantedAuthority {

	private static final long serialVersionUID = 1L;

	private final Authorities authority;

	public SecurityAuthority(Authorities authority) {
		super();
		this.authority = authority;
	}

	@Override
	public String getAuthority() {
		return authority.getName();
	}

}

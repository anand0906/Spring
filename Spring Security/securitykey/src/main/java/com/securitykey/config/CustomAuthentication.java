package com.securitykey.config;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class CustomAuthentication implements Authentication {
	

	private static final long serialVersionUID = 1L;

	private final boolean authenticated;
	
	private final String key;
	
	public CustomAuthentication(boolean authenticated, String key) {
		super();
		this.authenticated = authenticated;
		this.key = key;
	}

	@Override
	public boolean isAuthenticated() {
		return authenticated;
	}
	
	
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getKey() {
		return key;
	}

	@Override
	public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getCredentials() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getDetails() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

}

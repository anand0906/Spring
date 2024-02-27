package com.security.config;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.security.domain.Users;
import com.security.repository.UsersRepository;

@Service
public class JpaUserDetailsService implements UserDetailsService {
	
	UsersRepository repository;


	public JpaUserDetailsService(UsersRepository repository) {
		super();
		this.repository = repository;
	}


	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		Users user=repository.findByUsername(username);
		
		if(user==null) {
			throw new UsernameNotFoundException("Username not found");
		}
		
		return new SecurityUser(user);
	}

}

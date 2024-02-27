package com.multiplesecurity.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.multiplesecurity.domain.Users;
import com.multiplesecurity.repository.UsersRepository;

@Service
public class JpaUserDetailsService implements UserDetailsService {
	
	@Autowired
	UsersRepository repository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		System.out.println("Calling JpaUserDetailsService");
		
		Users user=repository.findByUsername(username);
		
		if(user!=null) {
			return new CustomUserDetails(user);
		}
		
		throw new UsernameNotFoundException("Invalid username");
	}

}

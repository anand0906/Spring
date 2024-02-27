package com.multiplesecurity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.multiplesecurity.domain.Users;

public interface UsersRepository extends JpaRepository<Users, Integer> {
	
	Users findByUsername(String username);

}

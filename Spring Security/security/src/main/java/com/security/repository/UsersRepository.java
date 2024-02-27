package com.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.security.domain.Users;

public interface UsersRepository extends JpaRepository<Users, Integer> {

	Users findByUsername(String username);

}

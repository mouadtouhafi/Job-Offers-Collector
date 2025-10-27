package com.websolutions.companies.collection.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.websolutions.companies.collection.entites.FlagsEntity;

public interface FlagsRepository extends JpaRepository<FlagsEntity, Long>{
	boolean existsByName(String name);
}

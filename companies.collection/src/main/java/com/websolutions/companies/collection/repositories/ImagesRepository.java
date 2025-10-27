package com.websolutions.companies.collection.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.websolutions.companies.collection.entites.ImagesEntity;

public interface ImagesRepository extends JpaRepository<ImagesEntity, Long>{
	boolean existsByName(String name);
}

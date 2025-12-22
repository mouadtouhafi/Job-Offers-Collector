package com.websolutions.companies.collection.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.websolutions.companies.collection.entites.FieldsImagesEntity;

public interface FieldsImagesRepository extends JpaRepository<FieldsImagesEntity, Long>{
	boolean existsByName(String name);
}

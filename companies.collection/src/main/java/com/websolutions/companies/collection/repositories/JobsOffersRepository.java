package com.websolutions.companies.collection.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.websolutions.companies.collection.entites.JobsOffers;

public interface JobsOffersRepository extends JpaRepository<JobsOffers, Long>{
	
	boolean existsByTitleAndCompanyAndLocationAndUrl(String title, String company, String location, String url);
	
}

package com.websolutions.companies.collection.services.repositories;

import org.springframework.stereotype.Service;

import com.websolutions.companies.collection.repositories.ImagesRepository;

@Service
public class ImportImagesService {
	
	private ImagesRepository imagesRepository;
	
	public ImportImagesService(ImagesRepository imagesRepository) {
		this.imagesRepository = imagesRepository;
	}
	
	
}

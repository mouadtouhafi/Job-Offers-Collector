package com.websolutions.companies.collection.services.repositories;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.websolutions.companies.collection.entites.ImagesEntity;
import com.websolutions.companies.collection.repositories.ImagesRepository;



@Service
public class ImportImagesService {
	
	private ImagesRepository imagesRepository;
	
	public ImportImagesService(ImagesRepository imagesRepository) {
		this.imagesRepository = imagesRepository;
	}
	
	public void loadImages(String folderPath) throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resolver.getResources("classpath:" + folderPath + "/*.{png,jpg,jpeg}");
		
		for(Resource resource : resources) {
			try (InputStream is = resource.getInputStream()){
				byte[] data = is.readAllBytes();
				String fileName = resource.getFilename().split("\\.")[0];
				String fileType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
				
				ImagesEntity image = new ImagesEntity(fileName, fileType, data);
				if(!imagesRepository.existsByName(fileName)) {
					imagesRepository.save(image);
				}
			}
		}
		
		
	}
	
}

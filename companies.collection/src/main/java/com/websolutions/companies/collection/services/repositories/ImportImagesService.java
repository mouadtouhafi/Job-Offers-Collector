package com.websolutions.companies.collection.services.repositories;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.websolutions.companies.collection.entites.FieldsImagesEntity;
import com.websolutions.companies.collection.entites.FlagsEntity;
import com.websolutions.companies.collection.entites.ImagesEntity;
import com.websolutions.companies.collection.repositories.FieldsImagesRepository;
import com.websolutions.companies.collection.repositories.FlagsRepository;
import com.websolutions.companies.collection.repositories.ImagesRepository;



@Service
public class ImportImagesService {
	
	private ImagesRepository imagesRepository;
	private FlagsRepository flagsRepository;
	private FieldsImagesRepository fieldsImagesRepository;
	
	public ImportImagesService(ImagesRepository imagesRepository, FlagsRepository flagsRepository, FieldsImagesRepository fieldsImagesRepository) {
		this.imagesRepository = imagesRepository;
		this.flagsRepository = flagsRepository;
		this.fieldsImagesRepository = fieldsImagesRepository;
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
	
	public void loadFlags(String folderPath) throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resolver.getResources("classpath:" + folderPath + "/*.{png,jpg,jpeg}");
		
		for(Resource resource : resources) {
			try (InputStream is = resource.getInputStream()){
				byte[] data = is.readAllBytes();
				String fileName = resource.getFilename().split("\\.")[0];
				String fileType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
				
				FlagsEntity image = new FlagsEntity(fileName, fileType, data);
				if(!flagsRepository.existsByName(fileName)) {
					flagsRepository.save(image);
				}
			}
		}
	}
	
	public void loadFieldsImages(String folderPath) throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resolver.getResources("classpath:" + folderPath + "/*.{png,jpg,jpeg}");
		
		for(Resource resource : resources) {
			try (InputStream is = resource.getInputStream()){
				byte[] data = is.readAllBytes();
				String fileName = resource.getFilename().split("\\.")[0];
				String fileType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
				
				FieldsImagesEntity image = new FieldsImagesEntity(fileName, fileType, data);
				if(!fieldsImagesRepository.existsByName(fileName)) {
					fieldsImagesRepository.save(image);
				}
			}
		}
	}
	
}

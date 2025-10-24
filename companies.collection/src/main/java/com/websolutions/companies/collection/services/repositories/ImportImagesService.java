package com.websolutions.companies.collection.services.repositories;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.websolutions.companies.collection.entites.ImagesEntity;
import com.websolutions.companies.collection.repositories.ImagesRepository;

@Service
public class ImportImagesService {
	
	private ImagesRepository imagesRepository;
	
	public ImportImagesService(ImagesRepository imagesRepository) {
		this.imagesRepository = imagesRepository;
	}
	
	public void loadImages(String folderPath) throws IOException {
		File folder = new File(folderPath);
		if(!folder.exists() || !folder.isDirectory()) {
			throw new IllegalArgumentException("Folder path is invalid : " + folderPath);
		}
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
		if(files == null) {
			return;
		}
		
		for(File file : files) {
			byte[] data = Files.readAllBytes(file.toPath());
			String fileType = Files.probeContentType(file.toPath());
			ImagesEntity image = new ImagesEntity(file.getName(), fileType, data);
			imagesRepository.save(image);
		}
	}
	
}

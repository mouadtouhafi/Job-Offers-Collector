package com.websolutions.companies.collection;

import java.io.IOException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.websolutions.companies.collection.modelAI.PredictTitle;
import com.websolutions.companies.collection.modelAI.TrainAndSave;
import com.websolutions.companies.collection.services.AkkodisJobCollector;
import com.websolutions.companies.collection.services.AltenJobCollector;
import com.websolutions.companies.collection.services.ApsideJobCollector;
import com.websolutions.companies.collection.services.AvlJobCollector;
import com.websolutions.companies.collection.services.CapgeminiEngineeringJobCollector;
import com.websolutions.companies.collection.services.CapgeminiJobCollector;
import com.websolutions.companies.collection.services.DevoteamJobCollector;
import com.websolutions.companies.collection.services.ExpleoJobCollector;
import com.websolutions.companies.collection.services.HirschmannAutomotiveJobCollector;
import com.websolutions.companies.collection.services.repositories.ImportImagesService;

@SpringBootApplication
public class Application implements CommandLineRunner {

	private final ImportImagesService importImagesService;
	
    private final CapgeminiEngineeringJobCollector capgeminiJobCollector;
    private final AvlJobCollector avlJobCollector;
    private final HirschmannAutomotiveJobCollector hirschmannAutomotiveJobCollector;
    
    private final TrainAndSave trainAndSave;

    public Application(ImportImagesService importImagesService, CapgeminiEngineeringJobCollector capgeminiJobCollector, AvlJobCollector avlJobCollector, HirschmannAutomotiveJobCollector hirschmannAutomotiveJobCollector, TrainAndSave trainAndSave) {
        this.importImagesService = importImagesService;
    	this.capgeminiJobCollector = capgeminiJobCollector;
        this.avlJobCollector = avlJobCollector;
        this.hirschmannAutomotiveJobCollector = hirschmannAutomotiveJobCollector;
        this.trainAndSave = trainAndSave;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
    	
    	trainAndSave.trainData();
    	System.out.println(" ---------------------------------------------- ");
    	new PredictTitle().predictField("Diagnostic automotive engineer");

    	//importImagesService.loadImages("Logos");
    	//importImagesService.loadFlags("Flags");
    	
    	//capgeminiEngineeringJobCollector.getMoroccanJobs(false);
//    	capgeminiJobCollector.getMoroccanJobs(false);
//    	avlJobCollector.getFulljobs(false);
    	//hirschmannAutomotiveJobCollector.getFulljobs(false);
    }
}

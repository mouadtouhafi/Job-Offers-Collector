package com.websolutions.companies.collection;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.websolutions.companies.collection.services.AkkodisJobCollector;
import com.websolutions.companies.collection.services.AltenJobCollector;
import com.websolutions.companies.collection.services.AvlJobCollector;
import com.websolutions.companies.collection.services.ExpleoJobCollector;
import com.websolutions.companies.collection.services.HirschmannAutomotiveJobCollector;
import com.websolutions.companies.collection.services.LearJobCollection;
import com.websolutions.companies.collection.services.StellantisJobCollector;

@SpringBootApplication
public class Application implements CommandLineRunner {

    private final ExpleoJobCollector expleoJobCollector;
    private final AltenJobCollector altenJobCollector;
    private final AvlJobCollector avlJobCollector;
    private final LearJobCollection learJobCollection;
    private final AkkodisJobCollector akkodisJobCollector;
    private final StellantisJobCollector stellantisJobCollector;
    private final HirschmannAutomotiveJobCollector hirschmannAutomotiveJobCollector;

    public Application(ExpleoJobCollector expleoJobCollector, 
    				   AltenJobCollector altenJobCollector, 
    				   AvlJobCollector avlJobCollector,
    				   LearJobCollection learJobCollection,
    				   AkkodisJobCollector akkodisJobCollector,
    				   StellantisJobCollector stellantisJobCollector,
    				   HirschmannAutomotiveJobCollector hirschmannAutomotiveJobCollector) {
        this.expleoJobCollector = expleoJobCollector;
        this.altenJobCollector = altenJobCollector;
        this.avlJobCollector = avlJobCollector;
        this.learJobCollection = learJobCollection;
        this.akkodisJobCollector = akkodisJobCollector;
        this.stellantisJobCollector = stellantisJobCollector;
        this.hirschmannAutomotiveJobCollector = hirschmannAutomotiveJobCollector;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws InterruptedException {
        //expleoJobCollector.scrapeJobs();
    	
//        altenJobCollector.getCountries();
////        altenJobCollector.getForeignJobs_1();
////        altenJobCollector.getForeignJobs_2();
//        altenJobCollector.getForeignJobs_3();
//        altenJobCollector.closeDriver();
    	
//    	avlJobCollector.getFulljobs();
    	
//    	learJobCollection.getFulljobs();
    	
//    	akkodisJobCollector.getFulljobs();
    	
//    	stellantisJobCollector.getFulljobs();
    	
    	hirschmannAutomotiveJobCollector.getFulljobs();
    }
}

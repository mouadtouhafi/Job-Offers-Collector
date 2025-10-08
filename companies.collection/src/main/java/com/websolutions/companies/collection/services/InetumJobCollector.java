package com.websolutions.companies.collection.services;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.websolutions.companies.collection.entites.JobsOffers;
import com.websolutions.companies.collection.repositories.JobsOffersRepository;

@Service
public class InetumJobCollector {
	
	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
    private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
    private final HashMap<Integer, String> jobsLinks = new HashMap<>();
    private final JobsOffersRepository jobsOffersRepository;
    private WebDriver driver;
    private EdgeOptions options;
    private boolean isFinalPageReached = false;
    private int maxNumberOfPagesClicked = 3;
    private String InetumLink = "https://www.inetum.com/en/jobs";
    
    
    public InetumJobCollector(JobsOffersRepository jobsOffersRepository) {
		super();
		this.jobsOffersRepository = jobsOffersRepository;
	}
    
	public void getFulljobs(boolean isFullJobsCollection) throws MalformedURLException {
		int jobIndex = 0;
		
		options = new EdgeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--headless=new");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--lang=en-US");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-notifications");
		
        driver = new RemoteWebDriver(
        		URI.create("http://selenium:4444").toURL(),
        	    options
        	);
        
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		driver.get(InetumLink);
		
		//Submit the shoosen language
		WebElement langSubmitButton = driver.findElement(By.cssSelector("div.modal-content button.btn--primary"));
		safeClick(driver, langSubmitButton);
		
		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			/* Check if popup cookies is appearing */
			try {
				 //driver.switchTo().frame("iFrame1");
				 WebElement closeBtn = driver.findElement(By.cssSelector("button.agree-button.eu-cookie-compliance-default-button"));
				 safeClick(driver, closeBtn);
				 
				 //driver.switchTo().defaultContent();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			popupAppearedAndClosed = true;
		}

		while (!isFinalPageReached) {
			List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("div.container section.region--content div.views-view-grid div.card-body"))
				);
			System.out.println(jobs.size());
			for(WebElement job : jobs) {
				String job_title = job.findElement(By.tagName("h3")).getText();
				String contract_type = job.findElement(By.cssSelector("p.card-subtitle")).getText();
				String location = job.findElement(By.cssSelector("p.card-text")).getText();
				String job_link = "https://www.inetum.com" + job.findElement(By.tagName("a")).getDomAttribute("href");
				
				System.out.println(job_title +"  |  " +location+"  |  "+ contract_type+"  |  " +job_link);
				
				List<String> infos = new ArrayList<>();
				infos.add(job_title.strip());
				infos.add(location.strip());
				infos.add(contract_type.strip());
	
				id_jobInfo.put(jobIndex, infos);
				jobsLinks.put(jobIndex, job_link);
				jobIndex++;
					
				
			}
		
			
			try {
				
				List<WebElement> btnList = driver.findElements(
						By.cssSelector("ul.pagination li")
					); 
				
				
				
				if (btnList.isEmpty()) {
					System.out.println("================================================================>  button is empty" );
					isFinalPageReached = true;
				}else {
					WebElement nextBtn = btnList.get(btnList.size()-2);
					String nextBtnText = nextBtn.findElement(By.tagName("i")).getDomAttribute("class");
					if(nextBtnText.contains("forward")) {
						safeClick(driver, nextBtn);
						maxNumberOfPagesClicked--;
						if(isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
							isFinalPageReached = true;
						}
					}else {
						isFinalPageReached = true;
					}
				}
	         } catch (Exception e) {
	             isFinalPageReached = true;
	         }
		}
		
		
		
		for(int id=0; id<jobsLinks.size(); id++) {
			try {
				driver.get(jobsLinks.get(id));
				String innerHTML = wait.until(ExpectedConditions.presenceOfElementLocated(
						By.cssSelector("div.container div.col-md-7")))
						  .getDomProperty("innerHTML").replace("\n", "");
				System.out.println(innerHTML);
				
			
				String apply_link = driver.findElements(By.cssSelector("div.container a.btn.btn-accent")).getFirst().getDomAttribute("href");
					
				System.out.println(apply_link);
				System.out.println();
				
				JobsOffers jobOffer = new JobsOffers();
                jobOffer.setTitle(id_jobInfo.get(id).getFirst());
                jobOffer.setCompany("Inetum");
                jobOffer.setLocation(id_jobInfo.get(id).get(1));
                jobOffer.setUrl(apply_link);
                jobOffer.setContractType(id_jobInfo.get(id).get(2));
                jobOffer.setWorkMode("N/A");
                jobOffer.setPublishDate("N/A");
                jobOffer.setPost(innerHTML);
                if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
                		id_jobInfo.get(id).getFirst(), 
                		"Inetum", 
                		id_jobInfo.get(id).get(1), 
                		apply_link)){
                	
                	try {
                		jobsOffersRepository.save(jobOffer);
					} catch (DataIntegrityViolationException e) {
						logger.info("Duplicate detected: " + jobOffer.getTitle() + " @ " + jobOffer.getUrl());
					}
                }
			} catch (Exception e) {
				System.out.println("⚠️ Unexpected error at job " + id + " (" + jobsLinks.get(id) + "): " + e.getMessage());
		        continue;
			}
			
		}
		driver.quit();
		
		
	}
	
	
	public void safeClick(WebDriver driver, WebElement element) {
		try {
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
			Thread.sleep(500);
			element.click();
		} catch (ElementClickInterceptedException e) {
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
		} catch (Exception ex) {
			 System.out.println("Error clicking element: " + ex.getMessage());
		}
	}
}

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
public class SqliJobCollector {
	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
    private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
    private final HashMap<Integer, String> jobsLinks = new HashMap<>();
    private final JobsOffersRepository jobsOffersRepository;
    private WebDriver driver;
    private EdgeOptions options;
    private boolean isFinalPageReached = false;
    private int maxNumberOfPagesClicked = 5;
    private String SqliLink = "https://www.sqli.com/int-en/careers/our-jobs?f%5B0%5D=";
    
    public SqliJobCollector(JobsOffersRepository jobsOffersRepository) {
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
        options.addArguments("--window-size=1920,1080");
		
        driver = new RemoteWebDriver(
        		URI.create("http://selenium:4444").toURL(),
        	    options
        	);
        
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		driver.get(SqliLink);
		
		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			/* Check if popup cookies is appearing */
			try {
				 //driver.switchTo().frame("iFrame1");
				 WebElement closeBtn = driver.findElement(By.id("didomi-notice-agree-button"));
				 safeClick(driver, closeBtn);
				 
				 //driver.switchTo().defaultContent();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			popupAppearedAndClosed = true;
		}

		while (!isFinalPageReached) {
			List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("div.jobs-list div.jobs-list__jobs ul li"))
				);
			System.out.println(jobs.size());
			for(WebElement job : jobs) {
				String job_title = job.findElement(By.cssSelector("p.title-h3")).getText();
				String location = job.findElement(By.cssSelector("dl dd:nth-child(2)")).getText();
				String contract_type = job.findElement(By.cssSelector("dl dd:nth-child(4)")).getText();
				String job_link = "https://www.sqli.com" + 
						   job.findElement(By.cssSelector("p.title-h3 a"))
						   .getDomAttribute("href");
				
				String city = "N/A";
				String country = "N/A";
				
				String[] splitLocation = location.split(",");
				if(splitLocation.length >= 2) {
					city = splitLocation[0].strip();
					country = splitLocation[1].strip();
				}
				
				System.out.println(job_title+"  |  "+location+"  |  "+ contract_type+"  |  " +job_link);
				
				List<String> infos = new ArrayList<>();
				infos.add(job_title.strip());
				infos.add(city);
				infos.add(country);
				infos.add(contract_type.strip());
	
				id_jobInfo.put(jobIndex, infos);
				jobsLinks.put(jobIndex, job_link);
				jobIndex++;
					
				
			}
		
			try {
				List<WebElement> btnList = driver.findElements(
						By.cssSelector("div.jobs-list nav.pager ul li[class*='item--next']")
					); 
				
				if (btnList.isEmpty()) {
					System.out.println("================================================================>  button is empty" );
					isFinalPageReached = true;
				}else {
					String btnCssClass = btnList.getFirst().findElement(By.tagName("a")).getDomAttribute("class");
					System.out.println(btnCssClass);
					if(btnCssClass.contains("disabled")) {
						isFinalPageReached = true;
						System.out.println("================================================================>  button disabled" );
					}else {
						safeClick(driver, btnList.getFirst());
						maxNumberOfPagesClicked--;
						if(isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
							isFinalPageReached = true;
						}
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
						By.cssSelector("div.node-job-page div.node-job-page__wrapper div.node-job-page__description")))
						  .getDomProperty("innerHTML").replace("\n", "");
				
				System.out.println(innerHTML);
				
			
				String apply_link = driver.findElements(By.cssSelector("a.button-primary")).getFirst().getDomAttribute("href");
					
				System.out.println(apply_link);
				System.out.println();
				
				JobsOffers jobOffer = new JobsOffers();
                jobOffer.setTitle(id_jobInfo.get(id).getFirst());
                jobOffer.setCompany("SQLI");
                jobOffer.setCity(id_jobInfo.get(id).get(1));
                jobOffer.setCountry(id_jobInfo.get(id).get(2));
                jobOffer.setUrl(apply_link);
                jobOffer.setContractType(id_jobInfo.get(id).get(3));
                jobOffer.setWorkMode("N/A");
                jobOffer.setPublishDate("N/A");
                jobOffer.setPost(innerHTML);
                if (!jobsOffersRepository.existsByTitleAndCompanyAndCityAndCountryAndUrl(
                		id_jobInfo.get(id).getFirst(), 
                		"SQLI", 
                		id_jobInfo.get(id).get(1),
                		id_jobInfo.get(id).get(2),
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

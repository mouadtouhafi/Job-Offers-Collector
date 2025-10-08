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
import org.openqa.selenium.SearchContext;
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
public class ScalianJobCollector {
	
	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
    private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
    private final HashMap<Integer, String> jobsLinks = new HashMap<>();
    private final JobsOffersRepository jobsOffersRepository;
    private WebDriver driver;
    private EdgeOptions options;
    private String ScalianLink = "https://careers.scalian.com/en/jobs?q=&options=&page=1";
    private int maxNumberOfPagesClicked = 1;
    boolean isFinalPageReached = false;
	
	public ScalianJobCollector(JobsOffersRepository jobsOffersRepository) {
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

		driver.get(ScalianLink);
		
		
		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			try {
				// The accept cookie button is inside a shadow DOM, we Wait for the shadow host
				WebElement shadowHost = wait.until(
				    ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.needsclick"))
				);

				// Get the shadow root
				SearchContext shadowRoot = shadowHost.getShadowRoot();

				// Now wait until the button exists inside the shadow root
				WebElement acceptBtn = wait.until(d -> shadowRoot.findElement(By.id("axeptio_btn_acceptAll")));

				// Click safely
				//((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptBtn);
				safeClick(driver, acceptBtn);

			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			popupAppearedAndClosed = true;
		}
		
		
		//Here we choose to display the 48 jobs in a page
		List<WebElement> numberOfJobsPerPage = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(
				"div.row.dragElement.widget.container-widget.job-results__main-content-container div.attrax-pagination__resultsperpage"
			)))
				.getFirst()
				.findElements(By.tagName("a"));
		WebElement maxNumberOfPagesButton = numberOfJobsPerPage.getLast();
		safeClick(driver, maxNumberOfPagesButton);
		

		while (!isFinalPageReached) {
			List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("div.row.dragElement div.attrax-list-widget__lists div.attrax-list-widget__list div.attrax-vacancy-tile"))
				);
			for(WebElement job : jobs) {
				String job_title = job.findElement(By.cssSelector("a.attrax-vacancy-tile__title")).getText();
				String contract_type = job.findElement(By.cssSelector("div.attrax-vacancy-tile__option-contract-type p.attrax-vacancy-tile__item-value")).getText();
				String location = job.findElement(By.cssSelector("div.attrax-vacancy-tile__location-freetext p.attrax-vacancy-tile__item-value")).getText();
				String job_link = "https://careers.scalian.com" + job.findElement(By.cssSelector("a.attrax-vacancy-tile__title")).getDomAttribute("href");
				String domain = job.findElement(By.cssSelector("div.attrax-vacancy-tile__option-function p.attrax-vacancy-tile__item-value")).getDomProperty("innerHTML").replace("\n", "").strip();
				
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
				
				WebElement nextPageButton = driver.findElement(
						By.cssSelector("div.row.dragElement.widget.container-widget.job-results__main-content-container div.attrax-pagination__pagination ul li.attrax-pagination__next")
					); 
				safeClick(driver, nextPageButton);
				maxNumberOfPagesClicked--;
				if(isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
					isFinalPageReached = true;
					
				}
	         } catch (Exception e) {
	             isFinalPageReached = true;
	         }
		}
		
		
		
		for(int id=0; id<jobsLinks.size(); id++) {
			try {
				driver.get(jobsLinks.get(id));
				String innerHTML = wait.until(ExpectedConditions.presenceOfElementLocated(
						By.cssSelector("div.description-widget")))
						  .getDomProperty("innerHTML").replace("\n", "");
				System.out.println(innerHTML);
				
			
				String apply_link = "https://careers.scalian.com" + driver.findElements(By.cssSelector("div.job-details__details-container a.jobApplyBtn")).getFirst().getDomAttribute("href");
					
				System.out.println(apply_link);
				System.out.println();
				
				
				JobsOffers jobOffer = new JobsOffers();
                jobOffer.setTitle(id_jobInfo.get(id).getFirst());
                jobOffer.setCompany("Scalian");
                jobOffer.setLocation(id_jobInfo.get(id).get(1));
                jobOffer.setUrl(apply_link);
                jobOffer.setContractType(id_jobInfo.get(id).get(2));
                jobOffer.setWorkMode("N/A");
                jobOffer.setPublishDate("N/A");
                jobOffer.setPost(innerHTML);
                if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
                		id_jobInfo.get(id).getFirst(), 
                		"Scalian", 
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

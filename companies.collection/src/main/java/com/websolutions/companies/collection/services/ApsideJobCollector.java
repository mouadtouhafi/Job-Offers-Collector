package com.websolutions.companies.collection.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.websolutions.companies.collection.entites.JobsOffers;
import com.websolutions.companies.collection.repositories.JobsOffersRepository;

@Service
public class ApsideJobCollector {
	
	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
    private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
    private final HashMap<Integer, String> jobsLinks = new HashMap<>();
    private final JobsOffersRepository jobsOffersRepository;
    private WebDriver driver;
    private EdgeOptions options;
    private String ApsideLink = "https://www.apside.com/fr/nos-offres-emploi/";
    private int maxNumberOfPagesClicked = 3;
    boolean isFinalPageReached = false;
	
	public ApsideJobCollector(JobsOffersRepository jobsOffersRepository) {
		super();
		this.jobsOffersRepository = jobsOffersRepository;
	}
	
	public void getFulljobs(boolean isFullJobsCollection) {
		int jobIndex = 0;
		
		options = new EdgeOptions();

        // Enable headless mode
        //options.addArguments("--headless");

        // Optional: Add other arguments for optimization
		options.addArguments("--lang=en-US");
        options.addArguments("--disable-gpu"); // For Windows systems
        //options.addArguments("--window-size=1200,880"); // Set a specific window size
        options.addArguments("--disable-notifications"); // Disable pop-ups
		
		driver = new EdgeDriver(options);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		driver.get(ApsideLink);
		
		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			/* Check if popup cookies is appearing */
			try {
				 //driver.switchTo().frame("iFrame1");
				 WebElement closeBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tarteaucitronPersonalize2")));
				 safeClick(driver, closeBtn);
				 
				 //driver.switchTo().defaultContent();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			popupAppearedAndClosed = true;
		}
		
		// The website acts weirdly, we must reload the page by clicking the first button page in the pagination nav.
		boolean isFirstPageReloaded = false;
		if (!isFirstPageReloaded) {
			try {
				WebElement reloadFirstPage = wait.until(ExpectedConditions.elementToBeClickable(
						By.cssSelector("span.facetwp-page.first-page.active"))
					);
				safeClick(driver, reloadFirstPage);
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			isFirstPageReloaded = true;
		}
		
		
		
		while (!isFinalPageReached) {
			wait.until(ExpectedConditions.stalenessOf(driver.findElement(By.cssSelector("main.main section[class*='joboffergrid'] div.bp12-joboffergrid__posts div.card-list__item"))));
			List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("main.main section[class*='joboffergrid'] div.bp12-joboffergrid__posts div.card-list__item"))
				);
			
			System.out.println(jobs.size());
			for(WebElement job : jobs) {
				String job_title = job.findElement(
						By.cssSelector("div.card-job-offer__content h3.card-job-offer__title"))
						  .getText();
				
				List<WebElement> span_infos = job.findElements(By.cssSelector("div.card-job-offer__content div.card-job-offer__tags > span"));
				
				String publish_date = span_infos.getFirst().getText();
				String contract_type= "N/A";
				String location = "N/A";
				for(int j=1; j<span_infos.size(); j++) {
					String text = span_infos.get(j).getText().toUpperCase();
					if(text.contains("TEMPS PLEIN") || 
							text.contains("FULLTIME") || 
							text.contains("CDI") || 
							text.contains("PARTTIME") || 
							text.contains("FREELANCE")) {
						contract_type = text;
					}else {
						location = text;
					}
				}
				
				String job_link = job.findElement(By.tagName("a")).getDomAttribute("href");
				
				System.out.println(publish_date+"  |  "+ contract_type + "  |  "+location);
				
				
				System.out.println(dateCheckValabilityStatus(publish_date));
				if(dateCheckValabilityStatus(publish_date)) {
					List<String> infos = new ArrayList<>();
					infos.add(job_title.strip());
					infos.add(contract_type.strip());
					infos.add(publish_date.strip());
					infos.add(location);
		
					id_jobInfo.put(jobIndex, infos);
					jobsLinks.put(jobIndex, job_link);
					jobIndex++;
				}
				
					
				
			}
		
			try {
				List<WebElement> btnList = driver.findElements(
						By.cssSelector("div.facetwp-pager span")
					); 
				
				if (btnList.isEmpty()) {
					System.out.println("================================================================>  button is empty" );
					isFinalPageReached = true;
				}else {
					btnList.removeLast();
					btnList.removeFirst();
					for (int i = 0; i < btnList.size(); i++) {
						WebElement btn = btnList.get(i);
						String cssClass = btn.getDomAttribute("class");
						System.out.println(cssClass);
//
						if (cssClass.contains("active")) {
							if (i + 1 < btnList.size()) {
								WebElement nextBtn = btnList.get(i + 1);
								safeClick(driver, nextBtn);
								maxNumberOfPagesClicked--;
								if(isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
									isFinalPageReached = true;
								}
								System.out.println("page clicked");

			                    //wait.until(ExpectedConditions.stalenessOf(jobs.getFirst()));
							} else {
								isFinalPageReached = true; // active button is last page
							}
							break;
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
				
				String innerHTMLMissions = wait.until(ExpectedConditions.presenceOfElementLocated(
						By.cssSelector("div[class*='jobmaindesc__text']")))
						  .getDomProperty("innerHTML").replace("\n", "");
				
				String innerHTMLQualifications = wait.until(ExpectedConditions.presenceOfElementLocated(
						By.cssSelector("div[class*='jobrequirements__content']")))
						  .getDomProperty("innerHTML").replace("\n", "");
				
				String innerHTML = innerHTMLMissions + innerHTMLQualifications;
				System.out.println(innerHTML);
				
			
				String apply_link = jobsLinks.get(id);
					
				System.out.println(apply_link);
				System.out.println();
				
				JobsOffers jobOffer = new JobsOffers();
                jobOffer.setTitle(id_jobInfo.get(id).getFirst());
                jobOffer.setCompany("Apside");
                jobOffer.setLocation(id_jobInfo.get(id).get(3));
                jobOffer.setUrl(apply_link);
                jobOffer.setContractType(id_jobInfo.get(id).get(1));
                jobOffer.setWorkMode("N/A");
                jobOffer.setPublishDate(id_jobInfo.get(id).get(2));
                jobOffer.setPost(innerHTML);
                if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
                		id_jobInfo.get(id).getFirst(), 
                		"Apside", 
                		id_jobInfo.get(id).get(3), 
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
//		driver.quit();
		
		
	}
	
	public boolean dateCheckValabilityStatus(String date_to_check) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy"); // match your format
		LocalDate inputDate = null;
		try {
			inputDate = LocalDate.parse(date_to_check, formatter);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Date format not supported: " + date_to_check);
		}

		LocalDate today = LocalDate.now();
		LocalDate twoMonthsLater = inputDate.plusMonths(2);

		if (twoMonthsLater.isAfter(today)) {
			return true;
		} else {
			return false;
		}
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

package com.websolutions.companies.collection.services;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.websolutions.companies.collection.entites.JobsOffers;
import com.websolutions.companies.collection.locations.DetectCities;
import com.websolutions.companies.collection.repositories.JobsOffersRepository;

@Service
public class AkkodisJobCollector {
	
	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
    private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
    private final HashMap<Integer, String> jobsLinks = new HashMap<>();
    private final JobsOffersRepository jobsOffersRepository;
    private EdgeOptions options;
    private WebDriver driver;
    private boolean isFinalPageReached = false;
    private int maxNumberOfPagesClicked = 3;
    private String AkkodisLink = "https://www.akkodis.com/en-us/careers/job-results";
    private final DetectCities detectCities;
	
	public AkkodisJobCollector(JobsOffersRepository jobsOffersRepository, DetectCities detectCities) {
		super();
		this.jobsOffersRepository = jobsOffersRepository;
		this.detectCities = detectCities;
	}

	public void getFulljobs(boolean isFullJobsCollection) throws MalformedURLException {
		int page_index = 1;
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
		
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

		driver.get(AkkodisLink);
		
		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			/* Check if popup cookies is appearing */
			try {
				WebElement popupAcceptBtn = wait
						.until(ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler")));
				safeClick(driver, popupAcceptBtn);
			} catch (TimeoutException e) {
				// Popup didn't appear, continue
			}
			popupAppearedAndClosed = true;
		}
		
		List<WebElement> chatbot_iframe = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("__sense-widget")));
		if(chatbot_iframe.size() > 0) {
			((JavascriptExecutor) driver).executeScript("document.getElementById('__sense-widget').style.display='none';");
			((JavascriptExecutor) driver).executeScript("document.getElementById('__sense-widget-button').style.display='none';");
		}
		
		while (!isFinalPageReached) {
			List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("div.JobSearchResults_filter-container__jj_1I > div:nth-child(3) ul[class*='JobSearchResults_filter'] li"))
				);
			for (int i = 0; i < jobs.size(); i++) {
				try {
			        // Re-locate the job element every iteration
			        jobs = driver.findElements(By.cssSelector(
			            "div.JobSearchResults_filter-container__jj_1I > div:nth-child(3) ul[class*='JobSearchResults_filter'] li"
			        ));
					WebElement job = jobs.get(i);
					String job_title = job.findElement(By.cssSelector("h3")).getText();
					String contract_type = job.findElement(By.cssSelector("a > div:nth-child(3) > div:nth-child(1) span:nth-child(2)")).getText();
					String location = job.findElement(By.cssSelector("a > div:nth-child(3) > div:nth-child(2) span:nth-child(2)")).getText();
					String publish_date = job.findElement(By.cssSelector("a > div:nth-child(3) > div:nth-child(3) span:nth-child(2)")).getText();
					String job_link = "https://www.akkodis.com" + 
							   job.findElement(By.tagName("a"))
							   .getDomAttribute("href");
					
					String city = location.strip();
					String country = "N/A";	
					Optional<String> detectedCountry = detectCities.getCountryForCity(city);
					if(detectedCountry.isPresent()) {
						country = detectedCountry.get();
					}
					
					System.out.println(job_title+"  |  "+location+"  |  "+ publish_date+"  |  " + contract_type);
					publish_date = date_formatter(publish_date);
					if(dateCheckValabilityStatus(publish_date)) {
						List<String> infos = new ArrayList<>();
						infos.add(job_title.strip());
						infos.add(city);
						infos.add(country);
						infos.add(publish_date);
						infos.add(contract_type);
			
						id_jobInfo.put(jobIndex, infos);
						jobsLinks.put(jobIndex, job_link);
						jobIndex++;
					}
				}catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				
					
				
			}
			
			try {
				int maxPagesNumber = 20;
				try {
					maxPagesNumber = Integer.parseInt(
										driver.findElements(
												By.cssSelector("ul[class*='pagination_pagination-list'] li"))
										.getLast()
										.getText());
				}catch (Exception e) {
					
				}
				//System.out.println("maxPagesNumber is : " + maxPagesNumber);
				
				List<WebElement> nextPageBtn = driver.findElements(
						By.cssSelector("span[class*='pagination-right-arrow'] span")
					);
				
				//System.out.println("number of buttons : " + nextPageBtn.size());
				if (nextPageBtn.isEmpty()) {
					isFinalPageReached = true;
				} else {
					String cssClass = nextPageBtn.getFirst().getDomAttribute("class");
					if(!cssClass.contains("pe-none") && page_index<maxPagesNumber) {
						safeClick(driver, nextPageBtn.getFirst());
						page_index++;
						maxNumberOfPagesClicked--;
						if(isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
							isFinalPageReached = true;
						}
						//System.out.println(page_index);
						//wait.until(ExpectedConditions.stalenessOf(jobs.getFirst()));
					}else {
						isFinalPageReached = true;	    
					}
					
				}
	         } catch (Exception e) {
	        	 System.out.println(e.getMessage());
	        	 System.out.println("Error next !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	             isFinalPageReached = true;	        	 
	         }
		}
		
		
		
		for(int id=0; id<jobsLinks.size(); id++) {
			try {
				driver.get(jobsLinks.get(id));
				String innerHTML = "";			
				WebElement innerHTMLElement = wait.until(ExpectedConditions.presenceOfElementLocated(
						By.cssSelector("div[class*='JobDescription_job-description-body']"))
						);
				innerHTML = innerHTMLElement.getDomProperty("innerHTML").replace("\n", "");
//				
//				
				String apply_link = "https://www.akkodis.com" + driver.findElement(
						By.cssSelector("div[class*='JobDescription_mobile-job-details'] a[class*='apply-now-button']"))
						.getDomAttribute("href");
//					
//				
				System.out.println("\n\n"+"  "+id+"  :  "+innerHTML);
				System.out.println(apply_link);
				
				JobsOffers jobOffer = new JobsOffers();
                jobOffer.setTitle(id_jobInfo.get(id).getFirst());
                jobOffer.setCompany("Akkodis");
                jobOffer.setCity(id_jobInfo.get(id).get(1));
                jobOffer.setCountry(id_jobInfo.get(id).get(2));
                jobOffer.setUrl(apply_link);
                jobOffer.setContractType(id_jobInfo.get(id).get(4));
                jobOffer.setWorkMode("N/A");
                jobOffer.setPublishDate(id_jobInfo.get(id).get(3));
                jobOffer.setPost(innerHTML);
                if (!jobsOffersRepository.existsByTitleAndCompanyAndCityAndCountryAndUrl(
                		id_jobInfo.get(id).getFirst(), 
                		"Akkodis", 
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
		closeDriver();
	}
	
	private void closeDriver(){
        driver.quit();
    }
	
	private String date_formatter(String input_date) {
	    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
	    LocalDate date = LocalDate.parse(input_date.trim(), inputFormatter);
	    return date.format(DateTimeFormatter.ISO_LOCAL_DATE); // yyyy-MM-dd
	}
	
	public boolean dateCheckValabilityStatus(String date_to_check) {
		DateTimeFormatter formatter2 = DateTimeFormatter.ISO_LOCAL_DATE;
		LocalDate inputDate = null;
		try {
			inputDate = LocalDate.parse(date_to_check, formatter2);
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
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'center'});", element);
			Thread.sleep(500);
			element.click();
		} catch (ElementClickInterceptedException e) {
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
		} catch (Exception ex) {
			 System.out.println("Error clicking element: " + ex.getMessage());
		}
	}
}

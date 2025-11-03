package com.websolutions.companies.collection.services;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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
import com.websolutions.companies.collection.locations.DetectCities;
import com.websolutions.companies.collection.repositories.JobsOffersRepository;

@Service
public class CapgeminiEngineeringJobCollector {

	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
	private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
	private final HashMap<Integer, String> jobsLinks = new HashMap<>();
	private final JobsOffersRepository jobsOffersRepository;
	private EdgeOptions options;
	private String CapgeminiEngineeringLink = "https://www.capgemini.com/ma-en/job-search/?page=1&size=11&country_code=ma-en";
	private int maxNumberOfPagesClicked = 3;
	boolean isFinalPageReached = false;
	private final DetectCities detectCities;

	public CapgeminiEngineeringJobCollector(JobsOffersRepository jobsOffersRepository, DetectCities detectCities) {
		super();
		this.jobsOffersRepository = jobsOffersRepository;
		this.detectCities = detectCities;
	}

	public void getMoroccanJobs(boolean isFullJobsCollection) throws IOException, InterruptedException {
		int jobIndex = 0;

		options = new EdgeOptions();
		options.addArguments("--no-sandbox");
		options.addArguments("--headless=new");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--lang=en-US");
		options.addArguments("--disable-gpu");
		options.addArguments("--disable-notifications");
		options.addArguments("--window-size=1920,1080");

		WebDriver driver = new RemoteWebDriver(URI.create("http://selenium:4444").toURL(), options);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		driver.get(CapgeminiEngineeringLink);

		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			/* Check if popup cookies is appearing */
			try {
				WebElement popupAcceptBtn = wait
						.until(ExpectedConditions.elementToBeClickable(By.id("truste-consent-button")));
				safeClick(driver, popupAcceptBtn);
			} catch (TimeoutException e) {
				// Popup didn't appear, continue
			}
			popupAppearedAndClosed = true;
		}

		while (!isFinalPageReached) {
			List<WebElement> jobs = wait.until(
					ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#job-list-section ul li a")));

			for (WebElement element : jobs) {
				String job_title = element.findElement(By.cssSelector("div[class*='title']")).getText();
				String location = element.findElement(By.cssSelector("div[class*='location']")).getText();
				String contract_type = element.findElement(By.cssSelector("ul li[class*='contract-type']")).getText();
				String job_link = element.getDomAttribute("href");

				System.out.println(job_title + " | " + job_link + " | " + location + " | " + contract_type);

				String city = "N/A";
				String country = "N/A";
				city = location.strip().replace("\n", ", ");
				Optional<String> detectedCountry = detectCities.getCountryForCity(city);
				if(detectedCountry.isPresent()) {
					country = detectedCountry.get();
				}
				
				List<String> infos = new ArrayList<>();
				infos.add(job_title.strip());
				infos.add(city);
				infos.add(country);
				infos.add(contract_type.strip());
				infos.add("N/A");
				infos.add("N/A");

				id_jobInfo.put(jobIndex, infos);
				jobsLinks.put(jobIndex, job_link);
				jobIndex++;
			}

			try {
				WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(
						"button.Pagination-module__next___sD4Yg.Pagination-module__arrow-button___I3AgN")));

				/*
				 * Here we check if next button is invisible, if yes then we reached the final
				 * page.
				 */
				String disabled = nextButton.getDomAttribute("disabled");
				if (disabled != null) {
					isFinalPageReached = true;
					break;
				}
				safeClick(driver, nextButton);
				maxNumberOfPagesClicked--;
				if (isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
					isFinalPageReached = true;
				}

				wait.until(ExpectedConditions.stalenessOf(jobs.getFirst()));
			} catch (Exception e) {
				isFinalPageReached = true;
			}
		}

		for (int id = 0; id < jobsLinks.size(); id++) {
			driver.get("https://www.capgemini.com" + jobsLinks.get(id));
			String innerHTML = "";
			innerHTML = wait
					.until(ExpectedConditions.presenceOfElementLocated(
							By.cssSelector("#detail-container div[class*='SingleJobDescription']")))
					.getDomProperty("innerHTML");

			String apply_link = driver.findElement(By.cssSelector("#sticky-header a[class*='Header-module__apply']"))
					.getDomAttribute("href");

			System.out.println("\n\n" + "  " + id + "  :  " + innerHTML);
			System.out.println(apply_link);
			
			JobsOffers jobOffer = new JobsOffers();
            jobOffer.setTitle(id_jobInfo.get(id).getFirst());
            jobOffer.setCompany("Capgemini Engineering");
            jobOffer.setCity(id_jobInfo.get(id).get(1));
            jobOffer.setCountry(id_jobInfo.get(id).get(2));
            jobOffer.setUrl(apply_link);
            jobOffer.setContractType(id_jobInfo.get(id).get(3));
            jobOffer.setWorkMode("N/A");
            jobOffer.setPublishDate("N/A");
            jobOffer.setPost(innerHTML);
            if (!jobsOffersRepository.existsByTitleAndCompanyAndCityAndCountryAndUrl(
            		id_jobInfo.get(id).getFirst(), 
            		"Capgemini Engineering", 
            		id_jobInfo.get(id).get(1),
            		id_jobInfo.get(id).get(2),
            		apply_link)){
            	
            	try {
            		jobsOffersRepository.save(jobOffer);
				} catch (DataIntegrityViolationException e) {
					logger.info("Duplicate detected: " + jobOffer.getTitle() + " @ " + jobOffer.getUrl());
				}
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

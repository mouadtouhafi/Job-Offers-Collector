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
public class CageminiJobCollector {
	private String capgeminiLinkPart = "https://www.capgemini.com/careers/join-capgemini/job-search/?size=";
	
	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
    private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
    private final HashMap<Integer, String> jobsLinks = new HashMap<>();
    private final JobsOffersRepository jobsOffersRepository;
    private EdgeOptions options;
    boolean isFinalPageReached = false;

  
	public CageminiJobCollector(JobsOffersRepository jobsOffersRepository) {
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
		
        WebDriver driver = new RemoteWebDriver(
        		URI.create("http://selenium:4444").toURL(),
        	    options
        	);

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		if (isFullJobsCollection) {
			capgeminiLinkPart = capgeminiLinkPart + "1500";
		} else {
			capgeminiLinkPart = capgeminiLinkPart + "20";
		}

		driver.get(capgeminiLinkPart);

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

		List<WebElement> jobs = wait.until(
				ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("section.table.filters-content a")));
		for (WebElement job : jobs) {
			List<WebElement> job_details = job.findElements(By.cssSelector(".table-td"));
			String job_title = job_details.get(0).findElement(By.tagName("div")).getText();
			String country = job_details.get(1).findElement(By.tagName("div")).getText();
			String location = job_details.get(2).findElement(By.tagName("div")).getText();
			String contract_type = job_details.get(5).findElement(By.tagName("div")).getText();
			String job_link = job.getDomAttribute("href");
			System.out.println(
					job_title + "  |  " + country + "  |  " + location + "  |  " + contract_type + "  |  " + job_link);

			List<String> infos = new ArrayList<>();
			infos.add(job_title.strip());
			infos.add(country.strip().replace("\n", ", ") + " " + location.strip().replace("\n", ", "));
			infos.add(contract_type.strip());
			infos.add("N/A");
			infos.add("N/A");

			id_jobInfo.put(jobIndex, infos);
			jobsLinks.put(jobIndex, job_link);
			jobIndex++;
		}

		for (int id = 0; id < jobsLinks.size(); id++) {
			try {
				driver.get(jobsLinks.get(id));
				String innerHTML = "";
				WebElement innerHTMLContainer = wait.until(ExpectedConditions
						.presenceOfElementLocated(By.cssSelector("section.section--job-info div.article-text")));
				innerHTML = innerHTMLContainer.getDomProperty("innerHTML");
				String apply_link = driver
						.findElement(By.cssSelector("section.section--job-info div.job-meta-box a.cta-link"))
						.getDomAttribute("href");

				System.out.println("\n\n" + "  " + id + "  :  " + innerHTML);
				System.out.println(apply_link);
				
				JobsOffers jobOffer = new JobsOffers();
                jobOffer.setTitle(id_jobInfo.get(id).getFirst());
                jobOffer.setCompany("Capgemini");
                jobOffer.setLocation(id_jobInfo.get(id).get(1));
                jobOffer.setUrl(apply_link);
                jobOffer.setContractType(id_jobInfo.get(id).get(2));
                jobOffer.setWorkMode("N/A");
                jobOffer.setPublishDate("N/A");
                jobOffer.setPost(innerHTML);
                if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
                		id_jobInfo.get(id).getFirst(), 
                		"Capgemini", 
                		id_jobInfo.get(id).get(1), 
                		apply_link)){
                	
                	try {
                		jobsOffersRepository.save(jobOffer);
					} catch (DataIntegrityViolationException e) {
						logger.info("Duplicate detected: " + jobOffer.getTitle() + " @ " + jobOffer.getUrl());
					}
                }
			} catch (Exception e) {
				System.out.println(
						"⚠️ Unexpected error at job " + id + " (" + jobsLinks.get(id) + "): " + e.getMessage());
				continue;
			}

		}
		driver.quit();

	}

	public static void safeClick(WebDriver driver, WebElement element) {
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

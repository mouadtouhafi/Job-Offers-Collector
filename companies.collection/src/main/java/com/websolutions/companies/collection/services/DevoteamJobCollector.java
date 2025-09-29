package com.websolutions.companies.collection.services;

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
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.websolutions.companies.collection.entites.JobsOffers;
import com.websolutions.companies.collection.repositories.JobsOffersRepository;

@Service
public class DevoteamJobCollector {
	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
    private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
    private final HashMap<Integer, String> jobsLinks = new HashMap<>();
    private final JobsOffersRepository jobsOffersRepository;
    private WebDriver driver;
    private EdgeOptions options;
    private String DevoteamLink = "https://www.devoteam.com/fr/jobs/";
    private int maxNumberOfPagesClicked = 1;
    boolean isFinalPageReached = false;
	
	public DevoteamJobCollector(JobsOffersRepository jobsOffersRepository) {
		super();
		this.jobsOffersRepository = jobsOffersRepository;
	}
	
	public void getFulljobs(boolean isFullJobsCollection) {
		int jobIndex = 0;
		

		options = new EdgeOptions();

		// Enable headless mode
		// options.addArguments("--headless");

		// Optional: Add other arguments for optimization
		options.addArguments("--lang=en-US");
		options.addArguments("--disable-gpu"); // For Windows systems
		// options.addArguments("--window-size=1200,880"); // Set a specific window size
		options.addArguments("--disable-notifications"); // Disable pop-ups

		driver = new EdgeDriver(options);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		driver.get(DevoteamLink);

		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			try {
				// The accept cookie button is inside a shadow DOM, we Wait for the shadow host

				WebElement acceptBtn = wait
						.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button.button-accept-all")));

				safeClick(driver, acceptBtn);

			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			popupAppearedAndClosed = true;
		}

		while (!isFinalPageReached) {
			try {
				List<WebElement> jobs = wait.until(ExpectedConditions
						.presenceOfAllElementsLocatedBy(By.cssSelector("div.jobs-list div.container")));
				for (WebElement job : jobs) {

					String job_title = "";
					String contract_type = "";
					String job_title_contract = job.findElement(By.cssSelector("div.wrapper a h2.title")).getText();
					int lastComma = job_title_contract.lastIndexOf(",");
					if (lastComma != -1) {
						job_title = job_title_contract.substring(0, lastComma).trim();
						contract_type = job_title_contract.substring(lastComma + 1).trim();
					}

					String job_link = job.findElement(By.cssSelector("div.wrapper a")).getDomAttribute("href");

					System.out.println(job_title + "  |  " + contract_type + "  |  " + job_link);

					List<String> infos = new ArrayList<>();
					infos.add(job_title.strip());
					infos.add(contract_type.strip());

					id_jobInfo.put(jobIndex, infos);
					jobsLinks.put(jobIndex, job_link);
					jobIndex++;

				}

				try {

					WebElement nextPageButton = driver.findElements(By.cssSelector("div.pagination-wrapper a"))
							.getLast();
					String className = nextPageButton.getDomAttribute("class");
					if (className.contains("active")) {
						safeClick(driver, nextPageButton);
						maxNumberOfPagesClicked--;
						if (isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
							isFinalPageReached = true;

						}
					} else {
						isFinalPageReached = true;
					}

				} catch (Exception e) {
					isFinalPageReached = true;
				}

			} catch (Exception e) {
				e.printStackTrace();
				WebElement closeAd = driver.findElement(By.id("interactive-close-button-container"));
				safeClick(driver, closeAd);
				continue;
			}
		}

		for (int id = 0; id < jobsLinks.size(); id++) {
			try {
				driver.get(jobsLinks.get(id));
				
				String location = wait.until(ExpectedConditions.presenceOfElementLocated(
								By.cssSelector("div.wp-block-acf-post-header div.wp-block-group p")))
								.getText().strip();
				List<WebElement> innerHTMLPostElements = driver.findElements(By.cssSelector("div.entry-content.wp-block-post-content div.description"));
				String innerHTML = "";
				for(WebElement element : innerHTMLPostElements) {
					innerHTML = innerHTML + element.getDomProperty("innerHTML").replace("\n", "").replaceAll("<img[^>]*>", "");
				}

				System.out.println(innerHTML);

				String apply_link = driver.findElements(
									By.cssSelector("div.entry-content.wp-block-post-content div.wp-block-buttons a"))
									.getFirst()
									.getDomAttribute("href");

				System.out.println(apply_link);
				System.out.println(location);
				System.out.println();
				
				
				JobsOffers jobOffer = new JobsOffers();
                jobOffer.setTitle(id_jobInfo.get(id).getFirst());
                jobOffer.setCompany("Devoteam");
                jobOffer.setLocation(location);
                jobOffer.setUrl(apply_link);
                jobOffer.setContractType(id_jobInfo.get(id).get(1));
                jobOffer.setWorkMode("N/A");
                jobOffer.setPublishDate("N/A");
                jobOffer.setPost(innerHTML);
                if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
                		id_jobInfo.get(id).getFirst(), 
                		"Devoteam", 
                		location, 
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

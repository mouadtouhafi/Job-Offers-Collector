package com.websolutions.companies.collection.services;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
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
public class HirschmannAutomotiveJobCollector {
	
	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
    private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
    private final HashMap<Integer, String> jobsLinks = new HashMap<>();
    private final JobsOffersRepository jobsOffersRepository;
    private WebDriver driver;
    private EdgeOptions options;
    private String HirschmannLink = "https://career.hirschmann-automotive.com/en/";
	
	public HirschmannAutomotiveJobCollector(JobsOffersRepository jobsOffersRepository) {
		super();
		this.jobsOffersRepository = jobsOffersRepository;
	}
	
	public void getFulljobs(boolean isFullJobsCollection) throws InterruptedException, MalformedURLException {
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
        
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		driver.get(HirschmannLink);

		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			/*
			 * the Usercentrics cookie banner is rendered inside a shadow DOM, and it often
			 * re-renders dynamically after the page loads. This causes Selenium to throw a
			 * StaleElementReferenceException because the reference to the button becomes
			 * invalid once the DOM refreshes. The solution is twofold: first, explicitly
			 * wait for the shadow host (id="usercentrics-root") to be present; second, each
			 * time we need to interact with elements inside it, re-fetch the shadow root
			 * and the target element instead of reusing old references. By combining a
			 * short retry loop with JavascriptExecutor to access the shadow DOM, we ensure
			 * Selenium always interacts with the most up-to-date element, which prevents
			 * the stale element error and allows the “Accept All” button to be clicked
			 * reliably.
			 */
			try {
				JavascriptExecutor js = (JavascriptExecutor) driver;

				WebElement cookieBannerRoot = wait
						.until(ExpectedConditions.presenceOfElementLocated(By.id("usercentrics-root")));

				// Retry logic to handle stale elements
				for (int i = 0; i < 3; i++) {
					try {
						// Get fresh shadowRoot each time
						SearchContext shadowRoot = (SearchContext) js.executeScript("return arguments[0].shadowRoot",
								cookieBannerRoot);

						// Find the button fresh
						WebElement acceptAllButton = shadowRoot
								.findElement(By.cssSelector("button[data-testid='uc-accept-all-button']"));

						// Click it
						acceptAllButton.click();
						System.out.println("Clicked Accept All");
						break; // exit loop if successful
					} catch (StaleElementReferenceException e) {
						System.out.println("Stale element, retrying... " + (i + 1));
						Thread.sleep(500); // short pause before retry
					}
				}

			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			popupAppearedAndClosed = true;
		}

		try {
			List<WebElement> domains = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("section.section-container.page-teaser.type-2 > div > div:nth-child(2) a")));
			System.out.println(domains.size());

			Map<String, String> domain_link_list = new HashMap<>();
			for (WebElement domain : domains) {
				String domain_link = domain.getDomAttribute("href");
				String domain_name = domain.findElement(By.cssSelector("div.page-teaser-link-wrap")).getText();
				domain_link_list.put(domain_name, domain_link);
			}

			Set<String> keys = domain_link_list.keySet();
			for (String key : keys) {
				System.out.println(domain_link_list.get(key));
				driver.get("https://career.hirschmann-automotive.com" + domain_link_list.get(key));

				List<WebElement> jobs = wait.until(ExpectedConditions
						.presenceOfAllElementsLocatedBy(By.cssSelector("div.jobs-list ul.jobs-list li")));

				for (WebElement job : jobs) {

					String job_link = "https://career.hirschmann-automotive.com" + job.findElement(By.tagName("a")).getDomAttribute("href");
					String job_title = job.findElement(By.cssSelector("h3")).getText();
					String location = job.findElement(By.cssSelector("p:nth-child(3)")).getText();

					System.out.println(job_link + " | " + job_title + " | " + location + " | " + key);
					List<String> infos = new ArrayList<>();
					infos.add(job_title.strip());
					infos.add(location);

					id_jobInfo.put(jobIndex, infos);
					jobsLinks.put(jobIndex, job_link);
					jobIndex++;
				}
				System.out.println();

			}

			for (int id = 0; id < jobsLinks.size(); id++) {
				try {
					driver.get(jobsLinks.get(id));

					String innerHTML = "";
					List<WebElement> innerHTMLElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
							By.cssSelector("section.job-detail-container div.left-col > div")));
					for (WebElement innerHTMLElement : innerHTMLElements) {
						innerHTML = innerHTML + innerHTMLElement.getDomProperty("innerHTML").replace("\n", "");
					}

					String apply_link = driver
							.findElement(By.cssSelector("section.job-detail-container div.left-col > a"))
							.getDomAttribute("href");

					System.out.println("\n\n" + "  " + id + "  :  " + innerHTML);
					System.out.println(apply_link);
					System.out.println();
					
					JobsOffers jobOffer = new JobsOffers();
	                jobOffer.setTitle(id_jobInfo.get(id).getFirst());
	                jobOffer.setCompany("Hirschmann Automotive");
	                jobOffer.setLocation(id_jobInfo.get(id).get(1));
	                jobOffer.setUrl(apply_link);
	                jobOffer.setContractType("N/A");
	                jobOffer.setWorkMode("N/A");
	                jobOffer.setPublishDate("N/A");
	                jobOffer.setPost(innerHTML);
	                if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
	                		id_jobInfo.get(id).getFirst(), 
	                		"Hirschmann Automotive", 
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		driver.quit();
	}

	public void safeClick(WebDriver driver, WebElement element) {
		try {
			((JavascriptExecutor) driver)
					.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'center'});", element);
			Thread.sleep(500);
			element.click();
		} catch (ElementClickInterceptedException e) {
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
		} catch (Exception ex) {
			System.out.println("Error clicking element: " + ex.getMessage());
		}
	}
}

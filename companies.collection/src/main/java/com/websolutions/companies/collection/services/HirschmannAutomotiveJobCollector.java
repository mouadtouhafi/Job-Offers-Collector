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
import com.websolutions.companies.collection.modelAI.PredictTitle;
import com.websolutions.companies.collection.repositories.JobsOffersRepository;
import com.websolutions.companies.collection.utils.CountryNormalizer;

@Service
public class HirschmannAutomotiveJobCollector {
	
	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
    private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
    private final HashMap<Integer, String> jobsLinks = new HashMap<>();
    private final JobsOffersRepository jobsOffersRepository;
    private WebDriver driver;
    private EdgeOptions options;
    private String HirschmannLink = "https://career.hirschmann-automotive.com/en/";
    private CountryNormalizer countryNormalizer;
    private PredictTitle predictTitle;
	
	public HirschmannAutomotiveJobCollector(JobsOffersRepository jobsOffersRepository, CountryNormalizer countryNormalizer, PredictTitle predictTitle) {
		super();
		this.jobsOffersRepository = jobsOffersRepository;
		this.countryNormalizer = countryNormalizer;
		this.predictTitle = predictTitle;
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
        options.addArguments("--window-size=1920,1080");
		
        driver = new RemoteWebDriver(
        		URI.create("http://selenium:4444").toURL(),
        	    options
        	);
        
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		driver.get(HirschmannLink);

		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			/*
			 * This part of the code handles closing the cookie consent popup that appears when opening the Hirschmann Automotive website. 
			 * Because the popup is built inside a shadow DOM (a hidden part of the page that Selenium can’t access directly), 
			 * normal element selection doesn’t work. 
			 * To solve this, the code uses a JavaScriptExecutor to repeatedly check if the popup and its “Accept All” button exist 
			 * inside the shadow DOM. 
			 * Once the button is found, it clicks it using JavaScript instead of the regular Selenium click method, 
			 * ensuring the popup closes reliably. 
			 * If the popup doesn’t appear within 15 seconds, the code safely skips it and continues without crashing.
			 */
			try {
			    JavascriptExecutor js = (JavascriptExecutor) driver;

			    WebElement acceptAllButton = new WebDriverWait(driver, Duration.ofSeconds(15))
			            .until(driver1 -> {
			                Object button = js.executeScript("""
			                    const host = document.querySelector('#usercentrics-root');
			                    if (!host || !host.shadowRoot) return null;
			                    return host.shadowRoot.querySelector("button[data-testid='uc-accept-all-button']");
			                """);
			                return (WebElement) button;
			            });

			    if (acceptAllButton != null) {
			        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptAllButton);
			        System.out.println("✅ Accepted cookies via JS click");
			    } else {
			        System.out.println("⚠️ Accept All button not found even after wait");
			    }

			} catch (TimeoutException e) {
			    System.out.println("⚠️ No cookie popup found within timeout, continuing...");
			}

			popupAppearedAndClosed = true;
		}

		try {
			List<WebElement> domains = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("section.section-container.page-teaser.type-2 > div > div:nth-child(2) a")));

			Map<String, String> domain_link_list = new HashMap<>();
			for (WebElement domain : domains) {
				String domain_link = domain.getDomAttribute("href");
				String domain_name = domain.findElement(By.cssSelector("div.page-teaser-link-wrap")).getText();
				domain_link_list.put(domain_name, domain_link);
			}

			Set<String> keys = domain_link_list.keySet();
			for (String key : keys) {
				driver.get("https://career.hirschmann-automotive.com" + domain_link_list.get(key));

				List<WebElement> jobs = wait.until(ExpectedConditions
						.presenceOfAllElementsLocatedBy(By.cssSelector("div.jobs-list ul.jobs-list li")));

				for (WebElement job : jobs) {

					String job_link = "https://career.hirschmann-automotive.com" + job.findElement(By.tagName("a")).getDomAttribute("href");
					String job_title = job.findElement(By.cssSelector("h3")).getText();
					String location = job.findElement(By.cssSelector("p:nth-child(3)")).getText();
					
					String city = "Undefined";
					String country = "Undefined";
					
					String[] splitLocation = location.split("-");
					if(splitLocation.length >= 2) {
						city = splitLocation[0].strip();
						country = splitLocation[1].strip();
						
						String normalizedCountry = countryNormalizer.find(country.toLowerCase());
						if(!normalizedCountry.equals("NOT FOUND")) {
							country = normalizedCountry;
						}
						
					}

					List<String> infos = new ArrayList<>();
					infos.add(job_title.strip());
					infos.add(city);
					infos.add(country);

					id_jobInfo.put(jobIndex, infos);
					jobsLinks.put(jobIndex, job_link);
					jobIndex++;
				}

			}

			for (int id = 0; id < jobsLinks.size(); id++) {
				try {
					driver.get(jobsLinks.get(id));

					String innerHTML = "";
					List<WebElement> innerHTMLElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
							By.cssSelector("section.job-detail-container div.left-col > div")));
					for (WebElement innerHTMLElement : innerHTMLElements) {
						innerHTML = innerHTML + innerHTMLElement.getDomProperty("innerHTML").replace("\n", "").replaceAll("(?i)<p>(\\s|&nbsp;|&#160;|<br\\s*/?>)*</p>","");
					}

					String apply_link = driver
							.findElement(By.cssSelector("section.job-detail-container div.left-col > a"))
							.getDomAttribute("href");

					
					JobsOffers jobOffer = new JobsOffers();
	                jobOffer.setTitle(id_jobInfo.get(id).getFirst());
	                jobOffer.setCompany("Hirschmann Automotive");
	                jobOffer.setCity(id_jobInfo.get(id).get(1));
	                jobOffer.setCountry(id_jobInfo.get(id).get(2));
	                jobOffer.setUrl(apply_link);
	                jobOffer.setContractType("Undefined");
	                jobOffer.setWorkMode("Undefined");
	                jobOffer.setPublishDate("Undefined");
	                jobOffer.setJobField(predictTitle.predictField(id_jobInfo.get(id).getFirst()).replace(" / ", " - "));
	                jobOffer.setPost(innerHTML);
	                if (!jobsOffersRepository.existsByTitleAndCompanyAndCityAndCountryAndUrl(
	                		id_jobInfo.get(id).getFirst(), 
	                		"Hirschmann Automotive", 
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

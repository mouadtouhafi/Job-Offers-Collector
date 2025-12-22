package com.websolutions.companies.collection.services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.websolutions.companies.collection.entites.JobsOffers;
import com.websolutions.companies.collection.modelAI.PredictTitle;
import com.websolutions.companies.collection.repositories.JobsOffersRepository;
import com.websolutions.companies.collection.utils.CountryNormalizer;

@Service
public class StellantisJobCollector {

	private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
	private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
	private final HashMap<Integer, String> jobsLinks = new HashMap<>();
	private final JobsOffersRepository jobsOffersRepository;
	private WebDriver driver;
	private EdgeOptions options;
	private boolean isFinalPageReached = false;
	private int maxNumberOfPagesClicked = 3;
	private String StellantisLink = "https://careers.stellantis.com/job-search-results/";
	private CountryNormalizer countryNormalizer;
	private PredictTitle predictTitle;

	ObjectMapper mapper = new ObjectMapper();

	public StellantisJobCollector(JobsOffersRepository jobsOffersRepository, CountryNormalizer countryNormalizer,
			PredictTitle predictTitle) {
		super();
		this.jobsOffersRepository = jobsOffersRepository;
		this.countryNormalizer = countryNormalizer;
		this.predictTitle = predictTitle;
	}

	public void getFulljobs(boolean isFullJobsCollection) throws IOException {
		int jobIndex = 0;

		options = new EdgeOptions();
		options.addArguments("--no-sandbox");
		options.addArguments("--headless=new");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--lang=en-US");
		options.addArguments("--disable-gpu");
		options.addArguments("--disable-notifications");
		options.addArguments("--window-size=1920,1080");

		driver = new RemoteWebDriver(URI.create("http://selenium:4444").toURL(), options);

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		driver.get(StellantisLink);

		JsonNode root = mapper.readTree(new File("data-for-database/stellantis_sites.json"));

		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			/* Check if popup cookies is appearing */
			try {
				driver.switchTo().frame("iFrame1");
				WebElement closeBtn = driver.findElement(By.id("acceptAllBtn"));
				safeClick(driver, closeBtn);

				driver.switchTo().defaultContent();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			popupAppearedAndClosed = true;
		}

		while (!isFinalPageReached) {
			List<WebElement> jobs = wait.until(ExpectedConditions
					.presenceOfAllElementsLocatedBy(By.cssSelector("#widget-jobsearch-results-list ol > li")));
			for (WebElement job : jobs) {
				String job_title = job.findElement(By.cssSelector("li.title div.jobTitle a")).getText();
				String location = job
						.findElement(By.cssSelector("li.city_state_or_locationtype div[class*='location']")).getText();
				String job_link = "https://careers.stellantis.com"
						+ job.findElement(By.cssSelector("li.title div.jobTitle a")).getDomAttribute("href");

				location = location.strip();
				String city = "Undefined";
				String country = "Undefined";
				if (root.has(location)) {
					JsonNode plant = root.get(location);
					city = plant.get("city").asText();
					country = plant.get("country").asText();

					String normalizedCountry = countryNormalizer.find(country.toLowerCase());
					if (!normalizedCountry.equals("NOT FOUND")) {
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
			try {
				List<WebElement> btnList = driver.findElements(By.cssSelector("#widget-jobsearch-results-pages ul li"));

				if (btnList.isEmpty()) {
					isFinalPageReached = true;
				} else {
					WebElement nextArrow = btnList.get(btnList.size() - 2);
					if (nextArrow.getText().equals(">")) {
						safeClick(driver, nextArrow);
						maxNumberOfPagesClicked--;
						if (isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
							isFinalPageReached = true;
						}
						wait.until(ExpectedConditions.stalenessOf(jobs.getFirst()));
					} else {
						isFinalPageReached = true;
					}
				}
			} catch (Exception e) {
				isFinalPageReached = true;
			}
		}

		for (int id = 0; id < jobsLinks.size(); id++) {
			try {
				driver.get(jobsLinks.get(id));
				WebElement jobInfoTabContainer = wait
						.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.tabcontainer")));

				String innerHTML = "";
				List<WebElement> sections = jobInfoTabContainer.findElements(By.tagName("section"));
				for (WebElement section : sections) {
					String tab_name = section.findElement(By.cssSelector(":scope > div:nth-child(1)"))
							.getDomProperty("innerHTML");
					if (!tab_name.contains("Benefits")) {
						innerHTML = innerHTML + "<h1>" + tab_name + "</h1>";
						String tab_innerHTML = section.findElement(By.cssSelector(":scope > div:nth-child(2)"))
								.getDomProperty("innerHTML");
						innerHTML = innerHTML + "<p>" + tab_innerHTML.replace("\n", "") + "</p>";
					}
				}

				WebElement publish_date_element = driver.findElement(By.id("gtm-jobdetail-date"));
				String publish_date = publish_date_element.getText();
				publish_date = date_formatter(publish_date);

				/* WebElement job_domain_element = driver.findElement(By.id("jobdetail-id2"));*/
				/* String job_domain = job_domain_element.getText(); */

				String apply_link = driver.findElement(By.cssSelector("#gtm-jobdetail-apply a"))
						.getDomAttribute("href");


				if (dateCheckValabilityStatus(publish_date)) {
					JobsOffers jobOffer = new JobsOffers();
					jobOffer.setTitle(id_jobInfo.get(id).getFirst());
					jobOffer.setCompany("Stellantis");
					jobOffer.setCity(id_jobInfo.get(id).get(1));
					jobOffer.setCountry(id_jobInfo.get(id).get(2));
					jobOffer.setUrl(apply_link);
					jobOffer.setContractType("Undefined");
					jobOffer.setWorkMode("Undefined");
					jobOffer.setPublishDate(publish_date);
					jobOffer.setJobField(
							predictTitle.predictField(id_jobInfo.get(id).getFirst()).replace(" / ", " - "));
					jobOffer.setPost(innerHTML);
					if (!jobsOffersRepository.existsByTitleAndCompanyAndCityAndCountryAndUrl(
							id_jobInfo.get(id).getFirst(), "Stellantis", id_jobInfo.get(id).get(1),
							id_jobInfo.get(id).get(2), apply_link)) {

						try {
							jobsOffersRepository.save(jobOffer);
						} catch (DataIntegrityViolationException e) {
							logger.info("Duplicate detected: " + jobOffer.getTitle() + " @ " + jobOffer.getUrl());
						}
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

	private String date_formatter(String input_date) {
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
		LocalDate date = LocalDate.parse(input_date.trim(), inputFormatter);
		return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	public boolean dateCheckValabilityStatus(String date_to_check) {
		DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
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
}

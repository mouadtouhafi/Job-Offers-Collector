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
import com.websolutions.companies.collection.locations.DetectCities;
import com.websolutions.companies.collection.modelAI.PredictTitle;
import com.websolutions.companies.collection.repositories.JobsOffersRepository;
import com.websolutions.companies.collection.utils.CountryNormalizer;

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
	private final DetectCities detectCities;
	private CountryNormalizer countryNormalizer;
	private PredictTitle predictTitle;

	public ScalianJobCollector(JobsOffersRepository jobsOffersRepository, DetectCities detectCities,
			CountryNormalizer countryNormalizer, PredictTitle predictTitle) {
		super();
		this.jobsOffersRepository = jobsOffersRepository;
		this.detectCities = detectCities;
		this.countryNormalizer = countryNormalizer;
		this.predictTitle = predictTitle;
	}

	public void getFulljobs(boolean isFullJobsCollection) throws IOException, InterruptedException {
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

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		driver.get(ScalianLink);

		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			try {
				WebElement shadowHost = wait
						.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.needsclick")));

				SearchContext shadowRoot = shadowHost.getShadowRoot();
				WebElement acceptBtn = wait.until(d -> shadowRoot.findElement(By.id("axeptio_btn_acceptAll")));
				safeClick(driver, acceptBtn);

			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			popupAppearedAndClosed = true;
		}

		List<WebElement> numberOfJobsPerPage = wait
				.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(
						"div.row.dragElement.widget.container-widget.job-results__main-content-container div.attrax-pagination__resultsperpage")))
				.getFirst().findElements(By.tagName("a"));
		WebElement maxNumberOfPagesButton = numberOfJobsPerPage.getLast();
		safeClick(driver, maxNumberOfPagesButton);

		while (!isFinalPageReached) {
			List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(
					"div.row.dragElement div.attrax-list-widget__lists div.attrax-list-widget__list div.attrax-vacancy-tile")));
			for (WebElement job : jobs) {
				String job_title = job.findElement(By.cssSelector("a.attrax-vacancy-tile__title")).getText();
				
				/* Contract type extraction section */
				String contract_type = "Undefined";
				List<WebElement> contract_elements = job.findElements(
				    By.cssSelector("div.attrax-vacancy-tile__option-contract-type p.attrax-vacancy-tile__item-value")
				);
				if (!contract_elements.isEmpty()) {contract_type = contract_elements.getFirst().getText().strip();}

				/* Location extraction section */
				String location = "Undefined";
				List<WebElement> location_elements = job.findElements(
					By.cssSelector("div.attrax-vacancy-tile__location-freetext p.attrax-vacancy-tile__item-value")
				);
				if (!location_elements.isEmpty()) {location = location_elements.getFirst().getText().strip();}
				
				String job_link = "https://careers.scalian.com"
						+ job.findElement(By.cssSelector("a.attrax-vacancy-tile__title")).getDomAttribute("href");
				
				/*
				String domain = job
						.findElement(By.cssSelector(
								"div.attrax-vacancy-tile__option-function p.attrax-vacancy-tile__item-value"))
						.getDomProperty("innerHTML").replace("\n", "").strip();
						*/

				String city = location.strip();
				String country = "Undefined";
				Optional<String> detectedCountry = detectCities.getCountryForCity(city);
				if (detectedCountry.isPresent()) {
					country = detectedCountry.get();

					String normalizedCountry = countryNormalizer.find(country.toLowerCase());
					if (!normalizedCountry.equals("NOT FOUND")) {
						country = normalizedCountry;
					}
				}

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

				WebElement nextPageButton = driver.findElement(By.cssSelector(
						"div.row.dragElement.widget.container-widget.job-results__main-content-container div.attrax-pagination__pagination ul li.attrax-pagination__next"));
				safeClick(driver, nextPageButton);
				maxNumberOfPagesClicked--;
				if (isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
					isFinalPageReached = true;

				}
			} catch (Exception e) {
				isFinalPageReached = true;
			}
		}

		for (int id = 0; id < jobsLinks.size(); id++) {
			try {
				driver.get(jobsLinks.get(id));
				String innerHTML = wait
						.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.description-widget")))
						.getDomProperty("innerHTML").replace("\n", "").replaceAll("(?i)<p>(\\s|&nbsp;|&#160;|<br\\s*/?>)*</p>","");

				String apply_link = "https://careers.scalian.com"
						+ driver.findElements(By.cssSelector("div.job-details__details-container a.jobApplyBtn"))
								.getFirst().getDomAttribute("href");

				JobsOffers jobOffer = new JobsOffers();
				jobOffer.setTitle(id_jobInfo.get(id).getFirst());
				jobOffer.setCompany("Scalian");
				jobOffer.setCity(id_jobInfo.get(id).get(1));
				jobOffer.setCountry(id_jobInfo.get(id).get(2));
				jobOffer.setUrl(apply_link);
				jobOffer.setContractType(id_jobInfo.get(id).get(3));
				jobOffer.setWorkMode("Undefined");
				jobOffer.setPublishDate("Undefined");
				jobOffer.setJobField(predictTitle.predictField(id_jobInfo.get(id).getFirst()).replace(" / ", " - "));
				jobOffer.setPost(innerHTML);
				if (!jobsOffersRepository.existsByTitleAndCompanyAndCityAndCountryAndUrl(id_jobInfo.get(id).getFirst(),
						"Scalian", id_jobInfo.get(id).get(1), id_jobInfo.get(id).get(2), apply_link)) {

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

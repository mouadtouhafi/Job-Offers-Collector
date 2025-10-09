package com.websolutions.companies.collection.services;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.websolutions.companies.collection.entites.JobsOffers;
import com.websolutions.companies.collection.repositories.JobsOffersRepository;



@Service
public class AltenJobCollector {

	private final JobsOffersRepository jobsOffersRepository;
	private static final Logger logger = Logger.getLogger(AltenJobCollector.class.getName());
	private String AltenLink = "https://www.alten.com/careers/job-offers/";
	private WebDriver driver;
	private EdgeOptions options;
	private final Map<String, String> ALTEN_COUNTRIES_LINK = new HashMap<String, String>();

	WebDriverWait wait;
	WebDriverWait fishingPopupWait;

	public AltenJobCollector(JobsOffersRepository jobsOffersRepository) {
		this.jobsOffersRepository = jobsOffersRepository;
	}

	public void closeDriver() {
		driver.quit();
	}
	
	public void setupDriver() throws MalformedURLException {
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
        
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        fishingPopupWait = new WebDriverWait(driver, Duration.ofSeconds(4));
        System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
	}


	/*
	 * This section collect the list of countries listed in Alten company. - After
	 * diving to the URL, we need first to close the popup by pressing the Accept
	 * button. - We are using the method safeClick() to scroll and make sure the
	 * button appears in the browser window, otherwise an exception will be raised.
	 * - The WebElement 'countriesInput' is the input element which contains the
	 * list of countries. Before getting the list, we need first to click the option
	 * input so the 'countriesDivList' element appears in the DOM. - After
	 * collecting the list of countries, we add them to ALTEN_COUNTRIES.
	 */
	public void getCountries() throws MalformedURLException {
		try {
			driver.get(AltenLink);
			WebElement cookieAcceptBtn = wait
					.until(ExpectedConditions.visibilityOfElementLocated(By.id("tarteaucitronPersonalize2")));
			safeClick(driver, cookieAcceptBtn);

			WebElement countriesInput = wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.cssSelector("div.col.is-style-column-text.wp-block-bootstrap-column "
							+ "div.selectize-input.items.full.has-options.has-items")));
			safeClick(driver, countriesInput);

			WebElement countriesDivList = wait.until(ExpectedConditions.visibilityOfElementLocated(
					By.cssSelector("div.selectize-dropdown.single.form-control div.selectize-dropdown-content")));
			List<WebElement> listCountries = countriesDivList.findElements(By.cssSelector("div.option"));
			listCountries.forEach(element -> {
				String _country = element.getText().strip();
				String _link = element.getDomAttribute("data-value");
				if (!_country.contains("COUNTRY")) {
					ALTEN_COUNTRIES_LINK.put(_country, _link);
				}
			});
			System.out.println(ALTEN_COUNTRIES_LINK);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public void getForeignJobs_1(boolean isFullJobsCollection) {
		Set<String> foreign_countries = ALTEN_COUNTRIES_LINK.keySet();
		 String[] countries_set1 = { "UNITED KINGDOM", "SWEDEN", "PORTUGAL",
				 "FINLAND", "SPAIN", "NETHERLANDS", "GERMANY", "SWITZERLAND", "FRANCE",
				 "BELGIUM", "ITALY" };
		for (String country : foreign_countries) {
			System.out.println("Scrapping "+country+" jobs ............................");
			if (Arrays.asList(countries_set1).contains(country)) {
				System.out.println(country);
				int maxNumberOfPagesClicked = 2;
				try {
					int jobIndex = 0;
					boolean isFinalPageReached = false;
					HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
					HashMap<Integer, String> jobsLinks = new HashMap<>();
					driver.get(ALTEN_COUNTRIES_LINK.get(country));
					while (!isFinalPageReached) {
						boolean popupAppearedAndClosed = false;
						if (!popupAppearedAndClosed) {
							/* Check if popup cookies is appearing */
							try {
								WebElement popupAcceptBtn = wait.until(
										ExpectedConditions.elementToBeClickable(By.id("tarteaucitronPersonalize2")));
								safeClick(driver, popupAcceptBtn);
							} catch (TimeoutException e) {
								// Popup didn't appear, continue
							}
							popupAppearedAndClosed = true;
						}

						boolean fishingPopupClosed = false;
						if (!fishingPopupClosed) {
							try {
								List<WebElement> fishingPopupCloseBtn = fishingPopupWait
										.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(
												"div.modal-dialog.modal-dialog-centered.modal-lg div div.modal-header button")));
								if (!fishingPopupCloseBtn.isEmpty()) {
									safeClick(driver, fishingPopupCloseBtn.getFirst());
								}
								fishingPopupClosed = true;
							} catch (Exception e) {
								// TODO: handle exception
							}
						}

						List<WebElement> jobs = wait
								.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(
										"div.wp-block-jobboard-loop div.is-style-card-default.wp-block-webfactory-card")));

						for (WebElement job : jobs) {
							try {
								WebElement job_title_element = job.findElement(By.tagName("a"));
								String job_title = job_title_element.getText();
								String job_link = job_title_element.getDomAttribute("href");

								WebElement location_element = job.findElement(
										By.cssSelector("div.col-md-3.order-2.px-1.px-md-2.py-2.d-flex.card-location "
												+ "span.location-list.ms-2.d-flex.flex-column"));
								String location = location_element.getText();
								WebElement publish_date_element = job.findElement(
										By.cssSelector("div.col-md-2.order-3.px-1.px-md-2.py-2.card-date span.mx-2"));

								String publish_date = publish_date_element.getText();
								if (dateCheckValabilityStatus(publish_date)) {
									List<String> infos = new ArrayList<>();
									infos.add(job_title.strip());
									infos.add(location.strip().replace("\n", ", "));
									infos.add("N/A");
									infos.add("N/A");
									infos.add(publish_date.strip());
									System.out.println(job_title + "  |  " + job_link + "  |  " + infos);

									id_jobInfo.put(jobIndex, infos);
									jobsLinks.put(jobIndex, job_link);
									jobIndex++;
								}

								/*
								 * Here we check if next button is invisible, if yes then we reached the final
								 * page.
								 */

							} catch (Exception e) {
								logger.log(Level.WARNING,
										"[" + AltenJobCollector.class.getName() + "]" + e.getMessage().split("\n")[0]);
								continue;
							}
						}

						try {
							List<WebElement> btnList = driver.findElements(
									By.cssSelector("div.col-lg-8.wp-block-bootstrap-column div nav ul li"));

							if (btnList.isEmpty()) {
								isFinalPageReached = true;
							} else {
								for (int i = 0; i < btnList.size(); i++) {
									WebElement btn = btnList.get(i);
									String cssClass = btn.getDomAttribute("class");

									if (cssClass.contains("active")) {
										if (i + 1 < btnList.size()) {
											WebElement nextBtn = btnList.get(i + 1);
											safeClick(driver, nextBtn);
											maxNumberOfPagesClicked--;
											if(isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
												isFinalPageReached = true;
											}
											
										} else {
											isFinalPageReached = true;
										}
										break;
									}
								}
							}

						} catch (Exception e) {
							logger.info("No more pages available " + e.getMessage());
							isFinalPageReached = true;
						}
					}

					try {
						for (int id = 0; id < id_jobInfo.size(); id++) {
							driver.get(jobsLinks.get(id));

							Wait<WebDriver> fluentWait = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(10))
									.pollingEvery(Duration.ofMillis(500)).ignoring(NoSuchElementException.class);

							List<WebElement> divPost = fluentWait.until(driver -> {
								List<WebElement> elements = driver
										.findElements(By.cssSelector(".mb-5.wp-block-jobboard-offer-meta"));
								if (!elements.isEmpty()) {
									return elements;
								}
								elements = driver.findElements(By.cssSelector(".my-0.wp-block-jobboard-offer-meta"));
								if (!elements.isEmpty()) {
									return elements.subList(1, 3);
								}
								return null;
							});

							String innerHTML = "";
							for (WebElement element : divPost) {
								innerHTML = innerHTML + element.getDomProperty("innerHTML") + "\n";
							}
							innerHTML = innerHTML.stripTrailing().replace("\n", "");
							innerHTML = innerHTML.replaceAll("\\s{2,}", " ");

							// retrieving direct link to apply for the offer
							WebElement applyBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By
									.cssSelector("div.mx-md-2.is-style-button-blue.wp-block-jobboard-offer-action a")));
							String applyLink = applyBtn.getDomAttribute("href");
							//System.out.println("the innerHTML is : " + innerHTML);
							System.out.println("the apply link is : " + applyLink);

							JobsOffers jobOffer = new JobsOffers();
							jobOffer.setTitle(id_jobInfo.get(id).getFirst());
							jobOffer.setCompany("Alten");
							jobOffer.setLocation(id_jobInfo.get(id).get(1));
							jobOffer.setUrl(applyLink);
							jobOffer.setContractType(id_jobInfo.get(id).get(2));
							jobOffer.setWorkMode(id_jobInfo.get(id).get(3));
							jobOffer.setPublishDate(id_jobInfo.get(id).get(4));
							jobOffer.setPost(innerHTML);

							if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
									id_jobInfo.get(id).getFirst(), 
									"Alten", 
									id_jobInfo.get(id).get(1), 
									applyLink)) {
								try {
									jobsOffersRepository.save(jobOffer);
								} catch (DataIntegrityViolationException e) {
									logger.info(
											"Duplicate detected: " + jobOffer.getTitle() + " @ " + jobOffer.getUrl());
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				} catch (Exception e) {
					String message = (e.getMessage() != null) ? e.getMessage().split("\n")[0] : "No message";
					logger.log(Level.WARNING,
							"[" + AltenJobCollector.class.getName() + "]" + " Failed at " + country + ": " + message,
							e);
					continue;
				}

			}

		}
	}

	public void getForeignJobs_2() {
		Set<String> foreign_countries = ALTEN_COUNTRIES_LINK.keySet();
		String[] countries_set2 = { "AUSTRIA" };
		for (String country : foreign_countries) {
			if (Arrays.asList(countries_set2).contains(country)) {
				System.out.println(country);
				try {
					int jobIndex = 0;
					HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
					HashMap<Integer, String> jobsLinks = new HashMap<>();
					driver.get(ALTEN_COUNTRIES_LINK.get(country));
					List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
							By.cssSelector("div.openings-body.js-openings ul.opening-jobs li.opening-job")));

					for (WebElement job : jobs) {
						try {
							WebElement linkElement = job.findElement(By.cssSelector("a.link--block.details"));
							String job_title = linkElement.findElement(By.cssSelector("h4.details-title")).getText();
							String job_link = job.findElement(By.cssSelector("a.link--block.details"))
									.getDomAttribute("href");
							String location = linkElement.findElement(By.cssSelector("ul.job-list li:nth-of-type(1)"))
									.getText().trim();
							String contract_type = linkElement
									.findElement(By.cssSelector("ul.job-list li:nth-of-type(2)")).getText().trim();

							System.out.println(job_title + " | " + location + " | " + contract_type + " | " + job_link);

							List<String> infos = new ArrayList<>();
							infos.add(job_title.strip());
							infos.add(location.strip().replace("\n", ", "));
							infos.add(contract_type.strip());
							infos.add("N/A");
							infos.add("N/A");

							id_jobInfo.put(jobIndex, infos);
							jobsLinks.put(jobIndex, job_link);
							jobIndex++;

						} catch (Exception e) {
							e.printStackTrace();
							logger.log(Level.WARNING,
									"[" + AltenJobCollector.class.getName() + "]" + e.getMessage().split("\n")[0]);
							continue;
						}
					}

					for (int id = 0; id < jobsLinks.size(); id++) {
						driver.get(jobsLinks.get(id));
						String jobPostInnerHTML = "";
						WebElement jobDescription = wait
								.until(ExpectedConditions.presenceOfElementLocated(By.id("st-jobDescription")));
						jobPostInnerHTML = jobPostInnerHTML + jobDescription.getDomProperty("innerHTML");

						WebElement jobQualification = wait
								.until(ExpectedConditions.presenceOfElementLocated(By.id("st-qualifications")));
						jobPostInnerHTML = jobPostInnerHTML + jobQualification.getDomProperty("innerHTML");
						System.out.println(jobPostInnerHTML);

						WebElement applyBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(
								"main.jobad-main.job section.job-apply.print-hidden a.button.button--primary.button--huge.js-oneclick.job-button")));
						String applyLink = applyBtn.getDomAttribute("href");
						System.out.println(applyLink);

						JobsOffers jobOffer = new JobsOffers();
						jobOffer.setTitle(id_jobInfo.get(id).getFirst());
						jobOffer.setCompany("Alten");
						jobOffer.setLocation(id_jobInfo.get(id).get(1));
						jobOffer.setUrl(applyLink);
						jobOffer.setContractType(id_jobInfo.get(id).get(2));
						jobOffer.setWorkMode(id_jobInfo.get(id).get(3));
						jobOffer.setPublishDate(id_jobInfo.get(id).get(4));
						jobOffer.setPost(jobPostInnerHTML);

						if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
								id_jobInfo.get(id).getFirst(), 
								"Alten", 
								id_jobInfo.get(id).get(1), 
								applyLink)) {
							try {
								jobsOffersRepository.save(jobOffer);
							} catch (DataIntegrityViolationException e) {
								logger.info("Duplicate detected: " + jobOffer.getTitle() + " @ " + jobOffer.getUrl());
							}
						}
					}

				} catch (Exception e) {
					String message = (e.getMessage() != null) ? e.getMessage().split("\n")[0] : "No message";
					logger.log(Level.WARNING,
							"[" + AltenJobCollector.class.getName() + "]" + " Failed at " + country + ": " + message,e);
					continue;
				}

			}
		}
	}

	public void getForeignJobs_3() {
		System.out.println("Scrapping INDIA jobs ............................");
		Set<String> foreign_countries = ALTEN_COUNTRIES_LINK.keySet();
		String[] countries_set2 = { "INDIA" };
		for (String country : foreign_countries) {
			if (Arrays.asList(countries_set2).contains(country)) {
				System.out.println(country);
				try {
					driver.get(ALTEN_COUNTRIES_LINK.get(country));
					WebElement careersLink = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(
							"#join-us div.row.row-cols-1.row-cols-md-2.justify-content-center.wp-block-bootstrap-row a.card-inner.text-decoration-none")));
					safeClick(driver, careersLink);
					boolean popupAppearedAndClosed = false;
					if (!popupAppearedAndClosed) {
						/* Check if popup cookies is appearing */
						try {
							WebElement popupAcceptBtn = wait
									.until(ExpectedConditions.elementToBeClickable(By.id("tarteaucitronPersonalize2")));
							safeClick(driver, popupAcceptBtn);
						} catch (TimeoutException e) {
							// Popup didn't appear, continue
						}
						popupAppearedAndClosed = true;
					}

					boolean fishingPopupClosed = false;
					if (!fishingPopupClosed) {
						try {
							List<WebElement> fishingPopupCloseBtn = fishingPopupWait
									.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(
											"div.modal-dialog.modal-dialog-centered.modal-lg div div.modal-header button")));
							if (!fishingPopupCloseBtn.isEmpty()) {
								safeClick(driver, fishingPopupCloseBtn.getFirst());
							}
							fishingPopupClosed = true;
						} catch (Exception e) {
							// TODO: handle exception
						}
					}

					List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
							By.cssSelector("#Opportunities div.accordion-item.wp-block-bootstrap-accordion")));

					for (WebElement job : jobs) {
						try {

							safeClick(driver, job);
							WebElement title = job.findElement(By.cssSelector("header span"));

							WebElement post = wait.until(ExpectedConditions.presenceOfNestedElementLocatedBy(job,
									By.cssSelector("div.accordion-collapse.collapse.show")));
							String job_title = title.getText();
							String jobPostInnerHTML = post.getDomProperty("innerHTML")
														  .replace("\n", "")
														  .replaceAll("\\s{2,}", " ");
							//System.out.println(jobPostInnerHTML);

							JobsOffers jobOffer = new JobsOffers();
							jobOffer.setTitle(job_title);
							jobOffer.setCompany("Alten");
							jobOffer.setLocation("India");
							jobOffer.setUrl("N/A");
							jobOffer.setContractType("N/A");
							jobOffer.setWorkMode("N/A");
							jobOffer.setPublishDate("N/A");
							jobOffer.setPost(jobPostInnerHTML);

							if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
									job_title, 
									"Alten",
									"India", 
									"N/A")) {
								try {
									jobsOffersRepository.save(jobOffer);
								} catch (DataIntegrityViolationException e) {
									logger.info(
											"Duplicate detected: " + jobOffer.getTitle() + " @ " + jobOffer.getUrl());
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
							logger.log(Level.WARNING,
									"[" + AltenJobCollector.class.getName() + "]" + e.getMessage().split("\n")[0]);
							continue;
						}
					}

				} catch (Exception e) {
					String message = (e.getMessage() != null) ? e.getMessage().split("\n")[0] : "No message";
					logger.log(Level.WARNING,
							"[" + AltenJobCollector.class.getName() + "]" + " Failed at " + country + ": " + message,e);
					continue;
				}

			}
		}
	}

	public void getMoroccanJobs(boolean isFullJobsCollection) {
		System.out.println("Scrapping Moroccan jobs ............................");
		Set<String> list_countries = ALTEN_COUNTRIES_LINK.keySet();
		String[] countries_set = { "MOROCCO" };
		
		for (String country : list_countries) {
			if (Arrays.asList(countries_set).contains(country)) {
				int maxNumberOfPagesClicked = 3;
				System.out.println(country);
				int jobIndex = 0;
				boolean isFinalPageReached = false;
				HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
				HashMap<Integer, String> jobsLinks = new HashMap<>();
				try {
					driver.get("https://www.alten.ma/rejoignez-nous/#rejoignez-nous");
					boolean popupAppearedAndClosed = false;
					if (!popupAppearedAndClosed) {
						/* Check if popup cookies is appearing */
						try {
							WebElement popupAcceptBtn = wait
									.until(ExpectedConditions.elementToBeClickable(By.id("tarteaucitronPersonalize2")));
							safeClick(driver, popupAcceptBtn);
						} catch (TimeoutException e) {
							// Popup didn't appear, continue
						}
						popupAppearedAndClosed = true;
					}

					while (!isFinalPageReached) {
						List<WebElement> jobs = driver.findElements(By.cssSelector(
								"#jobboard-jobboard-0 .col-lg-9.wp-block-bootstrap-column .wp-block-jobboard-loop .card-inner"));
						for (WebElement element : jobs) {
							String job_title = element.findElement(By.cssSelector(".card-title")).getText();
							String job_link = element.findElement(By.cssSelector(".card-title")).getDomAttribute("href");
							String location = element.findElement(By.cssSelector(".card-location .location-list")).getText();
							String publish_date = element.findElement(By.cssSelector(".card-date .mx-2")).getText().strip();

							System.out.println(job_title + " | " + job_link + " | " + location + " | " + publish_date);

							if (dateCheckValabilityStatus(publish_date)) {
								List<String> infos = new ArrayList<>();
								infos.add(job_title.strip());
								infos.add(location.strip().replace("\n", ", "));
								infos.add("N/A");
								infos.add("N/A");
								infos.add(publish_date.strip());

								id_jobInfo.put(jobIndex, infos);
								jobsLinks.put(jobIndex, job_link);
								jobIndex++;
							}
						}
						List<WebElement> btnList = driver.findElements(By.cssSelector(
								"div.row.gx-2.wp-block-bootstrap-row div.wp-block-jobboard-pagination nav ul.pagination li"));

						System.out.println("\n" + btnList.size());
						if (btnList.isEmpty()) {
							isFinalPageReached = true;
						} else {
							for (int i = 0; i < btnList.size(); i++) {
								WebElement btn = btnList.get(i);
								String cssClass = btn.getDomAttribute("class");
								if (cssClass.contains("active")) {
									if (i + 1 < btnList.size()) {
										WebElement nextBtn = btnList.get(i + 1);
										safeClick(driver, nextBtn);
										maxNumberOfPagesClicked--;
										if(isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
											isFinalPageReached = true;
										}
										System.out.println("page clicked");
										wait.until(ExpectedConditions.stalenessOf(jobs.getFirst()));
									} else {
										isFinalPageReached = true; 
									}
									break;
								}
							}
						}						
					}

					for (int id = 0; id < jobsLinks.size(); id++) {
						driver.get(jobsLinks.get(id));
						String innerHTML = "";
						List<WebElement> jobPostMissions = wait.until(ExpectedConditions
								.presenceOfAllElementsLocatedBy(By.cssSelector(".mb-5.wp-block-jobboard-offer-meta")));
						for (WebElement element : jobPostMissions) {
							innerHTML = innerHTML + element.getDomProperty("innerHTML");
						}
						
						String apply_link = driver.findElement(
							By.cssSelector(".mx-md-2.is-style-button-blue.wp-block-jobboard-offer-action a")
						).getDomAttribute("href");
						
						JobsOffers jobOffer = new JobsOffers();
		                jobOffer.setTitle(id_jobInfo.get(id).getFirst());
		                jobOffer.setCompany("Alten");
		                jobOffer.setLocation(id_jobInfo.get(id).get(1));
		                jobOffer.setUrl(apply_link);
		                jobOffer.setContractType(id_jobInfo.get(id).get(2));
		                jobOffer.setWorkMode(id_jobInfo.get(id).get(3));
		                jobOffer.setPublishDate(id_jobInfo.get(id).get(4));
		                jobOffer.setPost(innerHTML);

						if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
								id_jobInfo.get(id).getFirst(), 
								"Alten",
								"India", 
								"N/A")) {
							try {
								jobsOffersRepository.save(jobOffer);
							} catch (DataIntegrityViolationException e) {
								logger.info(
										"Duplicate detected: " + jobOffer.getTitle() + " @ " + jobOffer.getUrl());
							}
						}
						//System.out.println("\n\n" + "  " + id + "  :  " + innerHTML);
					}

				} catch (Exception e) {
					String message = (e.getMessage() != null) ? e.getMessage().split("\n")[0] : "No message";
					logger.log(Level.WARNING,
							"[" + AltenJobCollector.class.getName() + "]" + " Failed at " + country + ": " + message,e);
					continue;
				}

			}
		}
	}

	/*
	 * Sometimes the jobs section are not in the center of the page, this will
	 * result a problem because the element is in the html page but not clickable,
	 * to solve this we need to perform a scroll.
	 */
	public void safeClick(WebDriver driver, WebElement element) {
		try {
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
			Thread.sleep(500);
			element.click();
		} catch (ElementClickInterceptedException e) {
			/*
			 * If there is a problem in the scroll, we can't perform a click with Selenium
			 * we click the element using javascript
			 */
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
		} catch (Exception ex) {
		}
	}

	public boolean dateCheckValabilityStatus(String date_to_check) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		DateTimeFormatter formatter2 = DateTimeFormatter.ISO_LOCAL_DATE;

		LocalDate inputDate = null;
		try {
			inputDate = LocalDate.parse(date_to_check, formatter);
		} catch (DateTimeParseException e) {
			try {
				inputDate = LocalDate.parse(date_to_check, formatter2);
			} catch (DateTimeParseException ex) {
				throw new IllegalArgumentException("Date format not supported: " + date_to_check);
			}
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

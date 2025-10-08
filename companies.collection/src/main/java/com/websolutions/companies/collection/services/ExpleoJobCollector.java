package com.websolutions.companies.collection.services;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
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
public class ExpleoJobCollector {

    private static final Logger logger = Logger.getLogger(ExpleoJobCollector.class.getName());
    private final HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
    private final HashMap<Integer, String> jobsLinks = new HashMap<>();
    private final JobsOffersRepository jobsOffersRepository;
    private WebDriver driver;
    private boolean isFinalPageReached = false;
    private int maxNumberOfPagesClicked = 1;
	private EdgeOptions options;

    private String ExpleoLink = "https://expleo-jobs-fr-fr.icims.com/jobs/search?ss=1";

    public ExpleoJobCollector(JobsOffersRepository jobsOffersRepository) {
        this.jobsOffersRepository = jobsOffersRepository;
    }

    public void getFullFranceJobs(boolean isFullJobsCollection) {
    	options = new EdgeOptions();
		options.addArguments("--no-sandbox");
        options.addArguments("--headless=new");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--lang=en-US");
		options.addArguments("--disable-gpu");
		options.addArguments("--disable-notifications");
		driver = new EdgeDriver();
		try {
			driver.get(ExpleoLink);

			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

			int globalIndex = 0;
			int pageCount = 1;
			boolean popupAppearedAndClosed = false;

			if (!popupAppearedAndClosed) {
				/* Check if popup cookies is appearing */
				try {
					WebElement popupAcceptBtn = wait
							.until(ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler")));
					safeClick(driver, popupAcceptBtn);
				} catch (TimeoutException e) {
					/* Popup didn't appear, continue */
				}
				popupAppearedAndClosed = true;
			}

			while (!isFinalPageReached) {
				logger.info("Scraping page " + pageCount++);

				/*
				 * Here we list all the iframes and we switch to the one with the name "icims_content_iframe".
				 */
				List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
				for (WebElement iframe : iframes) {
					String name_attribute = iframe.getDomAttribute("name");
					if ("icims_content_iframe".equals(name_attribute)) {
						driver.switchTo().frame(iframe);
						break;
					}
				}

				WebElement jobTable;
				jobTable = wait.until(ExpectedConditions
						.visibilityOfElementLocated(By.cssSelector("div.container-fluid.iCIMS_JobsTable")));

				List<WebElement> rows = jobTable.findElements(By.className("row"));
				for (WebElement row : rows) {
					WebElement link = row.findElement(By.cssSelector("div.col-xs-12.title a.iCIMS_Anchor"));
					WebElement job = row.findElement(By.cssSelector("div.col-xs-12.title a.iCIMS_Anchor h3"));

					String jobLink = link.getDomAttribute("href");
					String jobTitle = job.getText();

					/* Here we extract the date of the job publication */
					WebElement date_element = row
							.findElement(By.cssSelector("div.col-xs-6.header.right > span:nth-of-type(2)"));
					String publishDate = date_element.getText().replaceAll("\\(.*\\)", "").replaceAll("\\n", "");

					/*
					 * Each job bloc contains a DIV which has infos such "Location", "work mode",
					 * "contract type" We get that block which we named as "InfosDivs", after this,
					 * we iterate over its DIVs. Each DIV has two child tags <dt> and <dd>.
					 */
					List<WebElement> InfosDivs = row
							.findElements(By.cssSelector("div.col-xs-12.additionalFields dl.iCIMS_JobHeaderGroup div"));
					String location = "";
					String contractType = "";
					String workMode = "";
					for (WebElement element : InfosDivs) {
						WebElement dt_element = element.findElement(By.tagName("dt"));
						WebElement dd_element = element.findElement(By.tagName("dd"));

						String name = dt_element.getText();
						String value = dd_element.getText();

						switch (name.toLowerCase().stripLeading().stripTrailing()) {
						case "job locations":
							location = value;
							break;
						case "type d’emploi":
							contractType = value;
							break;
						case "lieu de travail":
							workMode = value;
							break;
						default:
							break;
						}
					}

					List<String> infos = new ArrayList<>();
					infos.add(jobTitle);
					infos.add(location);
					infos.add(contractType);
					infos.add(workMode);
					infos.add(publishDate);

					id_jobInfo.put(globalIndex, infos);
					jobsLinks.put(globalIndex, jobLink);
					globalIndex++;
					
					System.out.println(jobTitle + " | " + location+ " | " +contractType+ " | " +workMode+ " | " +publishDate);
				}

				/*
				 * Here in this section, we will try to find and click the nextPage button.
				 */

				try {
					WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(
							By.cssSelector("div.iCIMS_Paging.text-center span.halflings.halflings-menu-right")));

					/*
					 * Here we check if next button is invisible, if yes then we reached the final
					 * page.
					 */
					String classAttr = nextButton.getDomAttribute("class");
					if ((classAttr != null && classAttr.contains("invisible"))
							|| (classAttr != null && classAttr.contains("disabled"))) {
						logger.info("Reached last page");
						isFinalPageReached = true;
						break;
					}
					WebElement firstRow = rows.getFirst();
					safeClick(driver, nextButton);
					maxNumberOfPagesClicked--;
					if (isFullJobsCollection == false && maxNumberOfPagesClicked == 0) {
						isFinalPageReached = true;
					}

					wait.until(ExpectedConditions.stalenessOf(firstRow));
				} catch (Exception e) {
					logger.info("No more pages available");
					isFinalPageReached = true;
				}
			}

			for (int i = 0; i < jobsLinks.size(); i++) {
				driver.get(jobsLinks.get(i));
				try {
					wait = new WebDriverWait(driver, Duration.ofSeconds(15));
					WebElement iframe = driver.findElement(By.id("icims_content_iframe"));
					driver.switchTo().frame(iframe);

					try {
						
						
						// Get all headers
						List<WebElement> headers = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
								By.cssSelector("h2.iCIMS_InfoMsg.iCIMS_InfoField_Job")));

						// Get all content divs
						List<WebElement> contents = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
								By.cssSelector("div.iCIMS_InfoMsg.iCIMS_InfoMsg_Job")));
						
						System.out.println("size : "+headers.size());
						System.out.println("size : "+contents.size());
						
						String innerHTML = "";
						if (headers.size() >= 3 && contents.size() >= 3) {
							if(!contents.getLast().getText().replace(" ", "").isEmpty()) {
								innerHTML =
								        headers.get(1).getDomProperty("outerHTML") +
								        contents.get(1).getDomProperty("outerHTML") +
								        headers.get(2).getDomProperty("outerHTML") +
								        contents.get(2).getDomProperty("outerHTML");
							}else {
								innerHTML = 
										headers.getFirst().getDomProperty("outerHTML") + 
										contents.getFirst().getDomProperty("outerHTML");
							}
						    
						    System.out.println("last content : "+contents.getLast().getText());
						}else if(headers.size() == 1 && contents.size() == 1) {
							innerHTML = 
									headers.getFirst().getDomProperty("outerHTML") + 
									contents.getFirst().getDomProperty("outerHTML");
						}
						innerHTML = innerHTML.replace("\n", "");
						
						WebElement applyButton = driver.findElements(By.cssSelector("a.iCIMS_ApplyOnlineButton")).getFirst();
						String applyLink = applyButton.getDomAttribute("href");
						System.out.println(applyLink);

                        JobsOffers jobOffer = new JobsOffers();
                        jobOffer.setTitle(id_jobInfo.get(i).getFirst());
                        jobOffer.setCompany("Expleo Group");
                        jobOffer.setLocation(id_jobInfo.get(i).get(1));
                        jobOffer.setUrl(applyLink);
                        jobOffer.setContractType(id_jobInfo.get(i).get(2));
                        jobOffer.setWorkMode(id_jobInfo.get(i).get(3));
                        jobOffer.setPublishDate(id_jobInfo.get(i).get(4));
                        jobOffer.setPost(innerHTML);
                        if (!jobsOffersRepository.existsByTitleAndCompanyAndLocationAndUrl(
                        		id_jobInfo.get(i).getFirst(), 
                        		"Expleo Group", 
                        		id_jobInfo.get(i).get(1), 
                        		jobsLinks.get(i))){
                        	
                        	try {
                        		jobsOffersRepository.save(jobOffer);
							} catch (DataIntegrityViolationException e) {
								logger.info("Duplicate detected: " + jobOffer.getTitle() + " @ " + jobOffer.getUrl());
							}
                        }

						System.out.println(innerHTML);
						System.out.println("=========================================================");
						System.out.println();

					} catch (Exception e) {
						logger.log(Level.WARNING, "Error processing job row " + i, e);
					}
				} catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
				}
			}

		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());

		} finally {
			closeDriver();
		}
    }


    /*
     * Sometimes the jobs section are not in the center of the page,
     * this will result a problem because the element is in the html page
     * but not clickable, to solve this we need to perform a scroll.
     * */
     private void safeClick(WebDriver driver, WebElement element) {
         try {
             ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
             Thread.sleep(500);
             element.click();
         } catch (ElementClickInterceptedException e) {
             System.out.println("Click intercepted, using JavaScript click instead.");
             /* If there is a problem in the scroll, we can't perform a click with Selenium
             *  we click the element using javascript */
             ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
         } catch (Exception ex) {
             System.out.println("Error clicking element: " + ex.getMessage());
         }
     }
     
     
     private void closeDriver(){
         driver.quit();
     }
}

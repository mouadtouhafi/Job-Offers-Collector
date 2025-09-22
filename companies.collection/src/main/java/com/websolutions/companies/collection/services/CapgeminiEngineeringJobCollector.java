package com.websolutions.companies.collection.services;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CapgeminiEngineeringJobCollector {
	public void getMoroccanJobs() {
		int jobIndex = 0;
		boolean isFinalPageReached = false;
		HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
		HashMap<Integer, String> jobsLinks = new HashMap<>();
		
		WebDriver driver = new EdgeDriver();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

		driver.get("https://www.capgemini.com/ma-en/job-search/?page=1&size=11&country_code=ma-en");
		
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
		
		while(!isFinalPageReached) {
			List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("#job-list-section ul li a"))
				);
			
			for(WebElement element : jobs) {
				String job_title = element.findElement(By.cssSelector("div[class*='title']")).getText();
				String location = element.findElement(By.cssSelector("div[class*='location']")).getText();
				String contract_type = element.findElement(By.cssSelector("ul li[class*='contract-type']")).getText();
				String job_link = element.getDomAttribute("href");
				
				System.out.println(job_title + " | " + job_link +" | "+location+" | "+contract_type);
				
				List<String> infos = new ArrayList<>();
				infos.add(job_title.strip());
				infos.add(location.strip().replace("\n", ", "));
				infos.add(contract_type.strip());
				infos.add("N/A");
				infos.add("N/A");

				id_jobInfo.put(jobIndex, infos);
				jobsLinks.put(jobIndex, job_link);
				jobIndex++;
			}
			
			 try {
	             WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(
	                     By.cssSelector("button.Pagination-module__next___sD4Yg.Pagination-module__arrow-button___I3AgN")
	             ));

	             /*  Here we check if next button is invisible, if yes then we reached the final page.  */
	             String disabled = nextButton.getDomAttribute("disabled");
	             if (disabled != null) {
	                 isFinalPageReached = true;
	                 break;
	             }
	             safeClick(driver, nextButton);

	             wait.until(ExpectedConditions.stalenessOf(jobs.getFirst()));
	         } catch (Exception e) {
	             isFinalPageReached = true;
	         }
		}
		
		for(int id=0; id<jobsLinks.size(); id++) {
			driver.get("https://www.capgemini.com"+jobsLinks.get(id));
			String innerHTML = "";
			innerHTML = wait.until(ExpectedConditions.presenceOfElementLocated(
					By.cssSelector("#detail-container div[class*='SingleJobDescription']")
				)).getDomProperty("innerHTML");
			
			String apply_link = driver.findElement(By.cssSelector("#sticky-header a[class*='Header-module__apply']")).getDomAttribute("href");
			
			System.out.println("\n\n"+"  "+id+"  :  "+innerHTML);
			System.out.println(apply_link);
		}
		
	
		
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

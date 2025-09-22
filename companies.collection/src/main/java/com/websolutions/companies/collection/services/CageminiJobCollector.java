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

public class CageminiJobCollector {
	public void getFulljobs() {
		int jobIndex = 0;
		boolean isFinalPageReached = false;
		HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
		HashMap<Integer, String> jobsLinks = new HashMap<>();
		
		WebDriver driver = new EdgeDriver();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

		driver.get("https://www.capgemini.com/careers/join-capgemini/job-search/?size=1500");
		
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
		
		List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
				By.cssSelector("section.table.filters-content a"))
			);
		for(WebElement job : jobs) {
			List<WebElement> job_details = job.findElements(By.cssSelector(".table-td"));
			String job_title = job_details.get(0).findElement(By.tagName("div")).getText();
			String country = job_details.get(1).findElement(By.tagName("div")).getText();
			String location = job_details.get(2).findElement(By.tagName("div")).getText();
			String contract_type = job_details.get(5).findElement(By.tagName("div")).getText();	
			String job_link = job.getAttribute("href");
			System.out.println(job_title+"  |  "+country+"  |  "+location+"  |  "+contract_type+"  |  "+job_link);
			
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
		
		for(int id=0; id<jobsLinks.size(); id++) {
			try {
				driver.get(jobsLinks.get(id));
				String innerHTML = "";
				WebElement innerHTMLContainer = wait.until(ExpectedConditions.presenceOfElementLocated(
						By.cssSelector("section.section--job-info div.article-text")
					));
				innerHTML = innerHTMLContainer.getDomProperty("innerHTML");
				String apply_link = driver.findElement(
						By.cssSelector("section.section--job-info div.job-meta-box a.cta-link"))
						.getDomAttribute("href");
				
				System.out.println("\n\n"+"  "+id+"  :  "+innerHTML);
				System.out.println(apply_link);
			} catch (Exception e) {
				System.out.println("⚠️ Unexpected error at job " + id + " (" + jobsLinks.get(id) + "): " + e.getMessage());
		        continue;
			}
			
		}
		
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

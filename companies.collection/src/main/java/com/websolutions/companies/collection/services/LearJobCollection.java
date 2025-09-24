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
import org.springframework.stereotype.Service;

@Service
public class LearJobCollection {
	public void getFulljobs() {
		int jobIndex = 0;
		boolean isFinalPageReached = false;
		HashMap<Integer, List<String>> id_jobInfo = new HashMap<>();
		HashMap<Integer, String> jobsLinks = new HashMap<>();
		
		WebDriver driver = new EdgeDriver();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

		driver.get("https://jobs.lear.com/search/");
		
		boolean popupAppearedAndClosed = false;
		if (!popupAppearedAndClosed) {
			/* Check if popup cookies is appearing */
			try {
				WebElement popupAcceptBtn = wait
						.until(ExpectedConditions.elementToBeClickable(By.id("cookie-accept")));
				safeClick(driver, popupAcceptBtn);
			} catch (TimeoutException e) {
				// Popup didn't appear, continue
			}
			popupAppearedAndClosed = true;
		}
		
		while (!isFinalPageReached) {
			List<WebElement> jobs = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("#searchresults tr.data-row"))
				);
			for(WebElement job : jobs) {
				String job_title = job.findElement(By.cssSelector("td.colTitle span.jobTitle")).getText();
				String job_domain = job.findElement(By.cssSelector("td.colDepartment span.jobDepartment")).getText();
				String job_link = "https://jobs.lear.com" + 
						   job.findElement(By.cssSelector("td.colTitle span.jobTitle a"))
						   .getDomAttribute("href");
				
				System.out.println(job_title+"  |  "+job_domain+"  |  "+job_link);
				
				List<String> infos = new ArrayList<>();
				infos.add(job_title.strip() + " - " + job_domain.strip());
	
				id_jobInfo.put(jobIndex, infos);
				jobsLinks.put(jobIndex, job_link);
				jobIndex++;
					
				
			}
			
			try {
				List<WebElement> btnList = driver.findElements(
						By.cssSelector("div.pagination-well ul.pagination li")
					);
				
				System.out.println("number of buttons : " + btnList.size());
				if (btnList.isEmpty()) {
					isFinalPageReached = true;
				} else {
					btnList.remove(btnList.size()-1);
					btnList.remove(0);
					for (int i = 0; i < btnList.size(); i++) {
						String cssClass = btnList.get(i).getAttribute("class");
						System.out.println("cssClass : "+cssClass);
						
						if (cssClass.contains("active")) {
							if (i + 1 < btnList.size()) {
								WebElement nextBtn = btnList.get(i + 1).findElement(By.tagName("a"));
								safeClick(driver, nextBtn);
								System.out.println("page clicked");

			                    wait.until(ExpectedConditions.stalenessOf(jobs.getFirst()));
							} else {
								isFinalPageReached = true;
							}
							break;
						}
					}
				}
	         } catch (Exception e) {
	             isFinalPageReached = true;
	         }
		}
		
		
		
		for(int id=0; id<jobsLinks.size(); id++) {
			try {
				driver.get(jobsLinks.get(id));
				String innerHTML = "";
				List<WebElement> extraInfosContainer = driver.findElements(By.cssSelector("div.content div.job div.joblayouttoken"));
				
				WebElement innerHTMLElement = extraInfosContainer.get(6).findElement(By.cssSelector("div.row"));
				innerHTML = innerHTMLElement.getDomProperty("innerHTML");
				
				String job_domain = extraInfosContainer.get(3).findElement(By.cssSelector("div.row span:nth-child(2)")).getText();
				
				String apply_link = "https://jobs.avl.com" + driver.findElement(
						By.cssSelector("div.applylink a.btn-primary"))
						.getDomAttribute("href");
					
				
				System.out.println("\n\n"+"  "+id+"  :  "+innerHTML);
				System.out.println(apply_link);
				System.out.println(job_domain);
			} catch (Exception e) {
				System.out.println("⚠️ Unexpected error at job " + id + " (" + jobsLinks.get(id) + "): " + e.getMessage());
		        continue;
			}
			
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

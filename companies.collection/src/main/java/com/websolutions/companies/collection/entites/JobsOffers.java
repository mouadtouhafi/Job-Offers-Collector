package com.websolutions.companies.collection.entites;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


/*
 * @Entity marks the class as a database entity so Spring Data JPA can persist it, 
 * @Table lets us customize how it maps to the database, such as giving the table a specific name. 
 * In our case, the table is called jobs_offers, and the uniqueConstraints ensure that no two rows can have 
 * the same combination of title, company, location, and url. 
 * This way, every time we collect and insert jobs, the database itself enforces uniqueness and prevents 
 * duplicate job offers, acting as an extra safety net on top of our Java checks.
 * */
@Entity
@Table(name = "jobs_offers", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"title", "company", "location", "url"})
})
public class JobsOffers {
	
	/*
	 * @GeneratedValue(strategy = GenerationType.IDENTITY) is used with the @Id field of an entity to tell the database 
	 * to auto-generate the primary key value. 
	 * With the IDENTITY strategy, PostgreSQL (or any SQL database) will automatically assign an incrementing number 
	 * to the ID column each time you insert a new row. 
	 * For example, if our case, we will not need to set the id ourself when we save a new job offer, 
	 * the database will give it 1, then 2, then 3, and so on, ensuring each row has a unique primary key.
	 * */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String title;
    private String company;
    private String city;
    private String country;
    private String url;
    private String contractType;
    private String workMode;
    private String publishDate;
    
    /*
     * @Column(columnDefinition = "TEXT") tells Hibernate to store this field as a TEXT column
	 * in the database. TEXT can hold very large strings, making it ideal for long job descriptions.
	 * It’s similar to @Lob but simpler, as the data is stored directly in the table.
     * */
    @Column(columnDefinition = "TEXT")
    private String post;

    public JobsOffers() {
    	
    }
    
	public JobsOffers(Long id, String title, String company, String city, String country, String url, String contractType,
			String workMode, String publishDate, String post) {
		super();
		this.id = id;
		this.title = title;
		this.company = company;
		this.city = city;
		this.country = country;
		this.url = url;
		this.contractType = contractType;
		this.workMode = workMode;
		this.publishDate = publishDate;
		this.post = post;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}
	
	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getContractType() {
		return contractType;
	}

	public void setContractType(String contractType) {
		this.contractType = contractType;
	}

	public String getWorkMode() {
		return workMode;
	}

	public void setWorkMode(String workMode) {
		this.workMode = workMode;
	}

	public String getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(String publishDate) {
		this.publishDate = publishDate;
	}

	public String getPost() {
		return post;
	}

	public void setPost(String post) {
		this.post = post;
	}
}

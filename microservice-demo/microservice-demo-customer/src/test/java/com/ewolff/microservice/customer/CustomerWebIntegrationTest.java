package com.ewolff.microservice.customer;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import com.ewolff.microservice.customer.domain.Customer;
import com.ewolff.microservice.customer.repository.CustomerRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CustomerApp.class)
@IntegrationTest
@WebAppConfiguration
@ActiveProfiles("test")
public class CustomerWebIntegrationTest {

	@Autowired
	private CustomerRepository customerRepository;

	@Value("${server.port}")
	private int serverPort;

	private RestTemplate restTemplate;

	private <T> T getForMediaType(Class<T> value, MediaType mediaType,
			String url) {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(mediaType));

		HttpEntity<String> entity = new HttpEntity<String>("parameters",
				headers);

		ResponseEntity<T> resultEntity = restTemplate.exchange(url,
				HttpMethod.GET, entity, value);

		return resultEntity.getBody();
	}

	@Test
	public void IsCustomerReturnedAsHTML() {

		Customer customerWolff = customerRepository.findByName("Wolff").get(0);

		String body = getForMediaType(String.class, MediaType.TEXT_HTML,
				customerURL() + customerWolff.getId() + ".html");

		assertThat(body, containsString("Wolff"));
		assertThat(body, containsString("<div"));
	}

	@Before
	public void setUp() {
		restTemplate = new RestTemplate();
	}

	@Test
	public void IsCustomerReturnedAsJSON() {

		Customer customerWolff = customerRepository.findByName("Wolff").get(0);

		String url = customerURL() + customerWolff.getId();
		Customer body = getForMediaType(Customer.class,
				MediaType.APPLICATION_JSON, url);

		assertThat(body, equalTo(customerWolff));
	}

	@Test
	public void IsCustomerListReturned() {

		List<Customer> customers = customerRepository.findAll();
		assertTrue(customers.stream().noneMatch(
				c -> (c.getName().equals("Hoeller1"))));
		ResponseEntity<String> resultEntity = restTemplate.getForEntity(
				customerURL() + "/list.html", String.class);
		assertTrue(resultEntity.getStatusCode().is2xxSuccessful());
		String customerList = resultEntity.getBody();
		assertFalse(customerList.contains("Hoeller1"));
		customerRepository.save(new Customer("Juergen", "Hoeller1",
				"springjuergen@twitter.com", "Schlossallee", "Linz"));

		customerList = restTemplate.getForObject(customerURL() + "/list.html",
				String.class);
		assertTrue(customerList.contains("Hoeller1"));

	}

	private String customerURL() {
		return "http://localhost:" + serverPort + "/customer/";
	}

	@Test
	public void IsCustomerFormDisplayed() {
		ResponseEntity<String> resultEntity = restTemplate.getForEntity(
				customerURL() + "/add.html", String.class);
		assertTrue(resultEntity.getStatusCode().is2xxSuccessful());
		assertTrue(resultEntity.getBody().contains("<form"));
	}

	@Test
	@Transactional
	public void IsSubmittedCustomerSaved() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("firstname", "Juergen");
		map.add("name", "Hoeller");
		map.add("street", "Schlossallee");
		map.add("city", "Linz");
		map.add("email", "springjuergen@twitter.com");

		URI uri = restTemplate.postForLocation(customerURL() + "add.html", map,
				String.class);
		UriTemplate uriTemplate = new UriTemplate(customerURL() + "{id}.html");
		Map<String, String> pathVariables = uriTemplate.match(uri.toString());
		Customer customer = customerRepository.findOne(Long
				.parseLong(pathVariables.get("id")));
		assertEquals(customer.getFirstname(), "Juergen");
		assertEquals(customer.getName(), "Hoeller");
		assertEquals(customer.getStreet(), "Schlossallee");
		assertEquals(customer.getCity(), "Linz");
		assertEquals(customer.getEmail(), "springjuergen@twitter.com");
	}

}
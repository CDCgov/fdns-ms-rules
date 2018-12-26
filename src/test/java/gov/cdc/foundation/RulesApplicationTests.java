package gov.cdc.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { 
		"logging.fluentd.host=fluentd", 
		"logging.fluentd.port=24224", 
		"proxy.hostname=",
		"security.oauth2.resource.user-info-uri=",
		"security.oauth2.protected=",
		"security.oauth2.client.client-id=",
		"security.oauth2.client.client-secret=",
		"ssl.verifying.disable=false" })
@AutoConfigureMockMvc
public class RulesApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;
	@Autowired
	private MockMvc mvc;
	private JacksonTester<JsonNode> json;
	private String baseUrlPath = "/api/1.0/";
	private String profile;

	@Before
	public void setup() {
		ObjectMapper objectMapper = new ObjectMapper();
		JacksonTester.initFields(this, objectMapper);

		// Define the object URL
		System.setProperty("OBJECT_URL", "http://fdns-ms-stubbing:3002/object");
	}

	@Test
	public void indexPage() {
		ResponseEntity<String> response = this.restTemplate.getForEntity("/", String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody(), CoreMatchers.containsString("FDNS Business Rules Microservice"));
	}

	@Test
	public void indexAPI() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(baseUrlPath, String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody(), CoreMatchers.containsString("version"));
	}

	@Test
	public void validateUsingProfile() throws Exception {
		// First create and update rules
		createAndUpdateRules();
		
		// Check a message
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				baseUrlPath + "validate/{profile}", 
				HttpMethod.POST, 
				getEntity(getResourceAsString("junit/object.json"), MediaType.APPLICATION_JSON), 
				JsonNode.class,
				profile);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).extractingJsonPathBooleanValue("@.valid").isEqualTo(true);
		assertThat(body).extractingJsonPathNumberValue("@.errors").isEqualTo(0);
		
		// Check a message with explain mode
		response = restTemplate.exchange(
				baseUrlPath + "validate/{profile}?explain=true", 
				HttpMethod.POST, 
				getEntity(getResourceAsString("junit/object.json"), MediaType.APPLICATION_JSON),
				JsonNode.class,
				profile);
		body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).extractingJsonPathBooleanValue("@.valid").isEqualTo(true);
		assertThat(body).extractingJsonPathNumberValue("@.errors").isEqualTo(0);
		assertThat(body).hasJsonPathValue("@.details");
	}

	@Test
	public void validateWithoutProfile() throws Exception {
		MockMultipartFile json = new MockMultipartFile("json", "object.json", "application/json", getResourceAsString("junit/object.json").getBytes());
		MockMultipartFile rules = new MockMultipartFile("rules", "rules.json", "application/json", getResourceAsString("junit/rules.json").getBytes());
		
		// Check a message
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.fileUpload(baseUrlPath + "/validate");
		String body = mvc.perform(builder.file(json).file(rules)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		JSONObject response  = new JSONObject(body);
		assertThat(response.getBoolean("valid")).isEqualTo(true);
		assertThat(response.getInt("errors")).isEqualTo(0);
		
		// Check a message with explain mode
		builder = MockMvcRequestBuilders.fileUpload(baseUrlPath + "/validate?explain=true");
		body = mvc.perform(builder.file(json).file(rules)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		response  = new JSONObject(body);
		assertThat(response.getBoolean("valid")).isEqualTo(true);
		assertThat(response.getInt("errors")).isEqualTo(0);
		assertThat(response.getJSONArray("details")).isNotNull();
	}
	
	@Test
	public void createAndUpdateRules() throws Exception {
		int nbOfCalls = 2;
		profile = UUID.randomUUID().toString();
		
		for (int i = 0; i < nbOfCalls; i++) {
			ResponseEntity<JsonNode> response = restTemplate.exchange(
					baseUrlPath + "profile/{profile}", 
					HttpMethod.POST, 
					getEntity(getResourceAsString("junit/rules.json"), MediaType.APPLICATION_JSON),
					JsonNode.class,
					profile);
			JsonContent<JsonNode> body = this.json.write(response.getBody());
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(body).hasJsonPathValue("@.success");
			assertThat(body).extractingJsonPathBooleanValue("@.success").isEqualTo(true);
			assertThat(body).hasJsonPathValue("@.profile");
			assertThat(body).extractingJsonPathStringValue("@.profile").isEqualTo(profile);
		}
	}
	
	@Test
	public void getProfile() throws Exception {
		// Be sure that we create the profile
		createAndUpdateRules();
		
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				baseUrlPath + "profile/{profile}", 
				HttpMethod.GET, 
				null,
				JsonNode.class,
				profile);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathValue("@._id");
	}

	private InputStream getResource(String path) throws IOException {
		return getClass().getClassLoader().getResourceAsStream(path);
	}

	private String getResourceAsString(String path) throws IOException {
		return IOUtils.toString(getResource(path), Charset.defaultCharset());
	}

	private HttpEntity<String> getEntity(String content, MediaType mediaType) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		HttpEntity<String> entity = new HttpEntity<String>(content, headers);
		return entity;
	}
}

package org.lakehouse.api.rest.client.test;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.runner.RunWith;
import org.lakehouse.api.rest.client.service.ClientApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.configs.ProjectDTO;

@RunWith(SpringRunner.class)
@TestInstance(Lifecycle.PER_CLASS)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
@RestClientTest(ClientApi.class)
//@Import(TestConf.class)
@ContextConfiguration(classes = {TestConf.class, ClientApi.class})
@ActiveProfiles("test")
public class ClientApiTest {
	 
	@Autowired RestClient restClient;

	@Autowired ClientApi client = new ClientApi(restClient);

	@Autowired MockRestServiceServer server;

	@Autowired private ObjectMapper objectMapper;
	  
	 
	public ProjectDTO loadProjectDTO() throws IOException {
		return objectMapper.readValue(new File("../lakehouse-api/demo/projects/demo.json"), ProjectDTO.class);
	}

	@BeforeAll
	public void setUp() {

		try {
			ProjectDTO projectDTO = loadProjectDTO();
			this.server.expect(requestTo(String.format("%s/%s", Endpoint.PROJECTS, projectDTO.getName())))
					.andRespond(withSuccess(objectMapper.writeValueAsString(projectDTO), MediaType.APPLICATION_JSON));
			System.out.println("Project is loaded");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@AfterAll
	public void afterAll() {
		
	}
	@Test
	public void MakesCorrectCallProjectDTO() throws Exception {
		ProjectDTO expectProjectDTO = loadProjectDTO();
		ProjectDTO projectDTO = this.client.getProjectDTO(expectProjectDTO.getName());
		assert (expectProjectDTO.equals(projectDTO));
	}
}

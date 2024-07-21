package lakehouse.api.controller;


import lakehouse.api.entities.ScenarioTemplate;
import lakehouse.api.repository.ScenarioTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
public class RepositoriesTest {



    @Test
    void shouldTestRepos (
            ScenarioTemplateRepository scenarioTemplateRepository
    ) throws Exception {
        ScenarioTemplate scenarioTemplate = new ScenarioTemplate();
        scenarioTemplate.setKey("testscenario");
        scenarioTemplate.setComment("comment");

    }
}

package lakehouse.api.configurationtest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class RestManipulator {
	@Autowired
	private MockMvc mockMvc;

	public String writeAndReadDTOTest(String keyName, String jsonString, String urlTemplate, String urlTemplateName)
			throws Exception {

		this.mockMvc.perform(post(urlTemplate).content(jsonString).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		this.mockMvc.perform(get(urlTemplate)).andDo(print()).andExpect(status().isOk());

		MvcResult mvcResult = this.mockMvc.perform(get(urlTemplateName, keyName)).andExpect(status().isOk())
				.andReturn();

		return mvcResult.getResponse().getContentAsString();
	}

	public void deleteDTO(String keyName, String urlTemplateName) throws Exception {
		this.mockMvc.perform(delete(urlTemplateName, keyName)).andExpect(status().isAccepted());
	}
}

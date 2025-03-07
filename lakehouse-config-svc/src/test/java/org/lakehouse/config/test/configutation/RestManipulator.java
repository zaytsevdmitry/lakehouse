package org.lakehouse.config.test.configutation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class RestManipulator {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private MockMvc mockMvc;

	public String writeAndReadDTOTest(String keyName, String jsonString, String urlTemplate, String urlTemplateName)
			throws Exception {
		logger.info("writeAndReadDTOTest keyName={}",keyName);
		logger.info("Mock urlTemplate={}",urlTemplate);
		this.mockMvc
				.perform(post(urlTemplate)
						.content(jsonString)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		logger.info("Mock urlTemplate={} status",urlTemplate);
		this.mockMvc.perform(get(urlTemplate)).andDo(print()).andExpect(status().isOk());

		logger.info("Mock urlTemplate={} take back",urlTemplate);
		MvcResult mvcResult = this.mockMvc.perform(get(urlTemplateName, keyName)).andExpect(status().isOk())
				.andReturn();

		logger.info("Mock urlTemplate={} return",urlTemplate);
		return mvcResult.getResponse().getContentAsString();
	}

	public String writeAndReadTextTestByKey(String keyName,String fileExt, String text, String urlTemplate, String urlTemplateName)
			throws Exception {
		String k = keyName.concat(".").concat(fileExt);
		this.mockMvc
				.perform(post(urlTemplate, k)
						.content(text)
						.contentType(MediaType.TEXT_PLAIN)
						.accept(MediaType.TEXT_PLAIN))
				.andExpect(status().isCreated());

		this.mockMvc.perform(get(urlTemplate,k)).andDo(print()).andExpect(status().isOk());

		MvcResult mvcResult = this.mockMvc.perform(get(urlTemplateName, k)).andExpect(status().isOk())
				.andReturn();

		return mvcResult.getResponse().getContentAsString();
	}

	public void deleteDTO(String keyName, String urlTemplateName) throws Exception {
		this.mockMvc.perform(delete(urlTemplateName, keyName)).andExpect(status().isAccepted());
	}
}

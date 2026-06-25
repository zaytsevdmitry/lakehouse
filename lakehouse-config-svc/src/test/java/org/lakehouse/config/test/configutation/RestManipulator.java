/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.config.test.configutation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private MockMvc mockMvc;

    public String writeAndReadDTOTest(String keyName, String jsonString, String urlTemplate, String urlTemplateName)
            throws Exception {
        logger.info("writeAndReadDTOTest keyName={}", keyName);
        logger.info("Mock urlTemplate={}", urlTemplate);
        this.mockMvc
                .perform(post(urlTemplate)
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        logger.info("Mock urlTemplate={} status", urlTemplate);
        this.mockMvc.perform(get(urlTemplate)).andDo(print()).andExpect(status().isOk());

        logger.info("Mock urlTemplate={} take back", urlTemplate);
        MvcResult mvcResult = this.mockMvc.perform(get(urlTemplateName, keyName)).andExpect(status().isOk())
                .andReturn();

        logger.info("Mock urlTemplate={} return", urlTemplate);
        return mvcResult.getResponse().getContentAsString();
    }

    public String writeAndReadTextTestByKey(String keyName, String fileExt, String text, String urlTemplate, String urlTemplateName)
            throws Exception {
        String k = keyName.concat(".").concat(fileExt).replaceAll("/",".");
        this.mockMvc
                .perform(post(urlTemplate, k)
                        .content(text)
                        .contentType(MediaType.TEXT_PLAIN)
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isCreated());

        this.mockMvc.perform(get(urlTemplate, k)).andDo(print()).andExpect(status().isOk());

        MvcResult mvcResult = this.mockMvc.perform(get(urlTemplateName, k)).andExpect(status().isOk())
                .andReturn();

        return mvcResult.getResponse().getContentAsString();
    }

    public void deleteDTO(String keyName, String urlTemplateName) throws Exception {
        this.mockMvc.perform(delete(urlTemplateName, keyName)).andExpect(status().isAccepted());
    }
}

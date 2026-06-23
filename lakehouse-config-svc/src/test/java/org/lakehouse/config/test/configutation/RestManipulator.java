/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

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

package org.lakehouse.client.commandline.component.objectactionfacade;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.commandline.model.CommandResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ObjectActionsHelper {

    private final static String columnSeparator = " | ";

    protected static CommandResult getObjectJSONResult(Object o) {
        CommandResult result = new CommandResult(new ArrayList<String>());
        try {
            result.getResultSrtingList().add(ObjectMapping
                    .asJsonStringPretty(o));
        } catch (JsonProcessingException e) {
            result.getResultSrtingList().add(e.getMessage());
        }
        return result;
    }

    private static List<Integer> getMaxColumnSizes(
            String[] header,
            List<String[]> body) {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < header.length; i++) {
            int columnLength = header[i].length();
            for (int j = 0; j < body.size(); j++) {
                if (body.get(j)[i] != null && body.get(j)[i].length() > columnLength)
                    columnLength = body.get(j)[i].length();
            }
            result.add(columnLength);
        }

        return result;
    }

    private static String getRowFormat(
            List<Integer> maxSizes) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < maxSizes.size(); i++) {
            int columnLength = maxSizes.get(i);

            if (i != 0)
                sb.append(columnSeparator);
            sb.append("%-").append(columnLength).append("s");
        }
        System.out.println(sb);
        return sb.toString();
    }

    private static String getFarameSeparator(List<Integer> maxSizes) {

        int rowlenght = columnSeparator.length() * (maxSizes.size() - 1);

        for (int i = 0; i < maxSizes.size(); i++) {
            rowlenght += maxSizes.get(i);
        }

        StringBuffer result = new StringBuffer();

        for (int i = 0; i < rowlenght; i++) {
            result.append("-");
        }
        return result.toString();
    }

    protected static CommandResult table(
            String[] header,
            List<String[]> body) {

        List<Integer> maxColumnSizes = getMaxColumnSizes(header, body);
        String rowFormat = getRowFormat(maxColumnSizes);
        String separator = getFarameSeparator(maxColumnSizes);

        List<String> strings = new ArrayList<String>();
        strings.add(separator);
        strings.add(String.format(rowFormat, header));
        strings.add(separator);
        body.forEach(v ->
                strings.add(String.format(rowFormat, v))
        );
        strings.add(separator);

        return new CommandResult(strings);
    }

    protected static CommandResult coverHttpCode(int code) {
        CommandResult result = new CommandResult(new ArrayList<String>());
        result.getResultSrtingList().add(String.format("http code %d", code));
        return result;
    }

    protected static CommandResult save(String filePath, Object o) {
        CommandResult result = new CommandResult(new ArrayList<String>());

        try {
            ObjectMapping.objectToFile(filePath, o);
        } catch (JsonProcessingException e) {
            result.getResultSrtingList().add(e.getMessage());
        } catch (IOException e) {
            result.getResultSrtingList().add(e.getMessage());
        }
        result.getResultSrtingList().add(String.format("Saved to file %s", filePath));
        return result;
    }
}

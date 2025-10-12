package org.lakehouse.jinja.java.functions;

import org.lakehouse.client.api.constant.SystemVarKeys;

/**
 * DBT style {{source(nameSpaceKey,dataSetKey)}} returns table
 */
public class CompatibilityDBT {
    public static String source(String nameSpace, String dataSet) {
        if (nameSpace == null || nameSpace.isBlank()
                || dataSet == null || dataSet.isBlank())
            return "";
        else {
            return "{{ " + SystemVarKeys.buildSourceTableFullName(nameSpace, dataSet).replace(".", "") + " }}";
        }
    }
}

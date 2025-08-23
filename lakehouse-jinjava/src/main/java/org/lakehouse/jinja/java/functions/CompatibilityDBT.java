package org.lakehouse.jinja.java.functions;

import org.lakehouse.client.api.constant.SystemVarKeys;

/**
 * DBT style {{source(projectKey,dataSetKey)}} returns table
 * */
public class CompatibilityDBT {
    public static String source(String project, String dataSet){
        if ( project == null || project.isBlank()
            || dataSet == null || dataSet.isBlank())
            return "";
        else {
            return "{{ " + SystemVarKeys.buildSourceTableFullName(project, dataSet).replace(".","") + " }}";
        }
    }
}

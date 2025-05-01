package org.lakehouse.client.api.utils;

import java.util.Map;

public class LogPasswdRespectively {
    public static String hidePasswords(Map.Entry<String,String> stringEntry){
        String key = stringEntry.getKey().toLowerCase();
        String result = stringEntry.getValue();
        if (key.contains("passw"))
            result = "*****";
        return result;
    }
}

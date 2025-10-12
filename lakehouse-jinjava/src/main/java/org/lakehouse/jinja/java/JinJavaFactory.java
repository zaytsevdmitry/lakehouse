package org.lakehouse.jinja.java;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import org.lakehouse.jinja.java.functions.CompatibilityDBT;
import org.lakehouse.jinja.java.functions.JinjavaDateTimeFunctions;

public class JinJavaFactory {
    public Jinjava getJinjava() {
        Jinjava jinjava = new Jinjava();
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "adddays",
                        JinjavaDateTimeFunctions.class,
                        "addDaysISO",
                        String.class, Integer.class));
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "addmonths",
                        JinjavaDateTimeFunctions.class,
                        "addMonthsISO",
                        String.class, Integer.class));
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "source",
                        CompatibilityDBT.class,
                        "source",
                        String.class, String.class));
        return jinjava;
    }
}

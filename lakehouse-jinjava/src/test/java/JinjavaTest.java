import com.hubspot.jinjava.Jinjava;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"}, classes = {JinJavaConfiguration.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JinjavaTest {
    @Autowired
    @Qualifier("jinjava")
    Jinjava jinjava;

    @Test
    @Order(1)
    public void testJinjaAddDay() {
        OffsetDateTime targetDateTime = OffsetDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        String template = "{{ adddays('" + targetDateTime.format(dateTimeFormatter) + "', 10)}}";
        String renderedTemplate = jinjava.render(template, new HashMap<>());
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTime.plusDays(10).format(dateTimeFormatter)));
    }

    @Test
    @Order(2)
    public void testJinjaAddDay2() {

        OffsetDateTime targetDateTime = OffsetDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        String template = "{{ adddays(" + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + ",  10)}}";
        Map<String, String> context = new HashMap<>(Map.of(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, targetDateTime.format(dateTimeFormatter)));
        String renderedTemplate = jinjava.render(template, context);
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTime.plusDays(10).format(dateTimeFormatter)));
    }


    @Test
    @Order(3)
    public void testJinjaContextReplacement2() {

        String targetDateTimeStr = "2025-06-12T16:03:00.435821544+03:00";
        String template = "{{ " + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + " }}";
        Map<String, String> context = new HashMap<>(Map.of(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, targetDateTimeStr));
        String renderedTemplate = jinjava.render(template, context);
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTimeStr));
    }

    @Test
    @Order(4)
    public void testJinjaAddMonths() {

        OffsetDateTime targetDateTime = OffsetDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        String template = "{{ addmonths(" + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + ",  10)}}";
        Map<String, String> context = new HashMap<>(Map.of(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, targetDateTime.format(dateTimeFormatter)));
        String renderedTemplate = jinjava.render(template, context);
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTime.plusMonths(10).format(dateTimeFormatter)));
    }

    @Test
    @Order(5)
    public void testJinjavaSource() {
        String nameSpace = "myNameSpace";
        String dataSet = "myDataSet";
        String fullTableName = "myTableName";
        String template = "{{ source('" + nameSpace + "', '" + dataSet + "') }}";
        String contextKey = SystemVarKeys.buildSourceTableFullName(nameSpace, dataSet).replace(".", "");
        Map<String, String> context = new HashMap<>(
                Map.of(
                        contextKey,
                        fullTableName));
        String renderedTemplate = jinjava.render(template, context);
        System.out.println(renderedTemplate);
        System.out.println("--->***" + context.get(contextKey));

        assert (renderedTemplate.equals(fullTableName));
    }
}

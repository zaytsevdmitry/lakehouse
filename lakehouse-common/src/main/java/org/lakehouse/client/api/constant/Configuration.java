package org.lakehouse.client.api.constant;

public class Configuration {
    public enum ModificationRule {
        append("append"),
        overwrite("overwrite"),
        errorIfExists("errorifexists"),
        ignore("ignore");
        private String value;

        private ModificationRule(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}

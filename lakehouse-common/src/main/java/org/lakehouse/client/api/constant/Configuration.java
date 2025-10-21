package org.lakehouse.client.api.constant;

public class Configuration {
    public enum ModificationRule {
        write("w"),
        overwrite("o"),
        errorIfExists("e");
        private String value;

        private ModificationRule(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}

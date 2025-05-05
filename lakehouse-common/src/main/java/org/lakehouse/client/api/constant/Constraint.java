package org.lakehouse.client.api.constant;

public class Constraint {
    public enum Key {
        primary("primary"),
        foreign("foreign"),
        unique("unique");

        public final String label;

        Key(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}

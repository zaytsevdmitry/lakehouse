package org.lakehouse.client.api.constant;

public class DataSetPropertyKeys {
    public enum Key {
        LOCATION("location"),
        TRANSFORM_LOCATION ( "transform.location"),
        TRANSFORM_LOCATION_MODE ("transform.location.mode"),
        FORMAT("format"),
        USING("using");


        Key(String label) {
            this.label = label;
        }

        public final String label;

        @Override
        public String toString() {
            return label;
        }
    }
    public enum Prefix {
        dataSet("dataset."),
        dataSetWrite("dataset.write."),
        dataSetRead("dataset.read."),
        transform("transform."),
        transformWrite("transform.write."),
        transformRead("transform.read.");

        public final String label;

        Prefix(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;}

    }
}

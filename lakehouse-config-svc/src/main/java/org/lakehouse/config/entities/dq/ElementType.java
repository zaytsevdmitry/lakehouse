package org.lakehouse.config.entities.dq;

public enum ElementType {

    TEST_SET(0),
    METRIC(1),
    THRESHOLD(2);

    private final int value;
    ElementType(int value){

        this.value = value;
    }
}

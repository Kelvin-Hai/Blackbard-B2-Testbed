package khkh.bblearn.b2.testbed.enums;

public enum B2Enum {
    VENDOR,
    HANDLE
    ;

    public String getLowerCaseValue() {
        return name().toLowerCase();
    }
}

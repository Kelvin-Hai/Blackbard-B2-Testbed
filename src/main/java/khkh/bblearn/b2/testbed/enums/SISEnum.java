package khkh.bblearn.b2.testbed.enums;

public enum SISEnum {
    DEFAULT_INTEGRATION,
    DELIMITER,
    DATA_SOURCE_BATCH_UID,
    INTEGRATION_HANDLER_TYPE,
    INTEGRATION_STATE,
    INTEGRATION_LOG_LEVEL
    ;

    public String toLowerCase() {
        return name().toLowerCase();
    }
}

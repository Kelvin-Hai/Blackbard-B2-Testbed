package khkh.bblearn.b2.testbed.enums;

public enum FeedEnum {
    DSK,
    SIS,
    USER,
    COURSE,
    COURSE_ENROLMENT,
    ORGANISATION,
    ORGANISATION_ENROLMENT,

    DELIMITER
    ;

    public String getLowerCaseValue() {
        return name().toLowerCase();
    }
}

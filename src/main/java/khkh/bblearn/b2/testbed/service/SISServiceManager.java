package khkh.bblearn.b2.testbed.service;

public interface SISServiceManager {
    // Create integration based on /WEB-INF/feeds/sis.feed
    void createDataIntegrations();

    // Create users using the /WEB-INF/feeds/user.feed
    void processUserFeed();

    // Create courses using the /WEB-INF/feeds/course.feed
    void processCourseFeed();

    // Create courses enrolments using the /WEB-INF/feeds/course_enrolment.feed
    void processCourseEnrolementFeed();

    // Create user, courses, enrolments etc using the feed files in /WEB-INF/feeds
    void processAllFeed();
}

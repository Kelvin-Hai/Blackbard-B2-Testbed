package khkh.bblearn.b2.testbed.controller;

import blackboard.platform.spring.beans.annotations.UserAuthorization;
import khkh.bblearn.b2.testbed.domain.Message;
import khkh.bblearn.b2.testbed.service.DSKServiceManager;
import khkh.bblearn.b2.testbed.service.SISServiceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

@RestController
public class FeedController {
    private static final String response_template = "Welcome, %s!";
    private final AtomicLong counter = new AtomicLong();

    @Autowired
    DSKServiceManager dskServiceManager;

    @Autowired
    SISServiceManager sisServiceManager;

    @UserAuthorization("system.admin.VIEW")
    @GetMapping("/welcome")
    public Message welcome(@RequestParam(value="name", defaultValue="Guest") String name){
        dskServiceManager.createDataSourceKeys();
        sisServiceManager.createDataIntegrations();

        sisServiceManager.processAllFeed();
//        sisServiceManager.processUserFeed();
//        sisServiceManager.processCourseFeed();
//        sisServiceManager.processCourseEnrolementFeed();
        return new Message(counter.incrementAndGet(), String.format(response_template, name));
    }

}

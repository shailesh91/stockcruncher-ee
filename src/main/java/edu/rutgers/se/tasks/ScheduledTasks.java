package edu.rutgers.se.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class ScheduledTasks {

private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTasks.class);

private static boolean needToRunStartupMethod = true;

    @Scheduled(fixedRate = 1000)
    public void keepAlive() {
        //log "alive" every hour for sanity checks
        LOGGER.debug("alive");
        if (needToRunStartupMethod) {
            runOnceOnlyOnStartup();
            needToRunStartupMethod = false;
        }
    }

    public void runOnceOnlyOnStartup() {
        LOGGER.debug("running startup job");
    }

}
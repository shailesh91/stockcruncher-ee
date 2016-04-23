package edu.rutgers.se.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.rutgers.se.svm.SVMMain;

@EnableScheduling
@Component
public class StartupJobs {
	private static final Logger LOGGER = LoggerFactory.getLogger(RealTimeStockDataDownloader.class);
	private static boolean needToRunStartupMethod = true;
	private static SVMMain svmtrain;
	//Run After 1 Minute
    @Scheduled(fixedRate = 3600000)
    public void keepAlive() {
        //log "alive" every minute for sanity checks
        LOGGER.debug("Application Startup Function Alive");
        if (needToRunStartupMethod) {
        	runOnceOnlyOnStartup();
            needToRunStartupMethod = false;
        }
    }
    public void runOnceOnlyOnStartup() {
		//SVM Trainer - Init - Singleton Instance  
    	svmtrain = SVMMain.GetInstance();
    }
    
    
}

package edu.rutgers.se.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.rutgers.se.stockdownloader.RealtimeStockData;

@EnableScheduling
@Component
public class RealTimeStockDataDownloader {
	private static final Logger LOGGER = LoggerFactory.getLogger(RealTimeStockDataDownloader.class);
	
	//Run After 1 Minute
    @Scheduled(fixedRate = 60000)
    public void keepAlive() {
        //log "alive" every minute for sanity checks
        LOGGER.debug("realtime alive");
        RealtimeStockData.collectData();
    }
}

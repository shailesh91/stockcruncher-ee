package edu.rutgers.se.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.rutgers.se.stockdownloader.HistoricalStockData;

@EnableScheduling
@Component
public class HistoricalStockDataDownloader {
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalStockDataDownloader.class);
	
	//Run After 12 Hours
    @Scheduled(fixedRate = 43200000)
    public void keepAlive() {
        //log "alive" every 12 Hours for sanity checks
		LOGGER.debug("historical alive");
		//HistoricalStockData.collectData();
    }
}

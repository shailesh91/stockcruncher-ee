package edu.rutgers.se.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
@Configuration 
@ComponentScan("edu.rutgers.se") 
@EnableWebMvc   
@EnableAsync
public class AppConfig {  

} 
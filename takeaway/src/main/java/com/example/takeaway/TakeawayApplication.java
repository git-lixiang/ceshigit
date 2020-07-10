package com.example.takeaway;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import com.example.takeaway.client.VisibleWindow;

@SpringBootApplication
public class TakeawayApplication {
    public static void main(String[] args) {
//        SpringApplication.run(TakeawayApplication.class, args);
        SpringApplicationBuilder builder = new SpringApplicationBuilder(TakeawayApplication.class);
        ApplicationContext applicationContext = builder.headless(false).run(args);
        VisibleWindow swing = applicationContext.getBean(VisibleWindow.class);
        swing.setVisible(true);
    }

}

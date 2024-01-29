package com.webnori.lighthouse;

import com.webnori.lighthouse.akka.AkkaManager;
import lombok.SneakyThrows;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * This event is executed as late as conceivably possible to indicate that
     * the application is ready to service requests.
     */
    @SneakyThrows
    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        AkkaManager.getInstance();
    }
}

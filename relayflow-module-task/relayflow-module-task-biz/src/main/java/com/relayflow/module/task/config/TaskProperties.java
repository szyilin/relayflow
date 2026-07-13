package com.relayflow.module.task.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "relayflow.task")
public class TaskProperties {

    /** Remind assignees when due time falls within [now, now + window]. */
    private Duration dueRemindWindow = Duration.ofHours(24);
}

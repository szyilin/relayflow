package com.relayflow.framework.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * XXL-JOB executor settings. Admin itself runs as a Compose service; this only configures the in-process executor.
 */
@Data
@ConfigurationProperties(prefix = "relayflow.job")
public class JobProperties {

    /**
     * When false, no executor bean is registered (useful for tests or hosts without Admin).
     */
    private boolean enabled = false;

    /**
     * Admin base URL(s), comma-separated. Example: {@code http://127.0.0.1:8090/xxl-job-admin}
     */
    private String adminAddresses = "http://127.0.0.1:8090/xxl-job-admin";

    /**
     * Must match Admin {@code xxl.job.accessToken}.
     */
    private String accessToken = "";

    private final Executor executor = new Executor();

    @Data
    public static class Executor {

        /**
         * Must match {@code deploy/xxl-job/seed/01_executor.sql} app_name.
         */
        private String appname = "relayflow-server";

        /**
         * Prefer empty; set {@code host.docker.internal} when Admin runs in Docker and server on the host.
         */
        private String address = "";

        private String ip = "";

        private int port = 9999;

        private String logPath = "logs/xxl-job/jobhandler";

        private int logRetentionDays = 30;
    }
}

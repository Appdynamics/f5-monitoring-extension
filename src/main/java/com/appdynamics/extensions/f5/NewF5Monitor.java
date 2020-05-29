/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.f5;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.f5.config.Stat;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by abey.tom on 3/14/16. prashant.mehta => 26/08/20
 */
public class NewF5Monitor extends ABaseMonitor {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(NewF5Monitor.class);

    private MonitorContextConfiguration configuration;

    protected String getDefaultMetricPrefix() {
        return Constants.METRIC_PREFIX;
    }

    public String getMonitorName() {
        return Constants.MonitorName;
    }

    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        try {
            configuration = getContextConfiguration();
            Map<String, ?> config = configuration.getConfigYml();
            Stat.Stats metricConfig = (Stat.Stats) configuration.getMetricsXml();
            if (config != null && metricConfig != null && !ArrayUtils.isEmpty(metricConfig.getStats())) {
                AuthTokenFetcher tokenFetcher = new AuthTokenFetcher(configuration.getContext().getHttpClient());
                Stat[] stats = metricConfig.getStats();
                List<Map<String, ?>> servers = getServers();
                if (servers != null && !servers.isEmpty()) {
                    for (Map server : servers) {
                        server.put(Constants.ENCRYPTION_KEY, config.get(Constants.ENCRYPTION_KEY));
                        String token = tokenFetcher.getToken(server);
                        for (Stat stat : stats) {
                            NewF5MonitorTask task = new NewF5MonitorTask(configuration, tasksExecutionServiceProvider.getMetricWriteHelper(), server, stat, token);
                            tasksExecutionServiceProvider.submit("F5 Stats task-" + stat.getLabel(), task);
                        }
                    }
                }
            } else {
                if (config == null) {
                    logger.error("The config.yml is not loaded due to previous errors.The task will not run");
                }
                if (metricConfig == null) {
                    logger.error("The metrics.xml is not loaded due to previous errors.The task will not run");
                } else if (ArrayUtils.isEmpty(metricConfig.getStats())) {
                    logger.error("The stats read from the metric xml is empty. Please make sure that the metrics xml is correct");
                }
            }
        }catch (Exception e){
            logger.error("Exception in the F5 monitor do run", e);
        }
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        Map<String, ?> config = getContextConfiguration().getConfigYml();
        AssertUtils.assertNotNull(config, "The config is not loaded due to previous error");
        List<Map<String, ?>> servers = (List<Map<String, ?>>) config.get("servers");
        AssertUtils.assertNotNull(servers, "The 'instances' section in config.yml is not initialised");
        return servers;
    }
    /**
     * An Overridden method which gets called to set metrics-xml into the {@code MonitorContextConfiguration}
     *
     * @param args
     */
    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        logger.info("initializing metric.xml file");
        this.getContextConfiguration().setMetricXml(args.get("metric-file"), Stat.Stats.class);
    }


//    public static void main(String[] args) {
//
//        final Map<String, String> taskArgs = new HashMap();
//        taskArgs.put("config-file", "src/main/resources/conf/config.yml");
//        taskArgs.put("metric-file", "src/main/resources/conf/metrics.xml");
//        try {
//            final NewF5Monitor monitor = new NewF5Monitor();
//            monitor.execute(taskArgs, null);
//        } catch (Exception e) {
//            logger.error("Error while running the task", e);
//        }
//    }
}

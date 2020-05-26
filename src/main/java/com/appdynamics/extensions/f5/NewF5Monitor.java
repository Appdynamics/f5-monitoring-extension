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
import com.appdynamics.extensions.f5.config.input.Stat;
import com.google.common.collect.Lists;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by abey.tom on 3/14/16. Updated by Prashant Mehta
 */
public class NewF5Monitor extends ABaseMonitor {
    public static final Logger logger = LoggerFactory.getLogger(NewF5Monitor.class);

    private MonitorContextConfiguration configuration;

    public NewF5Monitor() {
        logger.info(String.format("Using F5 Monitor Version [%s]", getImplementationVersion()));

    }

//    public TaskOutput execute(Map<String, String> map, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
//        return null;
//    }
//
//    protected void initialize(Map<String, String> argsMap) {
//        if (configuration == null) {
//            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
//            MonitorContextConfiguration conf = new MonitorContextConfiguration("Custom Metrics|F5",
//                    new TaskRunnable(), metricWriteHelper);
//            final String configFilePath = argsMap.get("config-file");
//            final String metricFilePath = argsMap.get("metric-file");
//            conf.setConfigYml(configFilePath);
//            conf.setMetricsXml(metricFilePath, Stat.Stats.class);
//            conf.setMetricWriter(MetricWriteHelperFactory.create(this));
//            conf.checkIfInitialized(ConfItem.METRIC_PREFIX, ConfItem.CONFIG_YML, ConfItem.HTTP_CLIENT
//                    , ConfItem.EXECUTOR_SERVICE, ConfItem.METRICS_XML, ConfItem.METRIC_WRITE_HELPER);
//            this.configuration = conf;
//        }
//    }

//    private class TaskRunnable implements Runnable {
//
//        public void run() {
//
//        }
//    }

//    public TaskOutput execute(Map<String, String> map, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
//        logger.debug("The raw arguments are {}", map);
//        initialize(map);
//        configuration.executeTask();
//        return null;
//    }
//
//    private static String getImplementationVersion() {
//        return NewF5Monitor.class.getPackage().getImplementationTitle();
//    }

    protected String getDefaultMetricPrefix() {
        return null;
    }

    public String getMonitorName() {
        return null;
    }

    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        Map<String, ?> config = configuration.getConfigYml();
        Stat.Stats metricConfig = (Stat.Stats) configuration.getMetricsXml();
        if (config != null && metricConfig != null && !ArrayUtils.isEmpty(metricConfig.getStats())) {
            AuthTokenFetcher tokenFetcher = new AuthTokenFetcher(configuration.getContext().getHttpClient());
            Stat[] stats = metricConfig.getStats();
            List<Map> servers = (List) config.get("servers");
            if (servers != null && !servers.isEmpty()) {
                for (Map server : servers) {
                    server.put(Constant.ENCRYPTION_KEY, config.get(Constant.ENCRYPTION_KEY));
                    String token = tokenFetcher.getToken(server);
                    F5MonitorTask f5MonitorTask = new F5MonitorTask(configuration, tasksExecutionServiceProvider.getMetricWriteHelper(), server, stats, token);
                    tasksExecutionServiceProvider.submit(server.get(Constant.DISPLAY_NAME) + "-Task", f5MonitorTask);

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
    }

    protected List<Map<String, ?>> getServers() {
        return Lists.newArrayList();
    }

    public static void main(String[] args) throws TaskExecutionException {

        NewF5Monitor monitor = new NewF5Monitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();

        taskArgs.put("config-file", "src/main/resources/conf/config.yml");
        taskArgs.put("metric-file", "src/main/resources/conf/metrics.xml");

        monitor.execute(taskArgs, null);

    }
}

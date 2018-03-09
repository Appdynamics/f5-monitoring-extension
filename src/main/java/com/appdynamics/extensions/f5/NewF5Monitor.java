/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.f5;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.conf.MonitorConfiguration.ConfItem;
import com.appdynamics.extensions.f5.config.input.Stat;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by abey.tom on 3/14/16.
 */
public class NewF5Monitor extends AManagedMonitor {
    public static final Logger logger = LoggerFactory.getLogger(NewF5Monitor.class);

    private MonitorConfiguration configuration;

    public NewF5Monitor() {
        logger.info(String.format("Using F5 Monitor Version [%s]",
                getImplementationVersion()));

    }

    protected void initialize(Map<String, String> argsMap) {
        if (configuration == null) {
            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration("Custom Metrics|F5",
                    new TaskRunnable(), metricWriteHelper);
            final String configFilePath = argsMap.get("config-file");
            final String metricFilePath = argsMap.get("metric-file");
            conf.setConfigYml(configFilePath);
            conf.setMetricsXml(metricFilePath, Stat.Stats.class);
            conf.setMetricWriter(MetricWriteHelperFactory.create(this));
            conf.checkIfInitialized(ConfItem.METRIC_PREFIX, ConfItem.CONFIG_YML, ConfItem.HTTP_CLIENT
                    , ConfItem.EXECUTOR_SERVICE, ConfItem.METRICS_XML, ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            Map<String, ?> config = configuration.getConfigYml();
            Stat.Stats metricConfig = (Stat.Stats) configuration.getMetricsXmlConfiguration();
            if (config != null && metricConfig != null && !ArrayUtils.isEmpty(metricConfig.getStats())) {
                AuthTokenFetcher tokenFetcher = new AuthTokenFetcher(config, configuration.getHttpClient());
                Stat[] stats = metricConfig.getStats();
                List<Map> servers = (List) config.get("servers");
                if (servers != null && !servers.isEmpty()) {
                    for (Map server : servers) {
                        String token = tokenFetcher.getToken(server);
                        for (Stat stat : stats) {
                            NewF5MonitorTask task = new NewF5MonitorTask(configuration, server, stat,token);
                            configuration.getExecutorService().execute(task);
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
        }
    }

    public TaskOutput execute(Map<String, String> map, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        logger.debug("The raw arguments are {}", map);
        initialize(map);
        configuration.executeTask();
        return null;
    }

    private static String getImplementationVersion() {
        return NewF5Monitor.class.getPackage().getImplementationTitle();
    }
}

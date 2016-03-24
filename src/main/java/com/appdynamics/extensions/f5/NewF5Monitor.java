package com.appdynamics.extensions.f5;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.conf.MonitorConfiguration.ConfItem;
import com.appdynamics.extensions.f5.config.input.Stat;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
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
            MonitorConfiguration conf = new MonitorConfiguration("Custom Metrics|F5");
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

    public TaskOutput execute(Map<String, String> map, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        logger.debug("The raw arguments are {}", map);
        try {
            initialize(map);
            Map<String, ?> config = configuration.getConfig();
            Stat[] stats = ((Stat.Stats) configuration.getMetricsXmlConfiguration()).getStats();
            if (stats != null && stats.length > 0) {
                List<Map> servers = (List) config.get("servers");
                if (servers != null && !servers.isEmpty()) {
                    for (Map server : servers) {
                        for (Stat stat : stats) {
                            NewF5MonitorTask task = new NewF5MonitorTask(configuration, server, stat);
                            configuration.getExecutorService().execute(task);
                        }
                    }
                }
            } else {
                logger.error("The stats read from the metric xml is empty. Please make sure that the metrics xml is correct");
            }
        } catch (Exception e) {
            if (configuration != null && configuration.getMetricWriter() != null) {
                configuration.getMetricWriter().registerError(e.getMessage(), e);
            }
            throw new RuntimeException("Exception while runnign the task", e);
        }

        return null;
    }

    private static String getImplementationVersion() {
        return NewF5Monitor.class.getPackage().getImplementationTitle();
    }
}

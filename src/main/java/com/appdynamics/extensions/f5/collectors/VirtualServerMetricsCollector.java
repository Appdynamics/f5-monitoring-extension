package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.VIRTUAL_SERVERS;
import static com.appdynamics.extensions.f5.util.F5Util.changePathSeparator;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.appdynamics.extensions.f5.http.HttpExecutor;
import com.appdynamics.extensions.f5.models.StatEntry;
import com.appdynamics.extensions.f5.models.Stats;
import com.appdynamics.extensions.f5.responseProcessor.Field;
import com.appdynamics.extensions.f5.responseProcessor.KeyField;
import com.appdynamics.extensions.f5.responseProcessor.ResponseProcessor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 * @author Satish Reddy M
 */
public class VirtualServerMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(VirtualServerMetricsCollector.class);

    private String f5DisplayName;
    private Set<String> virtualServerIncludes;
    private Set<String> metricExcludes;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;

    public VirtualServerMetricsCollector(
            CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.virtualServerIncludes = f5.getVirtualServerIncludes();
        this.metricExcludes = metricsFilter.getVirtualServerMetricExcludes();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {
        LOGGER.info("Virtual Server metrics collector started...");

        if (virtualServerIncludes == null || virtualServerIncludes.isEmpty()) {
            LOGGER.info("No virtual servers were included for monitoring.");
            return null;
        }

        try {

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/ltm/virtual/stats");

            String virtualServerStatsResponse = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if (virtualServerStatsResponse == null) {
                LOGGER.info("Unable to get any response for virtual server metrics");
                return null;
            }

            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Virtual server response : "+ virtualServerStatsResponse);
            }

            Field nodeName = new Field();
            nodeName.setFieldName("tmName");


            KeyField keyField = new KeyField();
            keyField.setFieldNames(nodeName);

            Pattern virtualServerIncludesPattern = createPattern(virtualServerIncludes);

            Stats poolMemberStats = ResponseProcessor.processStatsResponse(virtualServerStatsResponse, virtualServerIncludesPattern, keyField);


            String virtualServerMetricPrefix = getVirtualServerMetricPrefix();
            Pattern excludePatterns = createPattern(metricExcludes);

            Map<String, List<StatEntry>> poolStats = poolMemberStats.getPoolStats();

            for (String vs : poolStats.keySet()) {
                String vsName = changePathSeparator(vs, PATH_SEPARATOR,
                        METRIC_PATH_SEPARATOR, true);
                List<StatEntry> statEntries = poolStats.get(vs);

                for (StatEntry stat : statEntries) {

                    if (isMetricToMonitor(stat.getName(), excludePatterns)) {
                        if (stat.getType() == StatEntry.Type.NUMERIC) {
                            String metricName = String.format("%s%s%s%s", virtualServerMetricPrefix,
                                    vsName, METRIC_PATH_SEPARATOR, stat.getName());

                            BigInteger value = BigInteger.valueOf(Long.valueOf(stat.getValue()));
                            printCollectiveObservedCurrent(metricName, value);
                        }
                    }

                }
            }

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching virtual server list", e);
        }

        return null;
    }

    private String getVirtualServerMetricPrefix() {
        return String.format("%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, VIRTUAL_SERVERS);
    }

}

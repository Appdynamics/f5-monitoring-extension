package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.POOLS;
import static com.appdynamics.extensions.f5.F5Constants.STATUS;
import static com.appdynamics.extensions.f5.util.F5Util.changePathSeparator;
import static com.appdynamics.extensions.f5.util.F5Util.convertToStatus;
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
public class PoolMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(PoolMetricsCollector.class);

    private String f5DisplayName;
    private Set<String> poolIncludes;
    private Set<String> poolMemberIncludes;
    private Set<String> metricExcludes;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;

    public PoolMetricsCollector(CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.poolIncludes = f5.getPoolIncludes();
        this.poolMemberIncludes = f5.getPoolMemberIncludes();
        this.metricExcludes = metricsFilter.getPoolMetricExcludes();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {
        LOGGER.info("Pool metrics collector started...");

        if (poolIncludes == null || poolIncludes.isEmpty()) {
            LOGGER.info("No pools were included for monitoring.");
            return null;
        }

        collectPoolMetrics(httpContext);

        return null;
    }

    private void collectPoolMetrics(HttpClientContext context) {
        try {

            Pattern poolIncludesPattern = createPattern(poolIncludes);

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/ltm/pool/stats");

            String poolStatsResponse = HttpExecutor.execute(httpClient, httpGet, context);

            if (poolStatsResponse == null) {
                LOGGER.info("Unable to get any response for pool metrics");
                return;
            }

            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Pool response : "+ poolStatsResponse);
            }

            Field field = new Field();
            field.setFieldName("tmName");

            KeyField keyField = new KeyField();
            keyField.setFieldNames(field);


            Stats stats = ResponseProcessor.processStatsResponse(poolStatsResponse, poolIncludesPattern, keyField);

            if (stats != null) {
                String poolMetricPrefix = getPoolMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                Map<String, List<StatEntry>> poolStatValues = stats.getPoolStats();
                for (String pool : poolStatValues.keySet()) {
                    String poolName = changePathSeparator(pool,
                            PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);

                    List<StatEntry> statEntries = poolStatValues.get(pool);
                    String availabilityStatus = null;
                    String enabledStatus = null;
                    for (StatEntry stat : statEntries) {

                        if (isMetricToMonitor(stat.getName(), excludePatterns)) {
                            if (stat.getType() == StatEntry.Type.NUMERIC) {
                                String metricName = String.format("%s%s%s%s", poolMetricPrefix,
                                        poolName, METRIC_PATH_SEPARATOR, stat.getName());

                                BigInteger value = BigInteger.valueOf(Long.valueOf(stat.getValue()));
                                printCollectiveObservedCurrent(metricName, value);
                            } else {
                                if ("status.availabilityState".equalsIgnoreCase(stat.getName())) {
                                    availabilityStatus = stat.getValue();
                                } else if ("status.enabledState".equalsIgnoreCase(stat.getName())) {
                                    enabledStatus = stat.getValue();
                                }

                            }
                        }
                    }

                    if (isMetricToMonitor(STATUS, excludePatterns)) {
                        collectPoolStatus(poolName, availabilityStatus, enabledStatus);
                    }
                    collectMemberMetrics(pool, context);
                }
            }

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching pool metrics", e);
        }
    }


    private void collectPoolStatus(String poolName, String availabilityStatus, String enabledStatus) {


        String poolMetricName = changePathSeparator(poolName,
                PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);

        String metricName = String.format("%s%s%s%s", getPoolMetricPrefix(),
                poolMetricName, METRIC_PATH_SEPARATOR, STATUS);

        BigInteger value = BigInteger.valueOf(convertToStatus(availabilityStatus,
                enabledStatus).getValue());
        printCollectiveObservedCurrent(metricName, value);

    }

    private void collectMemberMetrics(String poolName, HttpClientContext context) {
        if (poolMemberIncludes == null || poolMemberIncludes.isEmpty()) {
            LOGGER.info("No pool members were included for monitoring.");
            return;
        }

        PoolMemberMetricsCollector memberMetricsCollector = new PoolMemberMetricsCollector(
                poolMemberIncludes, metricExcludes, f5, httpClient, context);


        Map<String, BigInteger> poolMemberMetrics = memberMetricsCollector.collectMemberMetrics(getPoolMetricPrefix(), poolName);
        if (poolMemberMetrics != null) {
            printMetrics(poolMemberMetrics);
        }
    }

    private String getPoolMetricPrefix() {
        return String.format("%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, POOLS);
    }

}

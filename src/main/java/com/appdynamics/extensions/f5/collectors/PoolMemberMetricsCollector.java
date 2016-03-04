package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.MEMBERS;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.STATUS;
import static com.appdynamics.extensions.f5.util.F5Util.changePathSeparator;
import static com.appdynamics.extensions.f5.util.F5Util.convertToStatus;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;

import com.appdynamics.extensions.f5.config.F5;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 * @author Satish Reddy M
 */
public class PoolMemberMetricsCollector {

    protected static final Logger LOGGER =
            Logger.getLogger(PoolMemberMetricsCollector.class);

    private Pattern excludePatterns;
    private Pattern poolMemberIncludesPattern;
    private F5 f5;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;

    public PoolMemberMetricsCollector(Set<String> poolMemberIncludes,
                                      Set<String> metricExcludes, F5 f5, CloseableHttpClient httpClient, HttpClientContext httpContext) {

        this.excludePatterns = createPattern(metricExcludes);
        this.poolMemberIncludesPattern = createPattern(poolMemberIncludes);
        this.f5 = f5;
        this.httpClient = httpClient;
        this.httpContext = httpContext;
    }

    public Map<String, BigInteger> collectMemberMetrics(String poolMetricPrefix, String poolName) {
        Map<String, BigInteger> poolMemberMetrics = new HashMap<String, BigInteger>();
        try {

            if (poolMemberIncludesPattern == null) {
                LOGGER.info("No pool members were included for monitoring.");
                return null;
            }

            String poolNameToQuery = changePathSeparator(poolName,
                    PATH_SEPARATOR, "~", false);

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/ltm/pool/" + poolNameToQuery + "/members/stats");

            String poolMemberStatsResponse = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if (poolMemberStatsResponse == null) {
                LOGGER.info("Unable to get any response for pool member metrics");
                return null;
            }

            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Pool member response for pool [ "+poolNameToQuery+" ] : "+ poolMemberStatsResponse);
            }

            Field nodeName = new Field();
            nodeName.setFieldName("nodeName");

            Field port = new Field();
            port.setFieldName("port");

            KeyField keyField = new KeyField();
            keyField.setFieldNames(nodeName, port);
            keyField.setFieldSeparator("|");

            Stats poolMemberStats = ResponseProcessor.processStatsResponse(poolMemberStatsResponse, poolMemberIncludesPattern, keyField);

            if (poolMemberStats != null) {

                Map<String, List<StatEntry>> poolMemberStatValues = poolMemberStats.getPoolStats();
                for (String poolMember : poolMemberStatValues.keySet()) {

                    String poolMemberMetricPrefix = getPoolMemberMetricPrefix(poolMetricPrefix, poolName, poolMember);

                    List<StatEntry> statEntries = poolMemberStatValues.get(poolMember);
                    String availabilityStatus = null;
                    String enabledStatus = null;
                    for (StatEntry stat : statEntries) {

                        if (isMetricToMonitor(stat.getName(), excludePatterns)) {
                            if (stat.getType() == StatEntry.Type.NUMERIC) {
                                String metricName = String.format("%s%s%s", poolMemberMetricPrefix,
                                        METRIC_PATH_SEPARATOR, stat.getName());

                                BigInteger value = BigInteger.valueOf(Long.valueOf(stat.getValue()));
                                poolMemberMetrics.put(metricName, value);
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
                        BigInteger value = BigInteger.valueOf(convertToStatus(availabilityStatus,
                                enabledStatus).getValue());
                        poolMemberMetrics.put(poolMemberMetricPrefix + METRIC_PATH_SEPARATOR + STATUS, value);
                    }

                }
            }

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching pool members metrics", e);
        }
        return poolMemberMetrics;
    }

    private String getPoolMemberMetricPrefix(String poolMetricPrefix, String poolName, String poolMemberName) {

        String poolNameToPrint = changePathSeparator(poolName,
                PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);

        String poolMemberFullName = changePathSeparator(poolMemberName,
                PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);

        return String.format("%s%s%s%s%s", poolMetricPrefix, poolNameToPrint, METRIC_PATH_SEPARATOR, MEMBERS, poolMemberFullName);
    }
}

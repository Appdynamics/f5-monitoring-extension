package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.SERVERS;
import static com.appdynamics.extensions.f5.F5Constants.SSL;
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
public class ServerSSLMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(ServerSSLMetricsCollector.class);

    private String f5DisplayName;
    private Set<String> serverSSLIncludes;
    private Set<String> metricExcludes;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;

    public ServerSSLMetricsCollector(
            CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.serverSSLIncludes = f5.getServerSSLProfileIncludes();
        this.metricExcludes = metricsFilter.getServerSSLProfileMetricExcludes();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {
        LOGGER.info("Profile Server SSL metrics collector started...");

        if (serverSSLIncludes == null || serverSSLIncludes.isEmpty()) {
            LOGGER.info("No server ssl names were included for monitoring.");
            return null;
        }

        try {

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/ltm/profile/server-ssl/stats");

            String serverSSLStatsResponse = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if (serverSSLStatsResponse == null) {
                LOGGER.info("Unable to get any response for server ssl metrics");
                return null;
            }

            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Server SSL response : "+ serverSSLStatsResponse);
            }

            Field nodeName = new Field();
            nodeName.setFieldName("tmName");

            KeyField keyField = new KeyField();
            keyField.setFieldNames(nodeName);

            Pattern serverSSLIncludesPattern = createPattern(serverSSLIncludes);

            Stats serverSSLStats = ResponseProcessor.processStatsResponse(serverSSLStatsResponse, serverSSLIncludesPattern, keyField);

            Map<String, List<StatEntry>> stats = serverSSLStats.getPoolStats();

            String serverSSLMetricPrefix = getServerSSLMetricPrefix();
            Pattern excludePatterns = createPattern(metricExcludes);

            for (String serverSSL : stats.keySet()) {
                String serverSSLName = changePathSeparator(serverSSL, PATH_SEPARATOR,
                        METRIC_PATH_SEPARATOR, true);
                List<StatEntry> statEntries = stats.get(serverSSL);

                for (StatEntry stat : statEntries) {

                    if (isMetricToMonitor(stat.getName(), excludePatterns)) {
                        if (stat.getType() == StatEntry.Type.NUMERIC) {
                            String metricName = String.format("%s%s%s%s", serverSSLMetricPrefix,
                                    serverSSLName, METRIC_PATH_SEPARATOR, stat.getName());

                            BigInteger value = BigInteger.valueOf(Long.valueOf(stat.getValue()));
                            printCollectiveObservedCurrent(metricName, value);
                        }
                    }

                }
            }

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching server ssl profile metrics", e);
        }

        return null;
    }

    private String getServerSSLMetricPrefix() {
        return String.format("%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, SSL,
                METRIC_PATH_SEPARATOR, SERVERS);
    }

}

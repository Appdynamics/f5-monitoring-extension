package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.CLIENTS;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
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
public class ClientSSLMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(ClientSSLMetricsCollector.class);

    private String f5DisplayName;
    private Set<String> clientSSLIncludes;
    private Set<String> metricExcludes;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;

    public ClientSSLMetricsCollector(CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.clientSSLIncludes = f5.getClientSSLProfileIncludes();
        this.metricExcludes = metricsFilter.getClientSSLProfileMetricExcludes();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {
        LOGGER.info("Profile Client SSL metrics collector started...");

        if (clientSSLIncludes == null || clientSSLIncludes.isEmpty()) {
            LOGGER.info("No client ssl names were included for monitoring.");
            return null;
        }

        try {

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/ltm/profile/client-ssl/stats");

            String clientSSLStatsResponse = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if (clientSSLStatsResponse == null) {
                LOGGER.info("Unable to get any response for client ssl metrics");
                return null;
            }

            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Client SSL Profile response : "+ clientSSLStatsResponse);
            }

            Field nodeName = new Field();
            nodeName.setFieldName("tmName");

            KeyField keyField = new KeyField();
            keyField.setFieldNames(nodeName);

            Pattern clientSSLIncludesPattern = createPattern(clientSSLIncludes);

            Stats clientSSLStats = ResponseProcessor.processStatsResponse(clientSSLStatsResponse, clientSSLIncludesPattern, keyField);


            Map<String, List<StatEntry>> stats = clientSSLStats.getPoolStats();

            String clientSSLMetricPrefix = getClientSSLMetricPrefix();
            Pattern excludePatterns = createPattern(metricExcludes);

            for (String clientSSL : stats.keySet()) {
                String clientSSLName = changePathSeparator(clientSSL, PATH_SEPARATOR,
                        METRIC_PATH_SEPARATOR, true);
                List<StatEntry> statEntries = stats.get(clientSSL);

                for (StatEntry stat : statEntries) {

                    String metricName = stat.getName();
                    if (isMetricToMonitor(metricName, excludePatterns)) {

                        if (stat.getType() == StatEntry.Type.NUMERIC) {
                            BigInteger value = new BigInteger(stat.getValue());
                            String fullMetricName = String.format("%s%s%s%s", clientSSLMetricPrefix,
                                    clientSSLName, METRIC_PATH_SEPARATOR, metricName);


                            printCollectiveObservedCurrent(fullMetricName, value);
                        }
                    }

                }
            }

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching client ssl profile metrics", e);
        }

        return null;
    }

    private String getClientSSLMetricPrefix() {
        return String.format("%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, SSL,
                METRIC_PATH_SEPARATOR, CLIENTS);
    }
}
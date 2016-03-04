package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.COMPRESSION;
import static com.appdynamics.extensions.f5.F5Constants.HTTP;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.NETWORK;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.appdynamics.extensions.f5.http.HttpExecutor;
import com.appdynamics.extensions.f5.responseProcessor.ResponseProcessor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 * @author Satish Reddy M
 */
public class HttpCompressionMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(HttpCompressionMetricsCollector.class);

    private String f5DisplayName;
    private Set<String> metricExcludes;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;

    public HttpCompressionMetricsCollector(
            CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.metricExcludes = metricsFilter.getHttpCompressionMetricExcludes();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {

        LOGGER.info("HTTP Compression metrics collector started...");

        try {

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/ltm/profile/http-compression/stats");

            String httpCompressionStatsResponse = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if (httpCompressionStatsResponse == null) {
                LOGGER.info("Unable to get any response for HTTP Compression metrics");
                return null;
            }

            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Http compression profile response : "+ httpCompressionStatsResponse);
            }

            Map<String, BigInteger> aggregatedStats = ResponseProcessor.aggregateStatsResponse(httpCompressionStatsResponse);

            String httpCompressionMetricPrefix = getHttpCompressionMetricPrefix();
            Pattern excludePatterns = createPattern(metricExcludes);


            for (String key : aggregatedStats.keySet()) {
                BigInteger value = aggregatedStats.get(key);

                if (value != null && isMetricToMonitor(key, excludePatterns)) {
                    String metricName = String.format("%s%s", httpCompressionMetricPrefix, key);
                    printCollectiveObservedCurrent(metricName, value);
                }

            }

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching http compression metrics", e);
        }

        return null;
    }

    private String getHttpCompressionMetricPrefix() {
        return String.format("%s%s%s%s%s%s%s%s",
                f5DisplayName, METRIC_PATH_SEPARATOR,
                NETWORK, METRIC_PATH_SEPARATOR,
                HTTP, METRIC_PATH_SEPARATOR,
                COMPRESSION, METRIC_PATH_SEPARATOR);
    }

}

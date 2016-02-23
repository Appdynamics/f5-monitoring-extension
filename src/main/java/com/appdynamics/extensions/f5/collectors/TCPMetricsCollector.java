package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.TCP;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.appdynamics.extensions.f5.http.HttpExecutor;
import com.appdynamics.extensions.f5.responseProcessor.PoolResponseProcessor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 */
public class TCPMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(TCPMetricsCollector.class);

    private String f5DisplayName;
    private Set<String> metricExcludes;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;

    public TCPMetricsCollector(
            CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.metricExcludes = metricsFilter.getTcpMetricExcludes();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {
        LOGGER.info("TCP metrics collector started...");

        try {

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/ltm/profile/tcp/stats");

            CloseableHttpResponse response = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if(response == null) {
                LOGGER.info("Unable to get any response for TCP metrics");
                return null;
            }

            String tcpStatsResponse = EntityUtils.toString(response.getEntity());


            Map<String, BigInteger> aggregatedStats = PoolResponseProcessor.aggregateStatsResponse(tcpStatsResponse);

            String tcpMetricPrefix = getTCPMetricPrefix();
            Pattern excludePatterns = createPattern(metricExcludes);


            for (String key : aggregatedStats.keySet()) {
                BigInteger value = aggregatedStats.get(key);

                if (isMetricToMonitor(key, excludePatterns)) {
                    String metricName = String.format("%s%s", tcpMetricPrefix, key);
                    printCollectiveObservedCurrent(metricName, value);
                }

            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching tcp statistics", e);
        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching tcp statistics", e);
        }

        return null;
    }

    private String getTCPMetricPrefix() {
        return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR,
                "Network", METRIC_PATH_SEPARATOR, TCP, METRIC_PATH_SEPARATOR);
    }
}

package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.INTERFACES;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.NETWORK;
import static com.appdynamics.extensions.f5.F5Constants.STATUS;
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
import com.appdynamics.extensions.f5.responseProcessor.PoolResponseProcessor;
import com.appdynamics.extensions.f5.util.F5Util.NetworkInterfaceStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 */
public class NetworkInterfaceMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(NetworkInterfaceMetricsCollector.class);

    private String f5DisplayName;
    private Set<String> networkInterfaceIncludes;
    private Set<String> metricExcludes;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;

    public NetworkInterfaceMetricsCollector(
            CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.networkInterfaceIncludes = f5.getNetworkInterfaceIncludes();
        this.metricExcludes = metricsFilter.getNetworkInterfaceMetricExcludes();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {
        LOGGER.info("Network interface metrics collector started...");

        if (networkInterfaceIncludes == null || networkInterfaceIncludes.isEmpty()) {
            LOGGER.info("No network interfaces were included for monitoring.");
            return null;
        }

        try {

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/net/interface/stats");

            CloseableHttpResponse response = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if(response == null) {
                LOGGER.info("Unable to get any response for network interface metrics");
                return null;
            }

            String networkInterfaceStatsResponse = EntityUtils.toString(response.getEntity());


            Field nodeName = new Field();
            nodeName.setFieldName("tmName");

            KeyField keyField = new KeyField();
            keyField.setFieldNames(nodeName);

            Pattern networkInterfaceIncludesPattern = createPattern(networkInterfaceIncludes);

            Stats networkInterfaceStats = PoolResponseProcessor.processPoolStatsResponse(networkInterfaceStatsResponse, networkInterfaceIncludesPattern, keyField);


            Map<String, List<StatEntry>> stats = networkInterfaceStats.getPoolStats();

            String networkInterfaceMetricPrefix = getNetworkInterfaceMetricPrefix();
            Pattern excludePatterns = createPattern(metricExcludes);

            for (String networkInterface : stats.keySet()) {

                List<StatEntry> statEntries = stats.get(networkInterface);

                for (StatEntry stat : statEntries) {

                    String metricName = stat.getName();

                    if (isMetricToMonitor(metricName, excludePatterns)) {
                        if (stat.getType() == StatEntry.Type.NUMERIC) {
                            String fullMetricName = String.format("%s%s%s%s", networkInterfaceMetricPrefix,
                                    networkInterface, METRIC_PATH_SEPARATOR, metricName);

                            BigInteger value = new BigInteger(stat.getValue());

                            printCollectiveObservedCurrent(fullMetricName, value);
                        } else {
                            if ("status".equalsIgnoreCase(stat.getName())) {
                                String fullMetricName = String.format("%s%s%s%s", networkInterfaceMetricPrefix,
                                        networkInterface,
                                        METRIC_PATH_SEPARATOR, STATUS);
                                BigInteger value = BigInteger.valueOf(NetworkInterfaceStatus.getStatus(stat.getValue()));
                                printCollectiveObservedCurrent(fullMetricName, value);
                            }
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching network interface list", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching network interface list", e);
        }

        return null;
    }

    private String getNetworkInterfaceMetricPrefix() {
        return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR,
                NETWORK, METRIC_PATH_SEPARATOR, INTERFACES, METRIC_PATH_SEPARATOR);
    }

}

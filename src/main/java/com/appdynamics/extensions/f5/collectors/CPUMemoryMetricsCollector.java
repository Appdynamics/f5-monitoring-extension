package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.CPU;
import static com.appdynamics.extensions.f5.F5Constants.DASH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.HOST;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.SYSTEM;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.http.HttpExecutor;
import com.appdynamics.extensions.f5.models.HostCPUMemoryStats;
import com.appdynamics.extensions.f5.responseProcessor.ResponseProcessor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.util.List;

/**
 * @author Florencio Sarmiento
 * @author Satish Reddy M
 */
public class CPUMemoryMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(CPUMemoryMetricsCollector.class);

    private String f5DisplayName;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;


    public CPUMemoryMetricsCollector(CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {
        LOGGER.info("CPU metrics collector started...");

        try {

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/sys/hostInfo");

            String hostInfoResponse = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if (hostInfoResponse == null) {
                LOGGER.info("Unable to get any response for CPU and Memory metrics");
                return null;
            }

            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Host Info for CPU and Memory response : "+ hostInfoResponse);
            }

            List<HostCPUMemoryStats> cpuStatsList = ResponseProcessor.parseHostInfoResponse(hostInfoResponse);
            String cpuMetricPrefix = getCPUMetricPrefix();
            String memoryMetricPrefix = getMemoryMetricPrefix();

            for (HostCPUMemoryStats hostCPUStats : cpuStatsList) {
                String cpuBusyMetricName = String.format("%s%s%s%s%s%s", cpuMetricPrefix,
                        HOST, DASH_SEPARATOR, hostCPUStats.getHostId(), METRIC_PATH_SEPARATOR, "CPU % BUSY");

                BigInteger cpuUsage = BigInteger.valueOf(100).subtract(hostCPUStats.getOneMinAvgIdle().divide(BigInteger.valueOf(hostCPUStats.getCpuCount())));
                printCollectiveObservedCurrent(cpuBusyMetricName, cpuUsage);

                String memoryUsedBytesMetricName = String.format("%s%s%s%s%s%s",
                        memoryMetricPrefix, HOST, DASH_SEPARATOR, hostCPUStats.getHostId(),
                        METRIC_PATH_SEPARATOR, "STATISTIC_MEMORY_USED_BYTES");
                printCollectiveObservedCurrent(memoryUsedBytesMetricName, hostCPUStats.getMemoryUsed());

                String memoryTotalBytesMetricName = String.format("%s%s%s%s%s%s",
                        memoryMetricPrefix, HOST, DASH_SEPARATOR, hostCPUStats.getHostId(),
                        METRIC_PATH_SEPARATOR, "STATISTIC_MEMORY_TOTAL_BYTES");
                printCollectiveObservedCurrent(memoryTotalBytesMetricName, hostCPUStats.getMemoryTotal());

            }

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching cpu metrics", e);
        }

        return null;
    }

    private String getCPUMetricPrefix() {
        return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR,
                SYSTEM, METRIC_PATH_SEPARATOR, CPU, METRIC_PATH_SEPARATOR);
    }

    private String getMemoryMetricPrefix() {
        return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR,
                "System", METRIC_PATH_SEPARATOR, "Memory", METRIC_PATH_SEPARATOR);
    }
}
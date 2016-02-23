package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.DISKS;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.SYSTEM;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.http.HttpExecutor;
import com.appdynamics.extensions.f5.models.DiskStats;
import com.appdynamics.extensions.f5.responseProcessor.PoolResponseProcessor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.util.List;

/**
 * @author Florencio Sarmiento
 */
public class DiskMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(DiskMetricsCollector.class);

    private String f5DisplayName;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;

    public DiskMetricsCollector(CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, F5Monitor monitor, String metricPrefix) {
        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {

        LOGGER.info("Disk metrics collector started...");

        try {

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/sys/disk/logical-disk");

            CloseableHttpResponse response = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if(response == null) {
                LOGGER.info("Unable to get any response for disk metrics");
                return null;
            }

            String logicalDisksResponse = EntityUtils.toString(response.getEntity());


            List<DiskStats> diskStatses = PoolResponseProcessor.parseDisksResponse(logicalDisksResponse);
            String diskMetricPrefix = getDiskMetricPrefix();

            for (DiskStats diskStats : diskStatses) {

                String spaceFreeMetric = String.format("%s%s%s%s", diskMetricPrefix, diskStats.getName(),
                        METRIC_PATH_SEPARATOR, "Space Available");
                printCollectiveObservedCurrent(spaceFreeMetric, diskStats.getFree());


                String spaceUsedMetric = String.format("%s%s%s%s", diskMetricPrefix, diskStats.getName(),
                        METRIC_PATH_SEPARATOR, "Space Used");
                printCollectiveObservedCurrent(spaceUsedMetric, diskStats.getInUse());

                String spaceTotalMetric = String.format("%s%s%s%s", diskMetricPrefix, diskStats.getName(),
                        METRIC_PATH_SEPARATOR, "Total Space");
                printCollectiveObservedCurrent(spaceTotalMetric, diskStats.getSize());

                String spaceReservedMetric = String.format("%s%s%s%s", diskMetricPrefix, diskStats.getName(),
                        METRIC_PATH_SEPARATOR, "Space Reserved");
                printCollectiveObservedCurrent(spaceReservedMetric, diskStats.getReserved());
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching disk list", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching disk list", e);
        }

        return null;
    }

    private String getDiskMetricPrefix() {
        return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR,
                SYSTEM, METRIC_PATH_SEPARATOR, DISKS, METRIC_PATH_SEPARATOR);
    }

}

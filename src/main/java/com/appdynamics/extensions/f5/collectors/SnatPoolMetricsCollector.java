package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.SNAT_POOLS;
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
import com.appdynamics.extensions.f5.responseProcessor.PoolResponseProcessor;
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
public class SnatPoolMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(SnatPoolMetricsCollector.class);

    private String f5DisplayName;
    private Set<String> snatPoolIncludes;
    private Set<String> metricExcludes;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;

    public SnatPoolMetricsCollector(
            CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.snatPoolIncludes = f5.getSnatPoolIncludes();
        this.metricExcludes = metricsFilter.getSnatPoolMetricExcludes();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {
        LOGGER.info("SNAT Pool metrics collector started...");

        if (snatPoolIncludes == null || snatPoolIncludes.isEmpty()) {
            LOGGER.info("No SNAT pools were included for monitoring.");
            return null;
        }

        try {

            Pattern poolIncludesPattern = createPattern(snatPoolIncludes);

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/ltm/snatpool/stats");

            CloseableHttpResponse response = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if(response == null) {
                LOGGER.info("Unable to get any response for snat pool metrics");
                return null;
            }

            String poolStatsResponse = EntityUtils.toString(response.getEntity());


            Field field = new Field();
            field.setFieldName("tmName");

            KeyField keyField = new KeyField();
            keyField.setFieldNames(field);


            Stats stats = PoolResponseProcessor.processPoolStatsResponse(poolStatsResponse, poolIncludesPattern, keyField);


            if (stats != null) {
                String poolMetricPrefix = getPoolMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                Map<String, List<StatEntry>> poolStatValues = stats.getPoolStats();
                for (String pool : poolStatValues.keySet()) {
                    String poolName = changePathSeparator(pool,
                            PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);

                    List<StatEntry> statEntries = poolStatValues.get(pool);
                    for (StatEntry stat : statEntries) {

                        if (isMetricToMonitor(stat.getName(), excludePatterns)) {
                            if (stat.getType() == StatEntry.Type.NUMERIC) {
                                String metricName = String.format("%s%s%s%s", poolMetricPrefix,
                                        poolName, METRIC_PATH_SEPARATOR, stat.getName());

                                BigInteger value = BigInteger.valueOf(Long.valueOf(stat.getValue()));
                                printCollectiveObservedCurrent(metricName, value);
                            }
                        }
                    }
                }
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching snat pool list", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching snat pool list", e);
        }

        return null;
    }

    private String getPoolMetricPrefix() {
        return String.format("%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, SNAT_POOLS);
    }

}

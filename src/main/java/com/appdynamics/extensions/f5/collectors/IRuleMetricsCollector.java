package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.IRULES;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
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
 * @author Satish Reddy M
 */

public class IRuleMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(IRuleMetricsCollector.class);

    private String f5DisplayName;
    private Set<String> iRuleIncludes;
    private Set<String> metricExcludes;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private F5 f5;

    public IRuleMetricsCollector(
            CloseableHttpClient httpClient, HttpClientContext httpContext, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.f5DisplayName = f5.getDisplayName();
        this.iRuleIncludes = f5.getiRuleIncludes();
        this.metricExcludes = metricsFilter.getiRuleMetricExcludes();
        this.httpClient = httpClient;
        this.httpContext = httpContext;
        this.f5 = f5;
    }

    public Void call() {
        LOGGER.info("iRule metrics collector started...");

        if (iRuleIncludes == null || iRuleIncludes.isEmpty()) {
            LOGGER.info("No rule names were included for monitoring.");
            return null;
        }

        try {

            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/ltm/rule/stats");

            String iRuleStatsResponse = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if (iRuleStatsResponse == null) {
                LOGGER.info("Unable to get any response for iRules metrics");
                return null;
            }

            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("iRules response : "+ iRuleStatsResponse);
            }

            Field nodeName = new Field();
            nodeName.setFieldName("tmName");

            Field eventType = new Field();
            eventType.setFieldName("eventType");


            KeyField keyField = new KeyField();
            keyField.setFieldNames(nodeName, eventType);
            keyField.setFieldSeparator("|");

            Pattern virtualServerIncludesPattern = createPattern(iRuleIncludes);

            Stats iRuleStats = ResponseProcessor.processStatsResponse(iRuleStatsResponse, virtualServerIncludesPattern, keyField);


            Map<String, List<StatEntry>> stats = iRuleStats.getPoolStats();

            String iRuleMetricPrefix = getIRuleMetricPrefix();
            Pattern excludePatterns = createPattern(metricExcludes);

            for (String iRule : stats.keySet()) {
                String iRuleName = changePathSeparator(iRule, PATH_SEPARATOR,
                        METRIC_PATH_SEPARATOR, true);
                List<StatEntry> statEntries = stats.get(iRule);

                for (StatEntry stat : statEntries) {
                    String metricName = stat.getName();

                    if (isMetricToMonitor(metricName, excludePatterns)) {
                        if (stat.getType() == StatEntry.Type.NUMERIC) {
                            String fullMetricName = String.format("%s%s%s%s", iRuleMetricPrefix,
                                    iRuleName,
                                    METRIC_PATH_SEPARATOR, metricName);

                            BigInteger value = BigInteger.valueOf(Long.valueOf(stat.getValue()));
                            printCollectiveObservedCurrent(fullMetricName, value);
                        }
                    }

                }
            }

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching iRule metrics", e);
        }

        return null;
    }

    private String getIRuleMetricPrefix() {
        return String.format("%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, IRULES);
    }

}

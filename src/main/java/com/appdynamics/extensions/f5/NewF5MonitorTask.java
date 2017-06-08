package com.appdynamics.extensions.f5;

import com.appdynamics.extensions.StringUtils;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.f5.config.input.Metric;
import com.appdynamics.extensions.f5.config.input.Naming;
import com.appdynamics.extensions.f5.config.input.Stat;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.util.*;
import com.google.common.base.Strings;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static com.appdynamics.extensions.util.AssertUtils.assertNotNull;

/**
 * Created by abey.tom on 3/11/16.
 */
public class NewF5MonitorTask implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(NewF5MonitorTask.class);

    private Map server;
    private Stat stat;
    private String token;
    private MonitorConfiguration configuration;
    private String serverPrefix;
    private Map<String, String> varMap;

    //This is for main stats
    public NewF5MonitorTask(MonitorConfiguration configuration, Map server, Stat stat, String token) {
        this.configuration = configuration;
        this.server = server;
        this.stat = stat;
        this.token = token;
        String serverName = (String) server.get("name");
        String serverPrefix = configuration.getMetricPrefix();
        if (!Strings.isNullOrEmpty(serverName)) {
            serverPrefix = serverPrefix + "|" + serverName;
        }
        this.serverPrefix = serverPrefix;
    }

    // This is for child stats
    public NewF5MonitorTask(MonitorConfiguration configuration, Map server,
                            Stat stat, String serverPrefix, Map<String, String> varMap, String token) {
        this.server = server;
        this.stat = stat;
        this.configuration = configuration;
        this.serverPrefix = serverPrefix;
        this.varMap = varMap;
        this.token = token;
    }

    public void run() {
        try {
            runTask();
        } catch (Exception e) {
            configuration.getMetricWriter().registerError(e.getMessage(), e);
            logger.error("Error while running the task", e);
        }
    }

    private void runTask() {
        Metric[] metrics = stat.getMetrics();
        boolean isFilterConfigured = isFilterConfigured(stat);
        if (metrics != null && isFilterConfigured) {
            String endpoint = stat.getUrl();
            String label = stat.getLabel();
            assertNotNull(endpoint, "The url attribute cannot be empty for stat " + stat);
            assertNotNull(label, "The label attribute cannot be empty for stat " + stat);
            String url = buildUrl(server, endpoint, varMap);
            logger.info("Fetching the F5 Stats from the URL {}", url);
            JsonNode json = getResponseAsJson(url);
            if (json != null) {
                Map<String, JsonNode> childMap = getChildren(json, stat);
                if (childMap != null && !childMap.isEmpty()) {
                    extractMetrics(childMap, stat, serverPrefix);
                } else {
                    logger.error("The response content for url {} doesn't contain children [{}]", url, stat.getChildren());
                }
            } else {
                configuration.getMetricWriter().registerError("Error while getting the data from "
                        + url + ". Please check logs for more information.", null);
            }
        } else {
            if (!isFilterConfigured) {
                logger.info("The filters are not configured for the stat {}. Skipping", stat.getUrl());
            }
            if (metrics == null) {
                logger.info("The metrics are not configured for the stat {}. Skipping", stat.getUrl());
            }
        }
    }

    protected JsonNode getResponseAsJson(String url) {
        Map<String, String> headers;
        if (token != null) {
            headers = Collections.singletonMap("X-F5-Auth-Token", token);
        } else {
            headers = Collections.emptyMap();
        }
        return HttpClientUtils.getResponseAsJson(configuration.getHttpClient(), url, JsonNode.class, headers);
    }

    /**
     * Gets the elements based on the "children" attribute of stats.
     * The metrics are collected after iterating the child nodes
     *
     * @param json - JSON output from API
     * @param stat - Read from the metrics.xml
     * @return a map of children. The key is the name of the entity.
     * The name is extracted based on <naming> element.
     */
    protected Map<String, JsonNode> getChildren(JsonNode json, Stat stat) {
        String childrenPath = stat.getChildren();
        if (!Strings.isNullOrEmpty(childrenPath)) {
            String[] split = childrenPath.split("\\|");
            JsonNode children = JsonUtils.getNestedObject(json, split);
            Map<String, JsonNode> childMap = new HashMap<String, JsonNode>();
            //In case of logical-disk it is an array
            if (children instanceof ArrayNode) {
                ArrayNode nodes = (ArrayNode) children;
                for (JsonNode child : nodes) {
                    String entityName = getEntityName(null, child, stat);
                    childMap.put(entityName, child);
                }
            } else if (children != null) {
                //In other cases, it is a map with key-value.
                // The entity name is extracted from the key
                Iterator<String> fields = children.getFieldNames();
                while (fields.hasNext()) {
                    String field = fields.next();
                    JsonNode child = children.get(field);
                    String entityName = getEntityName(field, child, stat);
                    childMap.put(entityName, child);
                }
            } else {
                logger.error("Cannot find the children from the path {} for the node {}", childrenPath, json);
            }
            return childMap;
        } else {
            logger.error("The children element of the stat [{}] is not set", stat.getUrl());
        }
        return null;
    }

    /**
     * Check if there is a filter configured for a stat in config.yml. The filter-name is an attr of <stat/>.
     * If no filters are configured, that stat is skipped. Currently only includes is supported.
     *
     * @param stat
     * @return
     */
    private boolean isFilterConfigured(Stat stat) {
        Map filter = getFilter(stat);
        if (filter != null) {
            List includes = (List) filter.get("includes");
            return includes != null && !includes.isEmpty();
        }
        return false;
    }

    /**
     * Iterates over child nodes that is extracted with the children attribute of <stat/>.
     * The metrics are extracted from the child nodes.
     *
     * @param childNodes
     * @param stat
     * @param serverPrefix
     */
    private void extractMetrics(Map<String, JsonNode> childNodes, Stat stat, String serverPrefix) {
        Metric[] metrics = stat.getMetrics();
        String statPrefix = serverPrefix + "|" + StringUtils.trim(stat.getLabel(), "|");
        //if aggregate=true in the <metric>, the it will be aggregated.
        AggregatorFactory aggregatorFactory = new AggregatorFactory();
        for (Map.Entry<String, JsonNode> mapEntry : childNodes.entrySet()) {
            String name = mapEntry.getKey();
            JsonNode childNode = mapEntry.getValue();
            //Apply the filter
            if (isIncluded(name, childNode, stat)) {
                // Replace : with -, otherwise the metric path will be split on :
                String entityNamePrefix = statPrefix + "|" + name.replace(":", "-");
                if (logger.isDebugEnabled()) {
                    logger.debug("Processing the prefix {} and entry {}", entityNamePrefix
                            , getTextValue(childNode, "nestedStats", "selfLink"));
                }
                for (Metric metric : metrics) {
                    try {
                        String metricType = getMetricType(stat, metric);
                        if (metricType != null) {
                            extractAndReport(metric, childNode, entityNamePrefix, metricType, aggregatorFactory);
                        } else {
                            logger.error("Cannot get the metric-type for {}|{}"
                                    , entityNamePrefix, metric.getAttr());
                        }
                    } catch (Exception e) {
                        String msg = "Error while getting the metrics for "
                                + entityNamePrefix + "|" + metric.getAttr();
                        logger.error(msg, e);
                        configuration.getMetricWriter().registerError(msg, e);
                    }
                }
                // If there are any child <stat> elements for a parent <stat>
                processChildStats(childNode, stat.getStats(), entityNamePrefix, name);
            } else {
                logger.debug("Excluding the entry {} since it didn't match any filter",
                        getTextValue(childNode, "nestedStats", "selfLink"));
            }
        }
        Collection<Aggregator<AggregatorKey>> aggregators = aggregatorFactory.getAggregators();
        for (Aggregator<AggregatorKey> aggregator : aggregators) {
            Set<AggregatorKey> keys = aggregator.keys();
            for (AggregatorKey key : keys) {
                BigDecimal value = aggregator.getAggregatedValue(key);
                printMetrics(statPrefix + "|" + key.getMetricPath(), value, key.getMetricType());
            }
        }
    }

    private void processChildStats(JsonNode parentNode, Stat[] childStats, String entryPrefix, String parentName) {
        if (childStats != null && childStats.length > 0) {
            for (Stat childStat : childStats) {
                logger.debug("Running the task for the child stat [{}] of parent [{}]", childStat.getUrl(), parentName);
                if (!Strings.isNullOrEmpty(childStat.getUrl())) {
                    Map<String, String> varMap = Collections.singletonMap("$PARENT_NAME", parentName);
                    NewF5MonitorTask task = new NewF5MonitorTask(configuration, server, childStat, entryPrefix, varMap, token);
                    configuration.getExecutorService().submit(task);
                } else {
                    //TODO children attribute with $PARENT_NAME replace?
                    // need to clone the stat for that.
                    Map<String, JsonNode> children = getChildren(parentNode, childStat);
                    extractMetrics(children, childStat, entryPrefix);
                }
            }
        }
    }

    /**
     * AVG.AVG.COL
     * - Aggregation Type
     * - Time Rollup
     * - Cluster Rollup
     *
     * @param stat
     * @param metric
     * @return
     */
    private String getMetricType(Stat stat, Metric metric) {
        if (!Strings.isNullOrEmpty(metric.getMetricType())) {
            return metric.getMetricType();
        } else if (!Strings.isNullOrEmpty(stat.getMetricType())) {
            return stat.getMetricType();
        } else {
            return null;
        }
    }

    /**
     * Extract the value based in <metric> from the childNode. Apply converter and the multiplier to the value.
     *
     * @param metric
     * @param childNode
     * @param metricPrefix
     * @param metricType
     * @param aggregatorFactory
     */
    private void extractAndReport(Metric metric, JsonNode childNode, String metricPrefix, String metricType,
                                  AggregatorFactory aggregatorFactory) {
        String attr = metric.getAttr();
        if (!Strings.isNullOrEmpty(attr)) {
            String[] attrs = attr.split("\\|");
            String valueStr = getTextValue(childNode, attrs);
            if (!Strings.isNullOrEmpty(valueStr)) {
                logger.debug("The raw value of [{}|{}] = [{}]", metricPrefix, attr, valueStr);
                //Apply the converter
                if (metric.hasConverters()) {
                    valueStr = metric.convertValue(attr, valueStr);
                }
                if (NumberUtils.isNumber(valueStr)) {
                    //Apply the multiplier
                    BigDecimal value = new BigDecimal(valueStr);
                    BigDecimal multiplier = metric.getMultiplier();
                    if (multiplier != null) {
                        value = value.multiply(multiplier);
                    }
                    String label = metric.getLabel();
                    if (Strings.isNullOrEmpty(label)) {
                        label = attrs[attrs.length - 1];
                    }
                    reportMetric(metric, metricPrefix, metricType, value, label, aggregatorFactory);
                } else {
                    logger.error("The metric value {} for {}|{} is not a number. Please add a converter if needed", valueStr, metricPrefix, attr);
                }
            } else {
                logger.warn("Cannot get metric {} from the entry {}.", metric.getAttr(), childNode);
            }
        } else {
            logger.error("attr cannot be empty for the metric [{}]", metric.getLabel());
        }


    }

    /**
     * Reports the value. aggregate=true and per-min calculation is done here
     *
     * @param metric
     * @param metricPrefix
     * @param metricType
     * @param value
     * @param label
     * @param aggregatorFactory
     */
    private void reportMetric(Metric metric, String metricPrefix, String metricType, BigDecimal value,
                              String label, AggregatorFactory aggregatorFactory) {
        boolean aggregate = Boolean.TRUE.equals(metric.getAggregate());

        String metricPath = metricPrefix + "|" + label;
        // Direct Metric
        if (!Boolean.TRUE.equals(metric.getShowOnlyPerMin())) {
            printMetrics(metricPath, value, metricType);
            if (aggregate) {
                aggregatorFactory.getAggregator(metricType)
                        .add(new AggregatorKey(label, metricType), value);
            }
        } else {
            logger.debug("Skipping the metric {}|{}, since only perMin is needed", metricPrefix, metric.getAttr());
        }
        // Per minute metrics
        Boolean calculatePerMin = metric.getCalculatePerMin();
        if (Boolean.TRUE.equals(calculatePerMin) || !Strings.isNullOrEmpty(metric.getPerMinLabel())) {
            PerMinValueCalculator perMin = configuration.getPerMinValueCalculator();
            BigDecimal perMinuteValue = perMin.getPerMinuteValue(metricPath, value);
            if (perMinuteValue != null) {
                String perMinLabel = getPerMinLabel(metric, label);
                String metricTypeForPerMin = getMetricTypeForPerMin(metric, metricType);
                printMetrics(metricPrefix + "|" + perMinLabel, perMinuteValue, metricTypeForPerMin);
                if (aggregate) {
                    aggregatorFactory.getAggregator(metricTypeForPerMin)
                            .add(new AggregatorKey(perMinLabel, metricTypeForPerMin), perMinuteValue);
                }
            } else {
                logger.debug("The per minute value for {} hasnt been calculated yet. It will take a couple of executions.", metricPath);
            }
        }
    }

    private String getMetricTypeForPerMin(Metric metric, String metricType) {
        if (StringUtils.hasText(metric.getPerMinMetricType())) {
            return metric.getPerMinMetricType();
        } else {
            return metricType;
        }
    }

    private String getPerMinLabel(Metric metric, String label) {
        if (!Strings.isNullOrEmpty(metric.getPerMinLabel())) {
            return metric.getPerMinLabel();
        } else {
            return label + " Per Minute";
        }
    }

    private void printMetrics(String metricPath, BigDecimal value, String metricType) {
        configuration.getMetricWriter().printMetric(metricPath, value, metricType);
    }

    //Apply the filters
    private boolean isIncluded(String entityName, JsonNode entry, Stat stat) {
        if (Strings.isNullOrEmpty(entityName)) {
            logger.error("Cannot resolve the entity name for {}, excluding the entry with selfLink",
                    getTextValue(entry, "nestedStats", "selfLink"));
            return false;
        } else if (Strings.isNullOrEmpty(stat.getFilterName())) {
            logger.error("The filter-name attribute is null for the stat {}, excluding entry with selfLink {}"
                    , stat.getUrl(), getTextValue(entry, "nestedStats", "selfLink"));
            return false;
        } else {
            Map filter = getFilter(stat);
            if (isIncluded(filter, entityName)) {
                return true;
            } else {
                logger.debug("The filter {} didnt match for entityName {} and url {}"
                        , filter, entityName, stat.getUrl());
                return false;
            }
        }
    }

    /**
     * The filters can be configured at the server level or at a global level.
     * The server level will take precedence
     *
     * @param stat
     * @return
     */
    private Map getFilter(Stat stat) {
        Map filter = (Map) YmlUtils.getNestedObject(server, "filter", stat.getFilterName());
        if (filter == null) {
            filter = (Map) YmlUtils.getNestedObject(configuration.getConfigYml(), "filter", stat.getFilterName());
        }
        return filter;
    }

    //Apply the filter
    private boolean isIncluded(Map filter, String entityName) {
        if (filter != null) {
            List<String> includes = (List) filter.get("includes");
            logger.debug("For the entity name [{}], the includes filter is {}", entityName, includes);
            if (includes != null) {
                for (String include : includes) {
                    boolean matches = entityName.matches(include);
                    if (matches) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Extract the name of the entity based on the <naming> element in <stat>
     *
     * @param field
     * @param entry
     * @param stat
     * @return
     */
    private String getEntityName(String field, JsonNode entry, Stat stat) {
        Naming naming = stat.getNaming();
        if (naming != null) {
            Boolean useEntryName = naming.getUseEntryName();
            String attrs = naming.getAttrs();
            String delimiter = naming.getDelimiter();
            delimiter = Strings.isNullOrEmpty(delimiter) ? ":" : delimiter;
            logger.trace("The useEntryName is [{}], attrs is [{}] and delimiter is [{}]", useEntryName, attrs, delimiter);
            if (Boolean.TRUE.equals(useEntryName) || Strings.isNullOrEmpty(attrs)) {
                return getNameFromUrl(field);
            } else if (!Strings.isNullOrEmpty(attrs)) {
                String[] split = attrs.split(",");
                StringBuilder sb = new StringBuilder();
                for (String attr : split) {
                    String value = getTextValue(entry, attr.split("\\|"));
                    if (value != null) {
                        sb.append(value).append(":");
                    }
                }
                if (sb.length() > 1) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                return sb.toString().replace("/", "~");
            } else {
                return null;
            }
        } else {
            return getNameFromUrl(field);
        }
    }

    private String getEmbeddedTextValue(JsonNode jsonNode) {
        if (jsonNode != null) {
            JsonNode value = jsonNode.get("value");
            if (value != null) {
                return value.toString();
            }
            JsonNode description = jsonNode.get("description");
            if (description != null) {
                return description.getTextValue();
            }
        }
        return null;
    }

    private String getTextValue(JsonNode node, String... nested) {
        JsonNode jsonObject = JsonUtils.getNestedObject(node, nested);
        if (jsonObject != null) {
            if (jsonObject.isValueNode()) {
                if (jsonObject.isTextual()) {
                    return jsonObject.getTextValue();
                } else {
                    return jsonObject.toString();
                }
            } else {
                return getEmbeddedTextValue(jsonObject);
            }
        }
        return null;
    }

    /**
     * The url will look like this https://localhost/mgmt/tm/ltm/pool/~Common~devcontr1/stats
     * The name will be the length - 2 when split on "/"
     *
     * @param url
     * @return
     */
    protected String getNameFromUrl(String url) {
        String name = null;
        try {
            URI uri = new URI(url);
            String fragment = uri.getPath();
            if (fragment != null) {
                String[] split = fragment.split("/");
                if (split.length > 2) {
                    name = split[split.length - 2];
                }
            }
        } catch (URISyntaxException e) {
            String msg = String.format("Error while getting the name from URL %mag", url);
            logger.error(msg, e);
            configuration.getMetricWriter().registerError(msg, e);
        }
        logger.debug("The entity name was resolved to [{}] from the url [{}]", name, url);
        return name;
    }

    /**
     * The endPoint is the url attr of the <stat> element.
     *
     * @param server
     * @param endpoint
     * @param varMap
     * @return
     */
    private String buildUrl(Map server, String endpoint, Map<String, String> varMap) {
        if (varMap != null && !varMap.isEmpty()) {
            logger.debug("The endpoint before replace is {}", endpoint);
            for (String key : varMap.keySet()) {
                endpoint = endpoint.replace(key, varMap.get(key));
            }
            logger.debug("The endpoint after replace is {}", endpoint);
        }
        UrlBuilder builder = UrlBuilder.fromYmlServerConfig(server);
        return builder.path(endpoint).build();
    }
}

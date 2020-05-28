/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.f5;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.f5.config.MetricConfig;
import com.appdynamics.extensions.f5.config.Naming;
import com.appdynamics.extensions.f5.config.Stat;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import static com.appdynamics.extensions.util.AssertUtils.assertNotNull;
import com.appdynamics.extensions.util.JsonUtils;
import com.appdynamics.extensions.util.NumberUtils;
import com.appdynamics.extensions.util.StringUtils;
import com.appdynamics.extensions.util.YmlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Created by abey.tom on 3/11/16.
 */
public class NewF5MonitorTask implements AMonitorTaskRunnable {
    public static final Logger logger = ExtensionsLoggerFactory.getLogger(NewF5MonitorTask.class);

    private Map server;
    private Stat stat;
    private String token;
    private MonitorContextConfiguration configuration;
    private MetricWriteHelper metricWriteHelper;
    private String serverPrefix;
    private Map<String, String> varMap;
    private ObjectMapper objectMapper = new ObjectMapper();

    //This is for main stats
    public NewF5MonitorTask(MonitorContextConfiguration configuration, MetricWriteHelper metricWriteHelper, Map server, Stat stat, String token) {
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
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
    public NewF5MonitorTask(MonitorContextConfiguration configuration, Map server,
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
            logger.error("Error while running the task", e);
        }
    }

    private void runTask() {
        MetricConfig[] metrics = stat.getMetricConfig();
        boolean isFilterConfigured = isFilterConfigured(stat);
        List<Metric> metricsList = Lists.newCopyOnWriteArrayList();
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
                    extractMetrics(childMap, stat, serverPrefix, metricsList);
                    metricWriteHelper.transformAndPrintMetrics(metricsList);
                    logger.info("completed metrics Collection for stats");
                } else {
                    logger.error("The response content for url {} doesn't contain children [{}]", url, stat.getChildren());
                }
            } else {
                logger.error("Error while getting the data from " + url + ". Please check logs for more information.");
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
        return HttpClientUtils.getResponseAsJson(configuration.getContext().getHttpClient(), url, JsonNode.class, headers);
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
                Iterator<String> fields = children.fieldNames();
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
    private void extractMetrics(Map<String, JsonNode> childNodes, Stat stat, String serverPrefix, List<Metric> metricsList) {
        MetricConfig[] metricConfigs = stat.getMetricConfig();
        String statPrefix = serverPrefix + "|" + StringUtils.trim(stat.getLabel(), "|");
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
                for (MetricConfig metricConfig : metricConfigs) {
                    try {
//                        String metricType = getMetricType(stat, metricConfig);
//                        if (metricType != null) {
                            extractAndReport(metricConfig, childNode, entityNamePrefix, metricsList);
//                        } else {
//                            logger.error("Cannot get the metric-type for {}|{}"
//                                    , entityNamePrefix, metricConfig.getAttr());
//                        }
                    } catch (Exception e) {
                        String msg = "Error while getting the metrics for "
                                + entityNamePrefix + "|" + metricConfig.getAttr();
                        logger.error(msg, e);
                    }
                }
                // If there are any child <stat> elements for a parent <stat>
                processChildStats(childNode, stat.getStats(), entityNamePrefix, name, metricsList);
            } else {
                logger.debug("Excluding the entry {} since it didn't match any filter",
                        getTextValue(childNode, "nestedStats", "selfLink"));
            }
        }
    }

    private void processChildStats(JsonNode parentNode, Stat[] childStats, String entryPrefix, String parentName, List<Metric> metricsList) {
        if (childStats != null && childStats.length > 0) {
            for (Stat childStat : childStats) {
                logger.debug("Running the task for the child stat [{}] of parent [{}]", childStat.getUrl(), parentName);
                if (!Strings.isNullOrEmpty(childStat.getUrl())) {
                    Map<String, String> varMap = Collections.singletonMap("$PARENT_NAME", parentName);
                    NewF5MonitorTask task = new NewF5MonitorTask(configuration, server, childStat, entryPrefix, varMap, token);
                    configuration.getContext().getExecutorService().submit("ChildTasks", task);
                } else {
                    //TODO children attribute with $PARENT_NAME replace?
                    // need to clone the stat for that.
                    Map<String, JsonNode> children = getChildren(parentNode, childStat);
                    extractMetrics(children, childStat, entryPrefix, metricsList);
                }
            }
        }
    }

    /**
     * - Aggregation Type
     * - Time Rollup
     * - Cluster Rollup
     *
     * @param stat
     * @param metric
     * @return
     */
//    private String getMetricType(Stat stat, MetricConfig metric) {
//        if (!Strings.isNullOrEmpty(metric.getMetricType())) {
//            return metric.getMetricType();
//        } else if (!Strings.isNullOrEmpty(stat.getMetricType())) {
//            return stat.getMetricType();
//        } else {
//            return null;
//        }
//    }

    /**
     * Extract the value based in <metric> from the childNode.
     *
     * @param metricConfig
     * @param childNode
     * @param metricPrefix
     */
    private void extractAndReport(MetricConfig metricConfig, JsonNode childNode, String metricPrefix, List<Metric> metricsList) {
        String attr = metricConfig.getAttr();
        if (!Strings.isNullOrEmpty(attr)) {
            String[] attrs = attr.split("\\|");
            String valueStr = getTextValue(childNode, attrs);
            if (!Strings.isNullOrEmpty(valueStr)) {
                logger.debug("The raw value of [{}|{}] = [{}]", metricPrefix, attr, valueStr);
                if (NumberUtils.isNumber(valueStr)) {
                    BigDecimal value = new BigDecimal(valueStr);
                    String label = metricConfig.getAlias();
                    if (Strings.isNullOrEmpty(label)) {
                        label = attrs[attrs.length - 1];
                    }
                    reportMetric(metricConfig, metricPrefix, value, label, metricsList);
                } else {
                    logger.error("The metric value {} for {}|{} is not a number. Please add a converter if needed", valueStr, metricPrefix, attr);
                }
            } else {
                logger.warn("Cannot get metric {} from the entry {}.", metricConfig.getAttr(), childNode);
            }
        } else {
            logger.error("attr cannot be empty for the metric [{}]", metricConfig.getAlias());
        }


    }

    /**
     * Reports the value. aggregate=true and per-min calculation is done here
     *
     * @param metricConfig
     * @param metricPrefix
     * @param value
     * @param label
     */
    private void reportMetric(MetricConfig metricConfig, String metricPrefix, BigDecimal value, String label, List<Metric> metricList) {
        String metricPath = metricPrefix + "|" + label;
        Metric metric = new Metric(metricConfig.getAttr(), value.toString(), metricPath, objectMapper.convertValue(metricConfig, Map.class));
        metricList.add(metric);
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
            logger.trace("The useEntryName is [{}], attrs is [{}] and delimiter is [{}]", useEntryName, attrs);
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
                return description.textValue();
            }
        }
        return null;
    }

    private String getTextValue(JsonNode node, String... nested) {
        JsonNode jsonObject = JsonUtils.getNestedObject(node, nested);
        if (jsonObject != null) {
            if (jsonObject.isValueNode()) {
                if (jsonObject.isTextual()) {
                    return jsonObject.textValue();
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

    @Override
    public void onTaskComplete() {

    }
}

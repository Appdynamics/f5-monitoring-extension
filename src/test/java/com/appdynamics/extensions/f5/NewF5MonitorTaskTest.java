/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.f5;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.f5.config.Stat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Created by abey.tom on 3/14/16.
 */
public class NewF5MonitorTaskTest {

    @Mock
    AMonitorJob aMonitorJob;

    @Test
    public void nameFromUrlTest() {
        NewF5MonitorTask task = new NewF5MonitorTask(null, null, null, null, null, null);
        String nameFromUrl = task.getNameFromUrl("https://localhost/mgmt/tm/ltm/virtual/~Common~Outbound_Forwarding/stats?a=b&c=d");
        Assert.assertEquals("~Common~Outbound_Forwarding", nameFromUrl);

    }

    @Test
    public void run() {
        MetricWriteHelper writer = Mockito.mock(MetricWriteHelper.class);
        MonitorContextConfiguration conf = new MonitorContextConfiguration("F5Monitor","Custom Metrics|F5 Monitor|", new File("demo"), aMonitorJob);
        conf.setConfigYml("src/test/resources/conf/test-config.yml");
        conf.setMetricXml("src/test/resources/metrics/metrics-with-children.xml", Stat.Stats.class);
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                System.out.println(args[0] + "=" + args[1]);
                return null;
            }
        }).when(writer).printMetric(Mockito.anyString(), Mockito.any(BigDecimal.class), Mockito.anyString());
        Stat.Stats wrapper = (Stat.Stats) conf.getMetricsXml();
        Stat[] stats = wrapper.getStats();
        List servers = (List) conf.getConfigYml().get("servers");
        for (Stat stat : stats) {
            NewF5MonitorTask task = new NewF5MonitorTask(conf, writer, (Map) servers.get(0), stat, null);
            task = Mockito.spy(task);
            spyForJson(task);
            task.run();
        }
    }

    private void spyForJson(NewF5MonitorTask task) {
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String url = (String) invocationOnMock.getArguments()[0];
                System.out.println(url);
                String file = "";
                if (url.contains("logical-disk")) {
                    file = "logical-disk.json";
                } else if (url.contains("interface")) {
                    file = "interface.json";
                } else if (url.contains("hostInfo")) {
                    file = "hostInfo.json";
                }
                String name = "src/test/resources/output/" + file;
                return new ObjectMapper().readValue(new FileInputStream(name), JsonNode.class);
            }
        }).when(task).getResponseAsJson(Mockito.anyString());
    }

    @Test
    public void metricsWithChildrenArray() throws IOException {
        MetricWriteHelper writer = Mockito.mock(MetricWriteHelper.class);
        Runnable runner = Mockito.mock(Runnable.class);
        MonitorContextConfiguration conf = new MonitorContextConfiguration("F5Monitor","Custom Metrics|F5 Monitor|", new File("demo"), aMonitorJob);
        conf.setConfigYml("src/test/resources/conf/test-config.yml");
        conf.setMetricXml("src/test/resources/metrics/metrics-logical-disk.xml", Stat.Stats.class);
        Stat.Stats wrapper = (Stat.Stats) conf.getMetricsXml();
        Stat[] stats = wrapper.getStats();
        List servers = (List) conf.getConfigYml().get("servers");
        NewF5MonitorTask task = new NewF5MonitorTask(conf, writer, (Map) servers.get(0), stats[0], null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readValue(new FileInputStream("src/test/resources/output/logical-disk.json"), JsonNode.class);
        Map<String, JsonNode> children = task.getChildren(jsonNode, stats[0]);
        Assert.assertEquals(2, children.size());
        Assert.assertNotNull(children.get("HD1"));
        Assert.assertNotNull(children.get("CF1"));
    }

    @Test
    public void metricsWithChildrenMap() throws IOException {
        MetricWriteHelper writer = Mockito.mock(MetricWriteHelper.class);
        Runnable runner = Mockito.mock(Runnable.class);
        MonitorContextConfiguration conf = new MonitorContextConfiguration("F5Monitor","Custom Metrics|F5 Monitor|", new File("demo"), aMonitorJob);
        conf.setConfigYml("src/test/resources/conf/test-config.yml");
        conf.setMetricXml("src/test/resources/metrics/metrics-hostInfo.xml", Stat.Stats.class);
        Stat.Stats wrapper = (Stat.Stats) conf.getMetricsXml();
        Stat[] stats = wrapper.getStats();
        List servers = (List) conf.getConfigYml().get("servers");
        NewF5MonitorTask task = new NewF5MonitorTask(conf, writer, (Map) servers.get(0), stats[0], null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readValue(new FileInputStream("src/test/resources/output/hostInfo.json"), JsonNode.class);
        Map<String, JsonNode> children = task.getChildren(jsonNode, stats[0]);
        Assert.assertEquals(1, children.size());
        Assert.assertNotNull(children.get("0"));
    }

    @Test
    public void getJsonObjectWithWildcard() {

    }
}
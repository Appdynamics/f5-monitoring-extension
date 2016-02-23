package com.appdynamics.extensions.f5.collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appdynamics.extensions.f5.F5Constants;
import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.appdynamics.extensions.f5.http.HttpExecutor;
import com.appdynamics.extensions.f5.models.StatEntry;
import com.appdynamics.extensions.f5.models.Stats;
import com.appdynamics.extensions.f5.responseProcessor.KeyField;
import com.appdynamics.extensions.f5.responseProcessor.PoolResponseProcessor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpExecutor.class, EntityUtils.class, PoolResponseProcessor.class})
public class VirtualServerMetricsCollectorTest {

    private VirtualServerMetricsCollector classUnderTest;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private HttpClientContext httpContext;

    @Mock
    private F5 mockF5;

    @Mock
    private MetricsFilter mockMetricsFilter;

    @Mock
    private F5Monitor monitor;

    @Mock
    private MetricWriter metricWriter;

    private String metricPrefix = F5Constants.DEFAULT_METRIC_PATH;

    @Before
    public void setUp() throws Exception {
        when(mockF5.getDisplayName()).thenReturn("TestF5");

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        PowerMockito.mockStatic(HttpExecutor.class, EntityUtils.class, PoolResponseProcessor.class);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        BDDMockito.given(HttpExecutor.execute(eq(httpClient), any(HttpUriRequest.class), eq(httpContext))).willReturn(response);
        BDDMockito.given(EntityUtils.toString(any(HttpEntity.class))).willReturn("hello");

        Stats stats = getTestStatistics();

        BDDMockito.given(PoolResponseProcessor.processPoolStatsResponse(eq("hello"), any(Pattern.class), any(KeyField.class))).willReturn(stats);

        when(monitor.getMetricWriter(anyString(), anyString(), anyString(), anyString())).thenReturn(metricWriter);

    }

    @Test
    public void testNoVirtualServersIncluded() throws Exception {
        classUnderTest = new VirtualServerMetricsCollector(httpClient,
                httpContext, mockF5, mockMetricsFilter, monitor, metricPrefix);

        classUnderTest.call();
        verify(metricWriter, never()).printMetric(anyString());
    }

    @Test
    public void testIncludeAllVirtualServers() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getVirtualServerIncludes()).thenReturn(testIncludes);


        classUnderTest = new VirtualServerMetricsCollector(httpClient, httpContext, mockF5, mockMetricsFilter, monitor, metricPrefix);
        classUnderTest.call();

        verify(metricWriter, times(6)).printMetric(anyString());

        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP|clientside.bitsIn"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP|clientside.bitsOut"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP|clientside.maxConns"), anyString(), anyString(), anyString());

        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|clientside.bitsIn"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|clientside.bitsOut"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|clientside.maxConns"), anyString(), anyString(), anyString());
    }

    @Test
    public void testExcludeMetrics() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getVirtualServerIncludes()).thenReturn(testIncludes);

        Set<String> testMetricExcludes = new HashSet<String>();
        testMetricExcludes.add(".*bitsOut.*");
        when(mockMetricsFilter.getVirtualServerMetricExcludes()).thenReturn(testMetricExcludes);


        classUnderTest = new VirtualServerMetricsCollector(httpClient, httpContext, mockF5, mockMetricsFilter, monitor, metricPrefix);
        classUnderTest.call();

        verify(metricWriter, times(4)).printMetric(anyString());

        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP|clientside.bitsIn"), anyString(), anyString(), anyString());
        verify(monitor, never()).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP|clientside.bitsOut"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP|clientside.maxConns"), anyString(), anyString(), anyString());

        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|clientside.bitsIn"), anyString(), anyString(), anyString());
        verify(monitor, never()).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|clientside.bitsOut"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|clientside.maxConns"), anyString(), anyString(), anyString());
    }

    private Stats getTestStatistics() {
        Stats stats = new Stats();

        StatEntry statEntry = new StatEntry();
        statEntry.setName("clientside.bitsIn");
        statEntry.setValue("5000");
        statEntry.setType(StatEntry.Type.NUMERIC);
        stats.addStat("/Common/devcontr7_VIP", statEntry);
        stats.addStat("/Common/devcontr7_VIP_SSL", statEntry);

        StatEntry statEntry1 = new StatEntry();
        statEntry1.setName("clientside.bitsOut");
        statEntry1.setValue("5000");
        statEntry1.setType(StatEntry.Type.NUMERIC);
        stats.addStat("/Common/devcontr7_VIP", statEntry1);
        stats.addStat("/Common/devcontr7_VIP_SSL", statEntry1);

        StatEntry statEntry2 = new StatEntry();
        statEntry2.setName("clientside.maxConns");
        statEntry2.setValue("5000");
        statEntry2.setType(StatEntry.Type.NUMERIC);
        stats.addStat("/Common/devcontr7_VIP", statEntry2);
        stats.addStat("/Common/devcontr7_VIP_SSL", statEntry2);


        return stats;
    }
}

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
import com.appdynamics.extensions.f5.responseProcessor.ResponseProcessor;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpExecutor.class, EntityUtils.class, ResponseProcessor.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*"})
public class PoolMetricsCollectorTest {

    private PoolMetricsCollector classUnderTest;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private HttpClientContext httpContext;

    @Mock
    private PoolMemberMetricsCollector mockPoolMemberMetricsCollector;

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
        PowerMockito.mockStatic(HttpExecutor.class, EntityUtils.class, ResponseProcessor.class);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        BDDMockito.given(EntityUtils.toString(any(HttpEntity.class))).willReturn("hello");
        BDDMockito.given(HttpExecutor.execute(eq(httpClient), any(HttpUriRequest.class), eq(httpContext))).willReturn("hello");

        Stats stats = getTestStatistics();

        BDDMockito.given(ResponseProcessor.processStatsResponse(eq("hello"), any(Pattern.class), any(KeyField.class))).willReturn(stats);

        when(monitor.getMetricWriter(anyString(), anyString(), anyString(), anyString())).thenReturn(metricWriter);

    }

    @Test
    public void testNoPoolsIncluded() throws Exception {
        classUnderTest = new PoolMetricsCollector(httpClient, httpContext,
                mockF5, mockMetricsFilter, monitor, metricPrefix);

        classUnderTest.call();
        verify(metricWriter, never()).printMetric(anyString());
    }

    @Test
    public void testIncludeAllPools() throws Exception {
        when(mockF5.isPreVersion11()).thenReturn(false);
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getPoolIncludes()).thenReturn(testIncludes);

        classUnderTest = new PoolMetricsCollector(httpClient, httpContext, mockF5, mockMetricsFilter, monitor, metricPrefix);
        classUnderTest.call();

        verify(metricWriter, times(6)).printMetric(anyString());

        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr7|serverside.bitsIn"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr8|serverside.bitsIn"), anyString(), anyString(), anyString());

        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr7|serverside.bitsOut"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr8|serverside.bitsOut"), anyString(), anyString(), anyString());

        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr7|STATUS"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr8|STATUS"), anyString(), anyString(), anyString());


    }

    @Test
    public void testExcludeMetrics() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getPoolIncludes()).thenReturn(testIncludes);

        Set<String> testMetricExcludes = new HashSet<String>();
        testMetricExcludes.add(".*bitsOut.*");
        when(mockMetricsFilter.getPoolMetricExcludes()).thenReturn(testMetricExcludes);


        classUnderTest = new PoolMetricsCollector(httpClient, httpContext, mockF5, mockMetricsFilter, monitor, metricPrefix);
        classUnderTest.call();


        verify(metricWriter, times(4)).printMetric(anyString());

        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr7|serverside.bitsIn"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr8|serverside.bitsIn"), anyString(), anyString(), anyString());

        verify(monitor, never()).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr7|serverside.bitsOut"), anyString(), anyString(), anyString());
        verify(monitor, never()).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr8|serverside.bitsOut"), anyString(), anyString(), anyString());

        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr7|STATUS"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Pools|Common|devcontr8|STATUS"), anyString(), anyString(), anyString());
    }

    private Stats getTestStatistics() {
        Stats stats = new Stats();


        StatEntry statEntry = new StatEntry();
        statEntry.setName("serverside.bitsIn");
        statEntry.setValue("2000");
        statEntry.setType(StatEntry.Type.NUMERIC);
        stats.addStat("/Common/devcontr7", statEntry);
        stats.addStat("/Common/devcontr8", statEntry);


        StatEntry statEntry1 = new StatEntry();
        statEntry1.setName("serverside.bitsOut");
        statEntry1.setValue("5300");
        statEntry1.setType(StatEntry.Type.NUMERIC);
        stats.addStat("/Common/devcontr7", statEntry1);
        stats.addStat("/Common/devcontr8", statEntry1);

        return stats;
    }
}

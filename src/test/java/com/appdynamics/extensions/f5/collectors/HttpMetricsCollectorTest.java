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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpExecutor.class, EntityUtils.class, PoolResponseProcessor.class})
public class HttpMetricsCollectorTest {

    private HttpMetricsCollector classUnderTest;

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

        Map<String, BigInteger> poolStats = getTestStatistics();

        BDDMockito.given(PoolResponseProcessor.aggregateStatsResponse(eq("hello"))).willReturn(poolStats);

        when(monitor.getMetricWriter(anyString(), anyString(), anyString(), anyString())).thenReturn(metricWriter);

    }

    @Test
    public void testAllMetricsIncluded() throws Exception {
        classUnderTest = new HttpMetricsCollector(
                httpClient, httpContext, mockF5, mockMetricsFilter, monitor, metricPrefix);

        classUnderTest.call();

        verify(metricWriter, times(3)).printMetric(anyString());

        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Network|HTTP|resp_2xxCnt"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Network|HTTP|resp_3xxCnt"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Network|HTTP|resp_4xxCnt"), anyString(), anyString(), anyString());

    }

    @Test
    public void testExcludeMetrics() throws Exception {
        Set<String> testMetricExcludes = new HashSet<String>();
        testMetricExcludes.add(".*2xx.*");
        when(mockMetricsFilter.getHttpMetricExcludes()).thenReturn(testMetricExcludes);

        classUnderTest = new HttpMetricsCollector(
                httpClient, httpContext, mockF5, mockMetricsFilter, monitor, metricPrefix);

        classUnderTest.call();

        verify(metricWriter, times(2)).printMetric(anyString());

        verify(monitor, never()).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Network|HTTP|resp_2xxCnt"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Network|HTTP|resp_3xxCnt"), anyString(), anyString(), anyString());
        verify(monitor, times(1)).getMetricWriter(eq("Custom Metrics|F5 Monitor|TestF5|Network|HTTP|resp_4xxCnt"), anyString(), anyString(), anyString());
    }

    private Map<String, BigInteger> getTestStatistics() {

        Map<String, BigInteger> stats = new HashMap<String, BigInteger>();
        stats.put("resp_2xxCnt", BigInteger.valueOf(1234));
        stats.put("resp_3xxCnt", BigInteger.valueOf(500));
        stats.put("resp_4xxCnt", BigInteger.valueOf(100));

        return stats;
    }

}

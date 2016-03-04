package com.appdynamics.extensions.f5.collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.appdynamics.extensions.f5.http.HttpExecutor;
import com.appdynamics.extensions.f5.models.StatEntry;
import com.appdynamics.extensions.f5.models.Stats;
import com.appdynamics.extensions.f5.responseProcessor.KeyField;
import com.appdynamics.extensions.f5.responseProcessor.ResponseProcessor;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpExecutor.class, EntityUtils.class, ResponseProcessor.class})
public class PoolMemberMetricsCollectorTest {

    private PoolMemberMetricsCollector classUnderTest;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private HttpClientContext httpContext;

    @Mock
    private F5 mockF5;

    @Mock
    private MetricsFilter mockMetricsFilter;


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
    }

    @Test
    public void testNoPoolMembersIncluded() throws Exception {
        classUnderTest = new PoolMemberMetricsCollector(mockF5.getPoolMemberIncludes(),
                mockMetricsFilter.getPoolMetricExcludes(), mockF5, httpClient, httpContext);

        String poolMetricPrefix = "TestF5|Pools";

        Map<String, BigInteger> statsMap = classUnderTest.collectMemberMetrics(poolMetricPrefix, "Common/devcontr7");
        assertEquals(null, statsMap);
    }

    @Test
    public void testAllPoolMembersIncluded() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getPoolMemberIncludes()).thenReturn(testIncludes);

        classUnderTest = new PoolMemberMetricsCollector(mockF5.getPoolMemberIncludes(),
                mockMetricsFilter.getPoolMetricExcludes(), mockF5, httpClient, httpContext);

        String poolMetricPrefix = "TestF5|Pools";

        Map<String, BigInteger> result = classUnderTest.collectMemberMetrics(poolMetricPrefix, "Common/devcontr7");
        assertEquals(6, result.size());

        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member1|8080|serverside.bitsIn"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member1|8080|serverside.bitsOut"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member1|8080|STATUS"));

        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member2|8090|serverside.bitsIn"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member2|8090|serverside.bitsOut"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member2|8090|STATUS"));


    }

    @Test
    public void testExcludeMetrics() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getPoolMemberIncludes()).thenReturn(testIncludes);

        Set<String> testMetricExcludes = new HashSet<String>();
        testMetricExcludes.add(".*bitsOut.*");
        when(mockMetricsFilter.getPoolMetricExcludes()).thenReturn(testMetricExcludes);

        classUnderTest = new PoolMemberMetricsCollector(mockF5.getPoolMemberIncludes(),
                mockMetricsFilter.getPoolMetricExcludes(), mockF5, httpClient, httpContext);

        String poolMetricPrefix = "TestF5|Pools";

        Map<String, BigInteger> result = classUnderTest.collectMemberMetrics(poolMetricPrefix, "Common/devcontr7");
        assertEquals(4, result.size());

        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member1|8080|serverside.bitsIn"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member2|8090|serverside.bitsIn"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member1|8080|STATUS"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member2|8090|STATUS"));


        assertTrue(!result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member2|8090|serverside.bitsOut"));
        assertTrue(!result.containsKey("TestF5|Pools|Common|devcontr7|Members|devcontr7_member1|8080|serverside.bitsOut"));
    }

    private Stats getTestStatistics() {

        Stats poolMemberStats = new Stats();

        StatEntry statEntry = new StatEntry();
        statEntry.setName("serverside.bitsIn");
        statEntry.setValue("2000");
        statEntry.setType(StatEntry.Type.NUMERIC);
        poolMemberStats.addStat("devcontr7_member1|8080", statEntry);
        poolMemberStats.addStat("devcontr7_member2|8090", statEntry);


        StatEntry statEntry1 = new StatEntry();
        statEntry1.setName("serverside.bitsOut");
        statEntry1.setValue("5300");
        statEntry1.setType(StatEntry.Type.NUMERIC);
        poolMemberStats.addStat("devcontr7_member1|8080", statEntry1);
        poolMemberStats.addStat("devcontr7_member2|8090", statEntry1);


        return poolMemberStats;
    }


}

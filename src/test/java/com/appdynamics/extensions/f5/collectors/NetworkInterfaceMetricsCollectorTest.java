package com.appdynamics.extensions.f5.collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appdynamics.extensions.f5.F5Constants;
import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import iControl.CommonStatistic;
import iControl.CommonStatisticType;
import iControl.CommonULong64;
import iControl.Interfaces;
import iControl.NetworkingInterfacesBindingStub;
import iControl.NetworkingInterfacesInterfaceStatisticEntry;
import iControl.NetworkingInterfacesInterfaceStatistics;
import iControl.NetworkingMediaStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class NetworkInterfaceMetricsCollectorTest {

    private NetworkInterfaceMetricsCollector classUnderTest;

    @Mock
    private Interfaces mockIcontrolInterfaces;

    @Mock
    private NetworkingInterfacesBindingStub mockNetworkInterfaceSub;

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
        when(mockIcontrolInterfaces.getNetworkingInterfaces()).thenReturn(mockNetworkInterfaceSub);
        when(monitor.getMetricWriter(anyString(), anyString(), anyString(), anyString())).thenReturn(metricWriter);

    }

    @Test
    public void testNoInterfacesIncluded() throws Exception {
        classUnderTest = new NetworkInterfaceMetricsCollector(mockIcontrolInterfaces,
                mockF5, mockMetricsFilter, monitor, metricPrefix);

        classUnderTest.call();
        //assertEquals(0, result.getMetrics().size());
        verify(metricWriter, never()).printMetric(anyString());
    }

    @Test
    public void testIncludeAllNetworkInterfaces() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getNetworkInterfaceIncludes()).thenReturn(testIncludes);

        String[] testProfiles = getTestInterfaces();
        when(mockNetworkInterfaceSub.get_list()).thenReturn(testProfiles);

        NetworkingInterfacesInterfaceStatistics testStats = getTestStatistics();
        when(mockNetworkInterfaceSub.get_statistics(any(String[].class))).thenReturn(testStats);

        NetworkingMediaStatus[] testStatuses = getTestNetworkMediaStatus();
        when(mockNetworkInterfaceSub.get_media_status(any(String[].class))).thenReturn(testStatuses);

        classUnderTest = new NetworkInterfaceMetricsCollector(mockIcontrolInterfaces,
                mockF5, mockMetricsFilter, monitor, metricPrefix);
        classUnderTest.call();

        verify(metricWriter, times(12)).printMetric(anyString());

		/*assertEquals(12, result.getMetrics().size());

		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.1|STATISTIC_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.1|STATISTIC_BYTES_OUT"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.1|STATISTIC_COLLISIONS"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.1|STATUS"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.2|STATISTIC_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.2|STATISTIC_BYTES_OUT"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.2|STATISTIC_COLLISIONS"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.2|STATUS"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.3|STATISTIC_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.3|STATISTIC_BYTES_OUT"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.3|STATISTIC_COLLISIONS"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.2|STATUS"));*/
    }

    @Test
    public void testExcludeMetrics() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getNetworkInterfaceIncludes()).thenReturn(testIncludes);

        Set<String> testMetricExcludes = new HashSet<String>();
        testMetricExcludes.add("STATUS");
        testMetricExcludes.add("STATISTIC_COLLISIONS");
        when(mockMetricsFilter.getNetworkInterfaceMetricExcludes()).thenReturn(testMetricExcludes);

        String[] testProfiles = getTestInterfaces();
        when(mockNetworkInterfaceSub.get_list()).thenReturn(testProfiles);

        NetworkingInterfacesInterfaceStatistics testStats = getTestStatistics();
        when(mockNetworkInterfaceSub.get_statistics(any(String[].class))).thenReturn(testStats);

        NetworkingMediaStatus[] testStatuses = getTestNetworkMediaStatus();
        when(mockNetworkInterfaceSub.get_media_status(any(String[].class))).thenReturn(testStatuses);

        classUnderTest = new NetworkInterfaceMetricsCollector(mockIcontrolInterfaces,
                mockF5, mockMetricsFilter, monitor, metricPrefix);
        classUnderTest.call();

        verify(metricWriter, times(6)).printMetric(anyString());

		/*assertEquals(6, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.1|STATISTIC_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.1|STATISTIC_BYTES_OUT"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.2|STATISTIC_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.2|STATISTIC_BYTES_OUT"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.3|STATISTIC_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.3|STATISTIC_BYTES_OUT"));
		
		// excluded
		assertFalse(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.1|STATISTIC_COLLISIONS"));
		assertFalse(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.1|STATUS"));
		assertFalse(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.2|STATISTIC_COLLISIONS"));
		assertFalse(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.2|STATUS"));
		assertFalse(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.3|STATISTIC_COLLISIONS"));
		assertFalse(result.getMetrics().containsKey("TestF5|Network|Interfaces|1.2|STATUS"));*/
    }

    private NetworkingInterfacesInterfaceStatistics getTestStatistics() {
        String[] testInterfaces = getTestInterfaces();
        NetworkingInterfacesInterfaceStatistics stats =
                new NetworkingInterfacesInterfaceStatistics();

        NetworkingInterfacesInterfaceStatisticEntry[] entries =
                new NetworkingInterfacesInterfaceStatisticEntry[testInterfaces.length];
        stats.setStatistics(entries);

        CommonStatisticType[] statisticTypes = getTestStatisticTypes();

        for (int interfaceIndex = 0; interfaceIndex < testInterfaces.length; interfaceIndex++) {
            NetworkingInterfacesInterfaceStatisticEntry entry =
                    new NetworkingInterfacesInterfaceStatisticEntry();
            entry.setInterface_name(testInterfaces[interfaceIndex]);
            entries[interfaceIndex] = entry;

            CommonStatistic[] commonStats = new CommonStatistic[statisticTypes.length];

            for (int index = 0; index < statisticTypes.length; index++) {
                CommonStatistic commonStat = new CommonStatistic();
                commonStat.setType(statisticTypes[index]);
                commonStat.setValue(getTestValue());
                commonStats[index] = commonStat;
            }

            entry.setStatistics(commonStats);
        }

        return stats;
    }

    private String[] getTestInterfaces() {
        String[] testInterfaces = {"1.1", "1.2", "1.3"};
        return testInterfaces;
    }

    private CommonStatisticType[] getTestStatisticTypes() {
        CommonStatisticType[] testMetricTypes = {
                CommonStatisticType.STATISTIC_BYTES_IN,
                CommonStatisticType.STATISTIC_BYTES_OUT,
                CommonStatisticType.STATISTIC_COLLISIONS};

        return testMetricTypes;
    }

    private NetworkingMediaStatus[] getTestNetworkMediaStatus() {
        return new NetworkingMediaStatus[]{
                NetworkingMediaStatus.MEDIA_STATUS_UP,
                NetworkingMediaStatus.MEDIA_STATUS_UP,
                NetworkingMediaStatus.MEDIA_STATUS_DOWN
        };

    }

    private CommonULong64 getTestValue() {
        CommonULong64 testValue = new CommonULong64();
        testValue.setHigh(new Random(100).nextLong());
        testValue.setHigh(new Random(10).nextLong());
        return testValue;
    }
}

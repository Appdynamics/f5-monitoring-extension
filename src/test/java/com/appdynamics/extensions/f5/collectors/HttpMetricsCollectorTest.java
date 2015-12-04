package com.appdynamics.extensions.f5.collectors;

import static org.mockito.Matchers.anyString;
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
import iControl.SystemStatisticsBindingStub;
import iControl.SystemStatisticsSystemStatistics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class HttpMetricsCollectorTest {

    private HttpMetricsCollector classUnderTest;

    @Mock
    private Interfaces mockIcontrolInterfaces;

    @Mock
    private F5 mockF5;

    @Mock
    private MetricsFilter mockMetricsFilter;

    @Mock
    private SystemStatisticsBindingStub mockSystemStatsStub;

    @Mock
    private F5Monitor monitor;

    @Mock
    private MetricWriter metricWriter;

    private String metricPrefix = F5Constants.DEFAULT_METRIC_PATH;

    @Before
    public void setUp() throws Exception {
        when(mockF5.getDisplayName()).thenReturn("TestF5");
        SystemStatisticsSystemStatistics testStats = getTestStatistics();
        when(mockSystemStatsStub.get_http_statistics()).thenReturn(testStats);
        when(mockIcontrolInterfaces.getSystemStatistics()).thenReturn(mockSystemStatsStub);
        when(monitor.getMetricWriter(anyString(), anyString(), anyString(), anyString())).thenReturn(metricWriter);

    }

    @Test
    public void testAllMetricsIncluded() throws Exception {
        classUnderTest = new HttpMetricsCollector(
                mockIcontrolInterfaces, mockF5, mockMetricsFilter, monitor, metricPrefix);

        classUnderTest.call();

        verify(metricWriter, times(3)).printMetric(anyString());

		/*assertEquals(3, result.getMetrics().size());

		assertTrue(result.getMetrics().containsKey("TestF5|Network|HTTP|STATISTIC_HTTP_2XX_RESPONSES"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|HTTP|STATISTIC_HTTP_3XX_RESPONSES"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|HTTP|STATISTIC_HTTP_4XX_RESPONSES"));*/
    }

    @Test
    public void testExcludeMetrics() throws Exception {
        Set<String> testMetricExcludes = new HashSet<String>();
        testMetricExcludes.add(".*2XX.*");
        when(mockMetricsFilter.getHttpMetricExcludes()).thenReturn(testMetricExcludes);

        classUnderTest = new HttpMetricsCollector(
                mockIcontrolInterfaces, mockF5, mockMetricsFilter, monitor, metricPrefix);

        classUnderTest.call();

        verify(metricWriter, times(2)).printMetric(anyString());

		/*assertEquals(2, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|Network|HTTP|STATISTIC_HTTP_3XX_RESPONSES"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|HTTP|STATISTIC_HTTP_4XX_RESPONSES"));
		
		//excluded
		assertFalse(result.getMetrics().containsKey("TestF5|Network|HTTP|STATISTIC_HTTP_2XX_RESPONSES"));*/
    }

    private SystemStatisticsSystemStatistics getTestStatistics() {
        SystemStatisticsSystemStatistics stats = new SystemStatisticsSystemStatistics();

        CommonStatisticType[] statisticTypes = getTestStatisticTypes();
        CommonStatistic[] commonStats = new CommonStatistic[statisticTypes.length];

        for (int index = 0; index < statisticTypes.length; index++) {
            CommonStatistic commonStat = new CommonStatistic();
            commonStat.setType(statisticTypes[index]);
            commonStat.setValue(getTestValue());
            commonStats[index] = commonStat;
        }

        stats.setStatistics(commonStats);
        return stats;
    }

    private CommonStatisticType[] getTestStatisticTypes() {
        CommonStatisticType[] testMetricTypes = {
                CommonStatisticType.STATISTIC_HTTP_2XX_RESPONSES,
                CommonStatisticType.STATISTIC_HTTP_3XX_RESPONSES,
                CommonStatisticType.STATISTIC_HTTP_4XX_RESPONSES};

        return testMetricTypes;
    }

    private CommonULong64 getTestValue() {
        CommonULong64 testValue = new CommonULong64();
        testValue.setHigh(new Random(100).nextLong());
        testValue.setHigh(new Random(10).nextLong());
        return testValue;
    }

}

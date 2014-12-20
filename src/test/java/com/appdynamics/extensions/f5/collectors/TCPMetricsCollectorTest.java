package com.appdynamics.extensions.f5.collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import iControl.CommonStatistic;
import iControl.CommonStatisticType;
import iControl.CommonULong64;
import iControl.Interfaces;
import iControl.SystemStatisticsBindingStub;
import iControl.SystemStatisticsSystemStatistics;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.appdynamics.extensions.f5.F5Metrics;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;

@RunWith(MockitoJUnitRunner.class)
public class TCPMetricsCollectorTest {
	
	private TCPMetricsCollector classUnderTest;
	
	@Mock
	private Interfaces mockIcontrolInterfaces;
	
	@Mock
	private F5 mockF5;
	
	@Mock
	private MetricsFilter mockMetricsFilter;
	
	@Mock
	private SystemStatisticsBindingStub mockSystemStatsStub;
	
	@Before
	public void setUp() throws Exception {
		when(mockF5.getDisplayName()).thenReturn("TestF5");
		SystemStatisticsSystemStatistics testStats = getTestStatistics();
		when(mockSystemStatsStub.get_tcp_statistics()).thenReturn(testStats);
		when(mockIcontrolInterfaces.getSystemStatistics()).thenReturn(mockSystemStatsStub);
	}
	
	@Test
	public void testAllMetricsIncluded() throws Exception {
		classUnderTest = new TCPMetricsCollector(
				mockIcontrolInterfaces, mockF5, mockMetricsFilter);
		
		F5Metrics result = classUnderTest.call();
		assertEquals(3, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|Network|TCP|STATISTIC_TCP_ABANDONED_CONNECTIONS"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|TCP|STATISTIC_TCP_CLOSE_WAIT_CONNECTIONS"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|TCP|STATISTIC_TCP_ACCEPTED_CONNECTIONS"));
	}
	
	@Test
	public void testExcludeMetrics() throws Exception {
		Set<String> testMetricExcludes = new HashSet<String>();
		testMetricExcludes.add(".*ABANDONED.*");
		when(mockMetricsFilter.getTcpMetricExcludes()).thenReturn(testMetricExcludes);
		
		classUnderTest = new TCPMetricsCollector(
				mockIcontrolInterfaces, mockF5, mockMetricsFilter);
		
		F5Metrics result = classUnderTest.call();
		assertEquals(2, result.getMetrics().size());
		assertTrue(result.getMetrics().containsKey("TestF5|Network|TCP|STATISTIC_TCP_CLOSE_WAIT_CONNECTIONS"));
		assertTrue(result.getMetrics().containsKey("TestF5|Network|TCP|STATISTIC_TCP_ACCEPTED_CONNECTIONS"));
		
		// excluded
		assertFalse(result.getMetrics().containsKey("TestF5|Network|TCP|STATISTIC_TCP_ABANDONED_CONNECTIONS"));
	}
	
	private SystemStatisticsSystemStatistics getTestStatistics() {
		SystemStatisticsSystemStatistics stats = new SystemStatisticsSystemStatistics();
		
		CommonStatisticType[] statisticTypes = getTestStatisticTypes();
		CommonStatistic[] commonStats = new CommonStatistic[statisticTypes.length];
		
		for (int index=0; index < statisticTypes.length; index++) {
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
				CommonStatisticType.STATISTIC_TCP_ABANDONED_CONNECTIONS,
				CommonStatisticType.STATISTIC_TCP_CLOSE_WAIT_CONNECTIONS,
				CommonStatisticType.STATISTIC_TCP_ACCEPTED_CONNECTIONS };
		
		return testMetricTypes;
	}
	
	private CommonULong64 getTestValue() {
		CommonULong64 testValue = new CommonULong64();
		testValue.setHigh(new Random(100).nextLong());
		testValue.setHigh(new Random(10).nextLong());
		return testValue;
	}

}

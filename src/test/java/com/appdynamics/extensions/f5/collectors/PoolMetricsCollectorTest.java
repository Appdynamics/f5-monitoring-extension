package com.appdynamics.extensions.f5.collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import iControl.CommonStatistic;
import iControl.CommonStatisticType;
import iControl.CommonULong64;
import iControl.Interfaces;
import iControl.LocalLBPoolBindingStub;
import iControl.LocalLBPoolPoolStatisticEntry;
import iControl.LocalLBPoolPoolStatistics;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.appdynamics.extensions.f5.F5Metrics;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PoolMetricsCollector.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*"})
public class PoolMetricsCollectorTest {

	private PoolMetricsCollector classUnderTest;
	
	@Mock
	private Interfaces mockIcontrolInterfaces;
	
	@Mock
	private LocalLBPoolBindingStub mockLocalLBPoolSub;
	
	@Mock
	private PoolMemberMetricsCollector mockPoolMemberMetricsCollector;
	
	@Mock
	private PreVersion11PoolMemberMetricsCollector mockPreVersion11PoolMemberMetricsCollector;
	
	@Mock
	private F5 mockF5;
	
	@Mock
	private MetricsFilter mockMetricsFilter;
	
	@Before
	public void setUp() throws Exception {
		when(mockF5.getDisplayName()).thenReturn("TestF5");
		when(mockIcontrolInterfaces.getLocalLBPool()).thenReturn(mockLocalLBPoolSub);
		
		whenNew(PoolMemberMetricsCollector.class)
				.withArguments(any(Set.class), any(Set.class), any(Interfaces.class))
				.thenReturn(mockPoolMemberMetricsCollector);
		
		whenNew(PreVersion11PoolMemberMetricsCollector.class)
		.withArguments(any(Set.class), any(Set.class), any(Interfaces.class))
		.thenReturn(mockPreVersion11PoolMemberMetricsCollector);
	}
	
	@Test
	public void testNoPoolsIncluded() throws Exception {
		classUnderTest = new PoolMetricsCollector(mockIcontrolInterfaces, 
				mockF5, mockMetricsFilter);
		
		F5Metrics result = classUnderTest.call();
		assertEquals(0, result.getMetrics().size());
	}
	
	@Test
	public void testIncludeAllPoolsForPreVersion11() throws Exception {
		when(mockF5.isPreVersion11()).thenReturn(true);
		runIncludeAllPoolsTest();
		verify(mockPreVersion11PoolMemberMetricsCollector)
			.collectMemberMetrics(anyString(), any(String[].class), any(F5Metrics.class));
	}
	
	@Test
	public void testIncludeAllPools() throws Exception {
		when(mockF5.isPreVersion11()).thenReturn(false);
		runIncludeAllPoolsTest();
		verify(mockPoolMemberMetricsCollector)
			.collectMemberMetrics(anyString(), any(String[].class), any(F5Metrics.class));
	}
	
	@Test
	public void testExcludeMetrics() throws Exception {
		Set<String> testIncludes = new HashSet<String>();
		testIncludes.add(".*");
		when(mockF5.getPoolIncludes()).thenReturn(testIncludes);
		
		Set<String> testMetricExcludes = new HashSet<String>();
		testMetricExcludes.add(".*BYTES.*");
		when(mockMetricsFilter.getPoolMetricExcludes()).thenReturn(testMetricExcludes);
		
		String[] testPools = getTestPools();
		when(mockLocalLBPoolSub.get_list()).thenReturn(testPools);
		
		LocalLBPoolPoolStatistics testStats = getTestStatistics();
		when(mockLocalLBPoolSub.get_statistics(any(String[].class))).thenReturn(testStats);
		
		classUnderTest = new PoolMetricsCollector(mockIcontrolInterfaces, 
				mockF5, mockMetricsFilter);
		F5Metrics result = classUnderTest.call();
		assertEquals(3, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7_agent|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr8_agent|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));

		// excluded
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|STATISTIC_SERVER_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7_agent|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7_agent|STATISTIC_SERVER_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr8_agent|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr8_agent|STATISTIC_SERVER_SIDE_BYTES_IN"));
	}
	
	private void runIncludeAllPoolsTest() throws Exception {
		Set<String> testIncludes = new HashSet<String>();
		testIncludes.add(".*");
		when(mockF5.getPoolIncludes()).thenReturn(testIncludes);
		
		Set<String> testMemberIncludes = new HashSet<String>();
		testMemberIncludes.add(".*");
		when(mockF5.getPoolMemberIncludes()).thenReturn(testMemberIncludes);
		
		String[] testPools = getTestPools();
		when(mockLocalLBPoolSub.get_list()).thenReturn(testPools);
		
		LocalLBPoolPoolStatistics testStats = getTestStatistics();
		when(mockLocalLBPoolSub.get_statistics(any(String[].class))).thenReturn(testStats);
		
		classUnderTest = new PoolMetricsCollector(mockIcontrolInterfaces, 
				mockF5, mockMetricsFilter);
		F5Metrics result = classUnderTest.call();
		assertEquals(9, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|STATISTIC_SERVER_SIDE_BYTES_IN"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7_agent|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7_agent|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7_agent|STATISTIC_SERVER_SIDE_BYTES_IN"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr8_agent|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr8_agent|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr8_agent|STATISTIC_SERVER_SIDE_BYTES_IN"));		
	}
	
	private LocalLBPoolPoolStatistics getTestStatistics() {
		String[] testPools = getTestPools();
		LocalLBPoolPoolStatistics stats = 
				new LocalLBPoolPoolStatistics();
		
		LocalLBPoolPoolStatisticEntry[] entries =
				new LocalLBPoolPoolStatisticEntry[testPools.length];
		stats.setStatistics(entries);
		
		CommonStatisticType[] statisticTypes = getTestStatisticTypes();
		
		for (int poolIndex=0; poolIndex<testPools.length; poolIndex++) {
			LocalLBPoolPoolStatisticEntry entry =
					new LocalLBPoolPoolStatisticEntry();
			entry.setPool_name(testPools[poolIndex]);
			entries[poolIndex] = entry;
			
			CommonStatistic[] commonStats = new CommonStatistic[statisticTypes.length];
			
			for (int index=0; index < statisticTypes.length; index++) {
				CommonStatistic commonStat = new CommonStatistic();
				commonStat.setType(statisticTypes[index]);
				commonStat.setValue(getTestValue());
				commonStats[index] = commonStat;
			}
			
			entry.setStatistics(commonStats);
		}
		
		return stats;
	}
	
	private String[] getTestPools() {
		String[] testPools = { "/Common/devcontr7", 
				"/Common/devcontr7_agent", "/Common/devcontr8_agent"};
		return testPools;
	}
	
	private CommonStatisticType[] getTestStatisticTypes() {
		CommonStatisticType[] testMetricTypes = { 
				CommonStatisticType.STATISTIC_PVA_CLIENT_SIDE_BYTES_IN,
				CommonStatisticType.STATISTIC_CONNQUEUE_AGE_MOVING_AVG,
				CommonStatisticType.STATISTIC_SERVER_SIDE_BYTES_IN };
		
		return testMetricTypes;
	}
	
	private CommonULong64 getTestValue() {
		CommonULong64 testValue = new CommonULong64();
		testValue.setHigh(new Random(100).nextLong());
		testValue.setHigh(new Random(10).nextLong());
		return testValue;
	}
}

package com.appdynamics.extensions.f5.collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import iControl.CommonAddressPort;
import iControl.CommonIPPortDefinition;
import iControl.CommonStatistic;
import iControl.CommonStatisticType;
import iControl.CommonULong64;
import iControl.Interfaces;
import iControl.LocalLBAvailabilityStatus;
import iControl.LocalLBEnabledStatus;
import iControl.LocalLBNodeAddressV2BindingStub;
import iControl.LocalLBObjectStatus;
import iControl.LocalLBPoolBindingStub;
import iControl.LocalLBPoolMemberBindingStub;
import iControl.LocalLBPoolMemberMemberObjectStatus;
import iControl.LocalLBPoolMemberStatisticEntry;
import iControl.LocalLBPoolMemberStatistics;

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
public class PoolMemberMetricsCollectorTest {

	private PoolMemberMetricsCollector classUnderTest;
	
	@Mock
	private Interfaces mockIcontrolInterfaces;
	
	@Mock
	private LocalLBPoolBindingStub mockLocalLBPoolStub;
	
	@Mock
	private LocalLBNodeAddressV2BindingStub mockNodeAddressV2Stub;
	
	@Mock
	private LocalLBPoolMemberBindingStub mockLocalLBPoolMemberStub;
	
	@Mock
	private F5 mockF5;
	
	@Mock
	private MetricsFilter mockMetricsFilter;
	
	@Before
	public void setUp() throws Exception {
		when(mockF5.getDisplayName()).thenReturn("TestF5");
		when(mockIcontrolInterfaces.getLocalLBPool()).thenReturn(mockLocalLBPoolStub);
		when(mockIcontrolInterfaces.getLocalLBNodeAddressV2()).thenReturn(mockNodeAddressV2Stub);
		when(mockIcontrolInterfaces.getLocalLBPoolMember()).thenReturn(mockLocalLBPoolMemberStub);
		
		LocalLBPoolMemberStatistics[] testMemberStats = getTestStatistics();
		when(mockLocalLBPoolStub.get_all_member_statistics(any(String[].class))).thenReturn(testMemberStats);
		
		String[] testIpAddresses = getTestIpAddresses();
		when(mockNodeAddressV2Stub.get_address(any(String[].class))).thenReturn(testIpAddresses);
		
		LocalLBPoolMemberMemberObjectStatus[][] testStatuses = getTestStatuses();
		when(mockLocalLBPoolMemberStub.get_object_status(any(String[].class))).thenReturn(testStatuses);
	}
	
	@Test
	public void testNoPoolMembersIncluded() throws Exception {
		classUnderTest = new PoolMemberMetricsCollector(mockF5.getPoolMemberIncludes(), 
				mockMetricsFilter.getPoolMetricExcludes(), mockIcontrolInterfaces);
		
		String poolMetricPrefix = "TestF5|Pools";
		String[] pools = getTestPools();
		
		F5Metrics result = new F5Metrics();
		
		classUnderTest.collectMemberMetrics(poolMetricPrefix, pools, result);
		assertEquals(0, result.getMetrics().size());
	}
	
	@Test
	public void testAllPoolMembersIncluded() throws Exception {
		Set<String> testIncludes = new HashSet<String>();
		testIncludes.add(".*");
		when(mockF5.getPoolMemberIncludes()).thenReturn(testIncludes);
		
		classUnderTest = new PoolMemberMetricsCollector(mockF5.getPoolMemberIncludes(), 
				mockMetricsFilter.getPoolMetricExcludes(), mockIcontrolInterfaces);
		
		String poolMetricPrefix = "TestF5|Pools";
		String[] pools = getTestPools();
		
		F5Metrics result = new F5Metrics();
		
		classUnderTest.collectMemberMetrics(poolMetricPrefix, pools, result);
		assertEquals(8, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member1:8080|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member1:8080|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member1:8080|STATISTIC_SERVER_SIDE_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member1:8080|STATUS"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member2:8090|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member2:8090|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member2:8090|STATISTIC_SERVER_SIDE_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member2:8090|STATUS"));
	}
	
	@Test
	public void testExcludeMetrics() throws Exception {
		Set<String> testIncludes = new HashSet<String>();
		testIncludes.add(".*");
		when(mockF5.getPoolMemberIncludes()).thenReturn(testIncludes);
		
		Set<String> testMetricExcludes = new HashSet<String>();
		testMetricExcludes.add(".*STATISTIC.*");
		when(mockMetricsFilter.getPoolMetricExcludes()).thenReturn(testMetricExcludes);
		
		classUnderTest = new PoolMemberMetricsCollector(mockF5.getPoolMemberIncludes(), 
				mockMetricsFilter.getPoolMetricExcludes(), mockIcontrolInterfaces);
		
		String poolMetricPrefix = "TestF5|Pools";
		String[] pools = getTestPools();
		
		F5Metrics result = new F5Metrics();
		
		classUnderTest.collectMemberMetrics(poolMetricPrefix, pools, result);
		assertEquals(2, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member1:8080|STATUS"));
		assertTrue(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member2:8090|STATUS"));

		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member1:8080|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member1:8080|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member1:8080|STATISTIC_SERVER_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member2:8090|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member2:8090|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
		assertFalse(result.getMetrics().containsKey("TestF5|Pools|Common|devcontr7|devcontr7_member2:8090|STATISTIC_SERVER_SIDE_BYTES_IN"));
	}
	
	private LocalLBPoolMemberStatistics[] getTestStatistics() {
		String[] testPools = getTestPools();
		
		LocalLBPoolMemberStatistics[] statsArray = 
				new LocalLBPoolMemberStatistics[testPools.length];
		
		CommonStatisticType[] statisticTypes = getTestStatisticTypes();
		
		for (int poolIndex=0; poolIndex<testPools.length; poolIndex++) {
			LocalLBPoolMemberStatistics stats = new LocalLBPoolMemberStatistics();
			statsArray[poolIndex] = stats;
			
			CommonAddressPort[] testMembers = getTestMembers();
			LocalLBPoolMemberStatisticEntry[] memberEntries =
					new LocalLBPoolMemberStatisticEntry[testMembers.length];
			stats.setStatistics(memberEntries);
			
			for (int memberIndex=0; memberIndex<testMembers.length; memberIndex++) {
				LocalLBPoolMemberStatisticEntry memberEntry = new LocalLBPoolMemberStatisticEntry();
				memberEntry.setMember(testMembers[memberIndex]);
				
				CommonStatistic[] commonStats = new CommonStatistic[statisticTypes.length];
				
				for (int index=0; index < statisticTypes.length; index++) {
					CommonStatistic commonStat = new CommonStatistic();
					commonStat.setType(statisticTypes[index]);
					commonStat.setValue(getTestValue());
					commonStats[index] = commonStat;
				}
				
				memberEntry.setStatistics(commonStats);
				memberEntries[memberIndex] = memberEntry;
			}
			
		}
		
		return statsArray;
	}
	
	private LocalLBPoolMemberMemberObjectStatus[][] getTestStatuses() {
		String[] testPools = getTestPools();
		
		LocalLBPoolMemberMemberObjectStatus[][] testStatuses = 
				new LocalLBPoolMemberMemberObjectStatus[testPools.length][];
		
		for(int poolIndex=0; poolIndex<testPools.length; poolIndex++) {
			CommonAddressPort[] testMembers = getTestMembers();
			String[] testIpAddresses = getTestIpAddresses();
			
			LocalLBPoolMemberMemberObjectStatus[] memberStatuses = 
					new LocalLBPoolMemberMemberObjectStatus[testMembers.length];
			testStatuses[poolIndex] = memberStatuses;
			
			for (int memberIndex=0; memberIndex<testMembers.length; memberIndex++) {
				CommonIPPortDefinition member = new CommonIPPortDefinition();
				member.setAddress(testIpAddresses[memberIndex]);
				member.setPort(testMembers[memberIndex].getPort());
				
				LocalLBPoolMemberMemberObjectStatus memberStatus = new LocalLBPoolMemberMemberObjectStatus();
				memberStatus.setMember(member);
				
				LocalLBObjectStatus status = new LocalLBObjectStatus();
				status.setAvailability_status(LocalLBAvailabilityStatus.AVAILABILITY_STATUS_GREEN);
				status.setEnabled_status(LocalLBEnabledStatus.ENABLED_STATUS_ENABLED);
				memberStatus.setObject_status(status);
				
				memberStatuses[memberIndex] = memberStatus;
			}
		}
		
		return testStatuses;
	}
	
	private String[] getTestPools() {
		return new String[] {"/Common/devcontr7"};
	}
	
	private CommonAddressPort[] getTestMembers() {
		CommonAddressPort[] members = new CommonAddressPort[2];
		
		CommonAddressPort member1 = new CommonAddressPort();
		member1.setAddress("/Common/devcontr7_member1");
		member1.setPort(8080);
		members[0] = member1;
		
		CommonAddressPort member2 = new CommonAddressPort();
		member2.setAddress("/Common/devcontr7_member2");
		member2.setPort(8090);
		members[1] = member2;
		
		return members;
	}
	
	private String[] getTestIpAddresses() {
		return new String[] { "10.10.10.1", "10.10.10.2"};
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

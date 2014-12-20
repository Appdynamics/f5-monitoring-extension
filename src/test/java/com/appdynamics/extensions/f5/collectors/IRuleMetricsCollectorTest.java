package com.appdynamics.extensions.f5.collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import iControl.CommonStatistic;
import iControl.CommonStatisticType;
import iControl.CommonULong64;
import iControl.Interfaces;
import iControl.LocalLBRuleBindingStub;
import iControl.LocalLBRuleRuleStatisticEntry;
import iControl.LocalLBRuleRuleStatistics;

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
public class IRuleMetricsCollectorTest {

	private IRuleMetricsCollector classUnderTest;
	
	@Mock
	private Interfaces mockIcontrolInterfaces;
	
	@Mock
	private LocalLBRuleBindingStub mockLocalLBRuleSub;
	
	@Mock
	private F5 mockF5;
	
	@Mock
	private MetricsFilter mockMetricsFilter;
	
	@Before
	public void setUp() throws Exception {
		when(mockF5.getDisplayName()).thenReturn("TestF5");
		when(mockIcontrolInterfaces.getLocalLBRule()).thenReturn(mockLocalLBRuleSub);
	}
	
	@Test
	public void testNoIrulesIncluded() throws Exception {
		classUnderTest = new IRuleMetricsCollector(mockIcontrolInterfaces, 
				mockF5, mockMetricsFilter);
		
		F5Metrics result = classUnderTest.call();
		assertEquals(0, result.getMetrics().size());
	}
	
	@Test
	public void testIncludeAllIRules() throws Exception {
		Set<String> testIncludes = new HashSet<String>();
		testIncludes.add(".*");
		when(mockF5.getiRuleIncludes()).thenReturn(testIncludes);
		
		String[] testProfiles = getTestIruleNames();
		when(mockLocalLBRuleSub.get_list()).thenReturn(testProfiles);
		
		LocalLBRuleRuleStatistics testStats = getTestStatistics();
		when(mockLocalLBRuleSub.get_statistics(any(String[].class))).thenReturn(testStats);
		
		classUnderTest = new IRuleMetricsCollector(mockIcontrolInterfaces, 
				mockF5, mockMetricsFilter);
		F5Metrics result = classUnderTest.call();
		assertEquals(9, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_APM_activesync|HTTP REQUEST|STATISTIC_RULE_ABORTS"));
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_APM_activesync|HTTP REQUEST|STATISTIC_RULE_AVERAGE_CYCLES"));
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_APM_activesync|HTTP REQUEST|STATISTIC_RULE_MAXIMUM_CYCLES"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_ldap|HTTP REQUEST|STATISTIC_RULE_ABORTS"));
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_ldap|HTTP REQUEST|STATISTIC_RULE_AVERAGE_CYCLES"));
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_ldap|HTTP REQUEST|STATISTIC_RULE_MAXIMUM_CYCLES"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_radius|HTTP REQUEST|STATISTIC_RULE_ABORTS"));
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_radius|HTTP REQUEST|STATISTIC_RULE_AVERAGE_CYCLES"));
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_radius|HTTP REQUEST|STATISTIC_RULE_MAXIMUM_CYCLES"));
	}
	
	@Test
	public void testExcludeMetrics() throws Exception {
		Set<String> testIncludes = new HashSet<String>();
		testIncludes.add(".*");
		when(mockF5.getiRuleIncludes()).thenReturn(testIncludes);
		
		Set<String> testMetricExcludes = new HashSet<String>();
		testMetricExcludes.add("STATISTIC_RULE_ABORTS");
		testMetricExcludes.add("STATISTIC_RULE_MAXIMUM_CYCLES");
		when(mockMetricsFilter.getiRuleMetricExcludes()).thenReturn(testMetricExcludes);
		
		String[] testProfiles = getTestIruleNames();
		when(mockLocalLBRuleSub.get_list()).thenReturn(testProfiles);
		
		LocalLBRuleRuleStatistics testStats = getTestStatistics();
		when(mockLocalLBRuleSub.get_statistics(any(String[].class))).thenReturn(testStats);
		
		classUnderTest = new IRuleMetricsCollector(mockIcontrolInterfaces, 
				mockF5, mockMetricsFilter);
		F5Metrics result = classUnderTest.call();
		assertEquals(3, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_APM_activesync|HTTP REQUEST|STATISTIC_RULE_AVERAGE_CYCLES"));
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_ldap|HTTP REQUEST|STATISTIC_RULE_AVERAGE_CYCLES"));
		assertTrue(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_radius|HTTP REQUEST|STATISTIC_RULE_AVERAGE_CYCLES"));
		
		//excluded
		assertFalse(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_APM_activesync|HTTP REQUEST|STATISTIC_RULE_ABORTS"));
		assertFalse(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_APM_activesync|HTTP REQUEST|STATISTIC_RULE_MAXIMUM_CYCLES"));
		assertFalse(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_ldap|HTTP REQUEST|STATISTIC_RULE_ABORTS"));
		assertFalse(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_ldap|HTTP REQUEST|STATISTIC_RULE_MAXIMUM_CYCLES"));
		assertFalse(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_radius|HTTP REQUEST|STATISTIC_RULE_ABORTS"));
		assertFalse(result.getMetrics().containsKey("TestF5|iRules|Common|_sys_auth_radius|HTTP REQUEST|STATISTIC_RULE_MAXIMUM_CYCLES"));
	}
	
	private LocalLBRuleRuleStatistics getTestStatistics() {
		String[] testIrules = getTestIruleNames();
		LocalLBRuleRuleStatistics stats = 
				new LocalLBRuleRuleStatistics();
		
		LocalLBRuleRuleStatisticEntry[] entries =
				new LocalLBRuleRuleStatisticEntry[testIrules.length];
		stats.setStatistics(entries);
		
		CommonStatisticType[] statisticTypes = getTestStatisticTypes();
		
		for (int iRuleIndex=0; iRuleIndex<testIrules.length; iRuleIndex++) {
			LocalLBRuleRuleStatisticEntry entry =
					new LocalLBRuleRuleStatisticEntry();
			entry.setRule_name(testIrules[iRuleIndex]);
			entry.setEvent_name("HTTP REQUEST");
			entries[iRuleIndex] = entry;
			
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
	
	private String[] getTestIruleNames() {
		String[] testIRuleNames = { "/Common/_sys_APM_activesync",
				"/Common/_sys_auth_ldap", "/Common/_sys_auth_radius"};
		return testIRuleNames;
	}
	
	private CommonStatisticType[] getTestStatisticTypes() {
		CommonStatisticType[] testMetricTypes = { 
				CommonStatisticType.STATISTIC_RULE_ABORTS,
				CommonStatisticType.STATISTIC_RULE_AVERAGE_CYCLES,
				CommonStatisticType.STATISTIC_RULE_MAXIMUM_CYCLES};
		
		return testMetricTypes;
	}
	
	private CommonULong64 getTestValue() {
		CommonULong64 testValue = new CommonULong64();
		testValue.setHigh(new Random(100).nextLong());
		testValue.setHigh(new Random(10).nextLong());
		return testValue;
	}
}

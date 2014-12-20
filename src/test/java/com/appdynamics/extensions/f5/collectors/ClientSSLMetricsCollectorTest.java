package com.appdynamics.extensions.f5.collectors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

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

import iControl.CommonStatistic;
import iControl.CommonStatisticType;
import iControl.CommonULong64;
import iControl.Interfaces;
import iControl.LocalLBProfileClientSSLBindingStub;
import iControl.LocalLBProfileClientSSLProfileClientSSLStatisticEntry;
import iControl.LocalLBProfileClientSSLProfileClientSSLStatistics;

@RunWith(MockitoJUnitRunner.class)
public class ClientSSLMetricsCollectorTest {

	private ClientSSLMetricsCollector classUnderTest;
	
	@Mock
	private Interfaces mockIcontrolInterfaces;
	
	@Mock
	private LocalLBProfileClientSSLBindingStub mockClientSSLSub;
	
	@Mock
	private F5 mockF5;
	
	@Mock
	private MetricsFilter mockMetricsFilter;
	
	@Before
	public void setUp() throws Exception {
		when(mockF5.getDisplayName()).thenReturn("TestF5");
		when(mockIcontrolInterfaces.getLocalLBProfileClientSSL()).thenReturn(mockClientSSLSub);
	}
	
	@Test
	public void testNoClientSSLProfileIncluded() throws Exception {
		classUnderTest = new ClientSSLMetricsCollector(mockIcontrolInterfaces, 
				mockF5, mockMetricsFilter);
		
		F5Metrics result = classUnderTest.call();
		assertEquals(0, result.getMetrics().size());
	}
	
	@Test
	public void testIncludeAllClientSSLProfile() throws Exception {
		Set<String> testIncludes = new HashSet<String>();
		testIncludes.add(".*");
		when(mockF5.getClientSSLProfileIncludes()).thenReturn(testIncludes);
		
		String[] testProfiles = getTestClientSSLProfiles();
		when(mockClientSSLSub.get_list()).thenReturn(testProfiles);
		
		LocalLBProfileClientSSLProfileClientSSLStatistics testStats = getTestStatistics();
		when(mockClientSSLSub.get_statistics(any(String[].class))).thenReturn(testStats);
		
		classUnderTest = new ClientSSLMetricsCollector(mockIcontrolInterfaces, 
				mockF5, mockMetricsFilter);
		F5Metrics result = classUnderTest.call();
		assertEquals(9, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|clientssl|STATISTIC_SSL_CIPHER_DES_BULK"));
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|clientssl|STATISTIC_SSL_CIPHER_AES_BULK"));
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|clientssl|STATISTIC_SSL_COMMON_BAD_RECORDS"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|SSL_SaaS_Digi|STATISTIC_SSL_CIPHER_DES_BULK"));
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|SSL_SaaS_Digi|STATISTIC_SSL_CIPHER_AES_BULK"));
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|SSL_SaaS_Digi|STATISTIC_SSL_COMMON_BAD_RECORDS"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|wom-default-clientssl|STATISTIC_SSL_CIPHER_DES_BULK"));
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|wom-default-clientssl|STATISTIC_SSL_CIPHER_AES_BULK"));
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|wom-default-clientssl|STATISTIC_SSL_COMMON_BAD_RECORDS"));
	}
	
	@Test
	public void testExcludeMetrics() throws Exception {
		Set<String> testIncludes = new HashSet<String>();
		testIncludes.add(".*");
		when(mockF5.getClientSSLProfileIncludes()).thenReturn(testIncludes);
		
		Set<String> testMetricExcludes = new HashSet<String>();
		testMetricExcludes.add("STATISTIC_SSL_CIPHER_AES_BULK");
		testMetricExcludes.add("STATISTIC_SSL_COMMON_BAD_RECORDS");
		when(mockMetricsFilter.getClientSSLProfileMetricExcludes()).thenReturn(testMetricExcludes);
		
		String[] testProfiles = getTestClientSSLProfiles();
		when(mockClientSSLSub.get_list()).thenReturn(testProfiles);
		
		LocalLBProfileClientSSLProfileClientSSLStatistics testStats = getTestStatistics();
		when(mockClientSSLSub.get_statistics(any(String[].class))).thenReturn(testStats);
		
		classUnderTest = new ClientSSLMetricsCollector(mockIcontrolInterfaces, 
				mockF5, mockMetricsFilter);
		F5Metrics result = classUnderTest.call();
		assertEquals(3, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|clientssl|STATISTIC_SSL_CIPHER_DES_BULK"));
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|SSL_SaaS_Digi|STATISTIC_SSL_CIPHER_DES_BULK"));
		assertTrue(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|wom-default-clientssl|STATISTIC_SSL_CIPHER_DES_BULK"));
		
		// not included
		assertFalse(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|clientssl|STATISTIC_SSL_CIPHER_AES_BULK"));
		assertFalse(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|clientssl|STATISTIC_SSL_COMMON_BAD_RECORDS"));
		assertFalse(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|SSL_SaaS_Digi|STATISTIC_SSL_CIPHER_AES_BULK"));
		assertFalse(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|SSL_SaaS_Digi|STATISTIC_SSL_COMMON_BAD_RECORDS"));
		assertFalse(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|wom-default-clientssl|STATISTIC_SSL_CIPHER_AES_BULK"));
		assertFalse(result.getMetrics().containsKey("TestF5|SSL|Clients|Common|wom-default-clientssl|STATISTIC_SSL_COMMON_BAD_RECORDS"));
	}
	
	private LocalLBProfileClientSSLProfileClientSSLStatistics getTestStatistics() {
		String[] testProfiles = getTestClientSSLProfiles();
		LocalLBProfileClientSSLProfileClientSSLStatistics stats = 
				new LocalLBProfileClientSSLProfileClientSSLStatistics();
		
		LocalLBProfileClientSSLProfileClientSSLStatisticEntry[] entries =
				new LocalLBProfileClientSSLProfileClientSSLStatisticEntry[testProfiles.length];
		stats.setStatistics(entries);
		
		CommonStatisticType[] statisticTypes = getTestStatisticTypes();
		
		for (int profileIndex=0; profileIndex<testProfiles.length; profileIndex++) {
			LocalLBProfileClientSSLProfileClientSSLStatisticEntry entry =
					new LocalLBProfileClientSSLProfileClientSSLStatisticEntry();
			entry.setProfile_name(testProfiles[profileIndex]);
			entries[profileIndex] = entry;
			
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
	
	private String[] getTestClientSSLProfiles() {
		String[] testSSLProfiles = { "/Common/clientssl",
				"/Common/SSL_SaaS_Digi", "/Common/wom-default-clientssl"};
		return testSSLProfiles;
	}
	
	private CommonStatisticType[] getTestStatisticTypes() {
		CommonStatisticType[] testMetricTypes = { 
				CommonStatisticType.STATISTIC_SSL_CIPHER_DES_BULK,
				CommonStatisticType.STATISTIC_SSL_CIPHER_AES_BULK,
				CommonStatisticType.STATISTIC_SSL_COMMON_BAD_RECORDS};
		
		return testMetricTypes;
	}
	
	private CommonULong64 getTestValue() {
		CommonULong64 testValue = new CommonULong64();
		testValue.setHigh(new Random(100).nextLong());
		testValue.setHigh(new Random(10).nextLong());
		return testValue;
	}
}

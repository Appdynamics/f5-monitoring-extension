package com.appdynamics.extensions.f5.util;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class F5UtilTest {
	
	@Test
	public void testFilterIncludesWithSinglePattern() {
		String[] stringArrayToFilter = new String[] {
				"/Common/_sys_auth_ldap",
				"/Common/_sys_auth_ssl_cc_ldap",
				"/Common/_sys_auth_ssl_ocsp",
				"/Common/_sys_auth_tacacs"
		};
		
		Pattern includePatterns = Pattern.compile("/Common/_sys_auth_ssl_ocsp");
		
		String[] results = F5Util.filterIncludes(stringArrayToFilter, includePatterns);
		assertEquals(1, results.length);
		assertEquals("/Common/_sys_auth_ssl_ocsp", results[0]);
	}
	
	@Test
	public void testFilterIncludesWithMultiplePatterns() {
		String[] stringArrayToFilter = new String[] {
				"/Common/_sys_auth_ldap",
				"/Common/_sys_auth_ssl_cc_ldap",
				"/Common/_sys_auth_ssl_ocsp",
				"/Common/_sys_auth_tacacs"
		};
		
		Pattern includePatterns = Pattern.compile(".*ocsp.*|.*acs.*");
		
		String[] results = F5Util.filterIncludes(stringArrayToFilter, includePatterns);
		assertEquals(2, results.length);
		assertEquals("/Common/_sys_auth_ssl_ocsp", results[0]);
		assertEquals("/Common/_sys_auth_tacacs", results[1]);
	}
	
	@Test
	public void testIsMetricToMonitorReturnsTrue() {
		String metricName = "STATISTIC_RULE_TOTAL_EXECUTIONS";
		Pattern excludePatterns = Pattern.compile(".*FAILURES.*");
		
		boolean result = F5Util.isMetricToMonitor(metricName, excludePatterns);
		assertTrue(result);
	}

	@Test
	public void testIsMetricToMonitorReturnsFalse() {
		String metricName = "STATISTIC_RULE_FAILURES";
		Pattern excludePatterns = Pattern.compile(".*FAILURES.*");
		
		boolean result = F5Util.isMetricToMonitor(metricName, excludePatterns);
		assertFalse(result);
	}
}

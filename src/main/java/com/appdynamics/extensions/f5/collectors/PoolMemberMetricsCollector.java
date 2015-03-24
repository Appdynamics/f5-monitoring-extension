package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.*;
import static com.appdynamics.extensions.f5.util.F5Util.*;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.LocalLBPoolMemberMemberObjectStatus;
import iControl.LocalLBPoolMemberStatisticEntry;
import iControl.LocalLBPoolMemberStatistics;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.appdynamics.extensions.f5.F5Metrics;
import com.appdynamics.extensions.f5.util.F5Util.PoolStatus;
import com.google.common.collect.Maps;

/**
 * @author Florencio Sarmiento
 *
 */
public class PoolMemberMetricsCollector {
	
	public static final Logger LOGGER = 
			Logger.getLogger("com.singularity.extensions.f5.collectors.PoolMemberMetricsCollector");
	
	private Pattern excludePatterns;
	private Pattern poolMemberIncludesPattern;
	private Interfaces iControlInterfaces;
	
	public PoolMemberMetricsCollector(Set<String> poolMemberIncludes,
			Set<String> metricExcludes, Interfaces iControlInterfaces) {
		
		this.excludePatterns = createPattern(metricExcludes);
		this.poolMemberIncludesPattern = createPattern(poolMemberIncludes);
		this.iControlInterfaces = iControlInterfaces;
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v11.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__Pool__get_all_member_statistics.ashx
	 *
	 */
	public void collectMemberMetrics(String poolMetricPrefix, String[] pools, F5Metrics f5Metrics) {
		try {
			LocalLBPoolMemberStatistics[] poolMembersStats = 
					iControlInterfaces.getLocalLBPool().get_all_member_statistics(pools);
			Set<String> rawMemberNames = new HashSet<String>();
			
			Map<String, BigInteger> poolMemberCountStats = initialisePoolMemberCountStats(pools);
			
			int index = 0;
			
			// The outer layer represents the pool which is in the same order as the pools array
			// Don't know why F5 didn't just include the name in this object
			for (LocalLBPoolMemberStatistics member : poolMembersStats) {
				String poolName = insertSeparatorAtStartIfNotThere(pools[index++], PATH_SEPARATOR);
				
				for (LocalLBPoolMemberStatisticEntry memStat : member.getStatistics()) {
					incrementPoolMemberCountStats(poolName, poolMemberCountStats);
					
					String rawMemberName = memStat.getMember().getAddress();
					rawMemberNames.add(rawMemberName);
					
					String memberName = extractMemberName(rawMemberName, PATH_SEPARATOR);
					String fullMemberName = getFullMemberName(poolName, memberName, 
							memStat.getMember().getPort());
					
					if (isToMonitor(fullMemberName, poolMemberIncludesPattern)) {
						// changing the separator for metric reporting
						fullMemberName = changePathSeparator(
								fullMemberName, PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);
						
						String poolMemberMetricPrefix = String.format("%s%s", poolMetricPrefix,
								fullMemberName);
						
						for (CommonStatistic stat : memStat.getStatistics()) {
							if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
								String metricName = String.format("%s%s%s", poolMemberMetricPrefix, 
										METRIC_PATH_SEPARATOR, stat.getType().getValue());
								BigInteger value = convertValue(stat.getValue());
								f5Metrics.add(metricName, value);
							}
						}
					}
				}
				
			}
			
			if (isMetricToMonitor(TOTAL_NO_OF_MEMBERS, excludePatterns)) {
				includePoolMemberCountStatsForReporting(poolMetricPrefix, poolMemberCountStats, f5Metrics);
			}
			
			if (isMetricToMonitor(STATUS, excludePatterns)) {
				collectMemberStatus(poolMetricPrefix, pools, 
						rawMemberNames.toArray(new String[rawMemberNames.size()]), f5Metrics);
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching pool members' statistics", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching pool members' statistics", e);
		} 
	}
	
	protected Map<String, BigInteger> initialisePoolMemberCountStats(String[] pools) {
		Map<String, BigInteger> poolMemberCountStats = new HashMap<String, BigInteger>();
		
		for (String poolName : pools) {
			poolMemberCountStats.put(insertSeparatorAtStartIfNotThere(poolName, PATH_SEPARATOR), 
					BigInteger.ZERO);
		}		
		
		return poolMemberCountStats;
	}
	
	protected void incrementPoolMemberCountStats(String poolName,
			Map<String, BigInteger> poolMemberCountStats) {
		
		BigInteger curValue = poolMemberCountStats.get(poolName);
		poolMemberCountStats.put(poolName, curValue.add(BigInteger.ONE));
	}

	protected void includePoolMemberCountStatsForReporting(String poolMetricPrefix,
			Map<String, BigInteger> poolMemberCountStats, F5Metrics f5Metrics) {
		
		for (Map.Entry<String, BigInteger> poolMemberCountStat : poolMemberCountStats.entrySet()) {
			String poolNameForMetric = changePathSeparator(
					poolMemberCountStat.getKey(), PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);
			
			String metricName = String.format("%s%s%s%s", poolMetricPrefix, 
					poolNameForMetric, METRIC_PATH_SEPARATOR, TOTAL_NO_OF_MEMBERS);
			
			f5Metrics.add(metricName, poolMemberCountStat.getValue());
		}
	}
	
	/*
	 * This method uses an old interface that returns the pool member's IP address, 
	 * i.e. getMember().getAddress(), rather than the node name (as per newer interface). 
	 * Therefore IP to name conversion is required.
	 * 
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__PoolMember__get_object_status.ashx
	 */
	protected void collectMemberStatus(String poolMetricPrefix, String[] pools, 
			String[] memberNames, F5Metrics f5Metrics) {
		try {
			if (ArrayUtils.isNotEmpty(memberNames)) {
				Map<String, String> nameConversionMap = createNameConversionMap(memberNames);
				
				LocalLBPoolMemberMemberObjectStatus [][] memberObjectStatuses = 
						iControlInterfaces.getLocalLBPoolMember().get_object_status(pools);
				
				Map<String, Map<String, BigInteger>> totalMemberAvailabilityStats = 
						initialiseTotalMemberAvailabilityStats(pools);
				
				int index = 0;
				
				for (LocalLBPoolMemberMemberObjectStatus[] poolStat : memberObjectStatuses) {
					String poolName = insertSeparatorAtStartIfNotThere(pools[index++], PATH_SEPARATOR);
					
					for (LocalLBPoolMemberMemberObjectStatus memberStat : poolStat) {
						String rawMemberName = nameConversionMap.get(memberStat.getMember().getAddress());
						String memberName = extractMemberName(rawMemberName, PATH_SEPARATOR);
						String fullMemberName = getFullMemberName(poolName, memberName, 
								memberStat.getMember().getPort());
						
						fullMemberName = changePathSeparator(
								fullMemberName, PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);
						
						PoolStatus status = convertToStatus(
								memberStat.getObject_status().getAvailability_status(), 
								memberStat.getObject_status().getEnabled_status());
						
						incrementTotalMemberAvailabilityStats(poolName, status, totalMemberAvailabilityStats);
						
						if (isToMonitor(fullMemberName, poolMemberIncludesPattern)) {
							String metricName = String.format("%s%s%s%s", 
									poolMetricPrefix, fullMemberName, 
									METRIC_PATH_SEPARATOR, STATUS);
							
							BigInteger availabilityValue = BigInteger.valueOf(status.getValue());
							f5Metrics.add(metricName, availabilityValue);
						}
					}
				}
				
				includeTotalMemberAvailabilityStatsForReportingIfApplicable(poolMetricPrefix, 
						totalMemberAvailabilityStats, f5Metrics);
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching pool members' status", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching pool members' status", e);
		} 
	}

	private Map<String, Map<String, BigInteger>> initialiseTotalMemberAvailabilityStats(String[] pools) {
		Map<String, Map<String, BigInteger>> totalMemberAvailabilityStats = new HashMap<String, Map<String, BigInteger>>();
		
		for (String poolName : pools) {
			Map<String, BigInteger> stats = new HashMap<String, BigInteger>();
			stats.put(TOTAL_NO_OF_MEMBERS_AVAILABLE, BigInteger.ZERO);
			stats.put(TOTAL_NO_OF_MEMBERS_UNAVAILABLE, BigInteger.ZERO);
			totalMemberAvailabilityStats.put(insertSeparatorAtStartIfNotThere(poolName, PATH_SEPARATOR), 
					stats);
		}
		
		return totalMemberAvailabilityStats;
	}
	
	private void incrementTotalMemberAvailabilityStats(String poolName, PoolStatus status, 
			Map<String, Map<String, BigInteger>> totalMemberAvailabilityStats) {
		
		Map<String, BigInteger> stats = totalMemberAvailabilityStats.get(poolName);
		
		if (status == PoolStatus.AVAILABLE_AND_ENABLED) {
			BigInteger curValue = stats.get(TOTAL_NO_OF_MEMBERS_AVAILABLE);
			stats.put(TOTAL_NO_OF_MEMBERS_AVAILABLE, curValue.add(BigInteger.ONE));
		} else {
			BigInteger curValue = stats.get(TOTAL_NO_OF_MEMBERS_UNAVAILABLE);
			stats.put(TOTAL_NO_OF_MEMBERS_UNAVAILABLE, curValue.add(BigInteger.ONE));
		}
		
		totalMemberAvailabilityStats.put(poolName, stats);
	}

	private void includeTotalMemberAvailabilityStatsForReportingIfApplicable(String poolMetricPrefix,
			Map<String, Map<String, BigInteger>> totalMemberAvailabilityStats,
			F5Metrics f5Metrics) {
		
		Set<String> poolNames = totalMemberAvailabilityStats.keySet();
		
		for (String poolName : poolNames) {
			String poolNameForMetric = changePathSeparator(poolName, PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);
			
			if (isMetricToMonitor(TOTAL_NO_OF_MEMBERS_AVAILABLE, excludePatterns)) {
				String metricName = String.format("%s%s%s%s", 
						poolMetricPrefix, poolNameForMetric, 
						METRIC_PATH_SEPARATOR, TOTAL_NO_OF_MEMBERS_AVAILABLE);
				f5Metrics.add(metricName, totalMemberAvailabilityStats.get(poolName).get(TOTAL_NO_OF_MEMBERS_AVAILABLE));
			}
			
			if (isMetricToMonitor(TOTAL_NO_OF_MEMBERS_UNAVAILABLE, excludePatterns)) {
				String metricName = String.format("%s%s%s%s", 
						poolMetricPrefix, poolNameForMetric, 
						METRIC_PATH_SEPARATOR, TOTAL_NO_OF_MEMBERS_UNAVAILABLE);
				f5Metrics.add(metricName, totalMemberAvailabilityStats.get(poolName).get(TOTAL_NO_OF_MEMBERS_UNAVAILABLE));				
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * Compatible with F5 v11.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__NodeAddressV2__get_address.ashx
	 *
	 */
	protected Map<String, String> createNameConversionMap(String[] memberNames) throws Exception {
		String[] ipAddresses = iControlInterfaces.getLocalLBNodeAddressV2().get_address(memberNames);
		
		Map<String, String> ipToNameMap = Maps.newHashMap();
		
		if (ArrayUtils.isNotEmpty(ipAddresses)) {
			int index = 0;
			
			for (String ipAddress : ipAddresses) {
				String memberName = memberNames[index++];
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Mapping IP [%s] to Name [%s]", ipAddress, memberName));
				}
				
				ipToNameMap.put(ipAddress, memberName);
			}
		}
		
		return ipToNameMap;
	}
	
	protected String getFullMemberName(String poolName, String memberName, long port) {
		return String.format("%s%s%s%s%s", poolName, PATH_SEPARATOR, memberName,
				PATH_SEPARATOR, port);
	}
}

package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.STATUS;
import static com.appdynamics.extensions.f5.util.F5Util.*;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.LocalLBPoolMemberMemberObjectStatus;
import iControl.LocalLBPoolMemberStatisticEntry;
import iControl.LocalLBPoolMemberStatistics;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.appdynamics.extensions.f5.F5Metrics;
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
			
			int index = 0;
			
			// The outer layer represents the pool which is in the same order as the pools array
			// Don't know why F5 didn't just include the name in this object
			for (LocalLBPoolMemberStatistics member : poolMembersStats) {
				String poolName = insertSeparatorAtStartIfNotThere(pools[index++], PATH_SEPARATOR);
				
				for (LocalLBPoolMemberStatisticEntry memStat : member.getStatistics()) {
					String rawMemberName = memStat.getMember().getAddress();
					String memberName = extractMemberName(rawMemberName, PATH_SEPARATOR);
					String fullMemberName = getFullMemberName(poolName, memberName, 
							memStat.getMember().getPort());
					
					if (isToMonitor(fullMemberName, poolMemberIncludesPattern)) {
						rawMemberNames.add(rawMemberName);
						
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
				
				int index = 0;
				
				for (LocalLBPoolMemberMemberObjectStatus[] poolStat : memberObjectStatuses) {
					String poolName = insertSeparatorAtStartIfNotThere(pools[index++], PATH_SEPARATOR);
					
					for (LocalLBPoolMemberMemberObjectStatus memberStat : poolStat) {
						String rawMemberName = nameConversionMap.get(memberStat.getMember().getAddress());
						String memberName = extractMemberName(rawMemberName, PATH_SEPARATOR);
						String fullMemberName = getFullMemberName(poolName, memberName, 
								memberStat.getMember().getPort());
						
						if (!isToMonitor(fullMemberName, poolMemberIncludesPattern)) {
							continue;
						}
						
						fullMemberName = changePathSeparator(
								fullMemberName, PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);
						
						String metricName = String.format("%s%s%s%s", 
								poolMetricPrefix, fullMemberName, 
								METRIC_PATH_SEPARATOR, STATUS);
						
						BigInteger value = BigInteger.valueOf(convertToStatus(
								memberStat.getObject_status().getAvailability_status(), 
								memberStat.getObject_status().getEnabled_status()));
						f5Metrics.add(metricName, value);
					}
				}
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching pool members' status", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching pool members' status", e);
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

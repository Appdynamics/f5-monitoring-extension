package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.STATUS;
import static com.appdynamics.extensions.f5.F5Constants.TOTAL_NO_OF_MEMBERS;
import static com.appdynamics.extensions.f5.util.F5Util.changePathSeparator;
import static com.appdynamics.extensions.f5.util.F5Util.convertValue;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.extractMemberName;
import static com.appdynamics.extensions.f5.util.F5Util.insertSeparatorAtStartIfNotThere;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;
import static com.appdynamics.extensions.f5.util.F5Util.isToMonitor;

import com.google.common.collect.Maps;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.LocalLBPoolMemberMemberStatisticEntry;
import iControl.LocalLBPoolMemberMemberStatistics;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 *
 */
public class PreVersion11PoolMemberMetricsCollector extends PoolMemberMetricsCollector {
	
	private Pattern excludePatterns;
	private Pattern poolMemberIncludesPattern;
	private Interfaces iControlInterfaces;

	public PreVersion11PoolMemberMetricsCollector(
			Set<String> poolMemberIncludes, Set<String> metricExcludes,
			Interfaces iControlInterfaces) {
		
		super(poolMemberIncludes, metricExcludes, iControlInterfaces);
		this.excludePatterns = createPattern(metricExcludes);
		this.poolMemberIncludesPattern = createPattern(poolMemberIncludes);
		this.iControlInterfaces = iControlInterfaces;
	}

	/*	 
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__PoolMember__get_all_statistics.ashx
	 */
	public Map<String, BigInteger> collectMemberMetrics(String poolMetricPrefix, String[] pools) {
		Map<String, BigInteger> poolMemberMetrics = new HashMap<String, BigInteger>();
		try {
			LocalLBPoolMemberMemberStatistics[] poolMembersStats = 
					iControlInterfaces.getLocalLBPoolMember().get_all_statistics(pools);
			Set<String> rawMemberNames = new HashSet<String>();
			
			Map<String, BigInteger> poolMemberCountStats = initialisePoolMemberCountStats(pools);
			
			int index = 0;
			
			for (LocalLBPoolMemberMemberStatistics member : poolMembersStats) {
				String poolName = insertSeparatorAtStartIfNotThere(pools[index++], PATH_SEPARATOR);
				
				for (LocalLBPoolMemberMemberStatisticEntry memStat : member.getStatistics()) {
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
								poolMemberMetrics.put(metricName, value);
							}
						}
					}
				}
				
			}
			
			if (isMetricToMonitor(TOTAL_NO_OF_MEMBERS, excludePatterns)) {
				includePoolMemberCountStatsForReporting(poolMetricPrefix, poolMemberCountStats, poolMemberMetrics);
			}
			
			if (isMetricToMonitor(STATUS, excludePatterns)) {
				collectMemberStatus(poolMetricPrefix, pools, 
						rawMemberNames.toArray(new String[rawMemberNames.size()]), poolMemberMetrics);
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching pool members' statistics", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching pool members' statistics", e);
		}
		return poolMemberMetrics;
	}

	/*
	 * We don't have IP address to name conversion in the old version so we're just simulating it here
	 */
	@Override
	protected Map<String, String> createNameConversionMap(String[] memberNames) throws Exception {
		Map<String, String> nameMapping = Maps.newHashMap();
		
		for (String memberName : memberNames) {
			nameMapping.put(memberName, memberName);
		}
		
		return nameMapping;
		
	}
}

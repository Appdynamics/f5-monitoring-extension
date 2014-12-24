package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.util.F5Util.changePathSeparator;
import static com.appdynamics.extensions.f5.util.F5Util.convertValue;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.extractMemberName;
import static com.appdynamics.extensions.f5.util.F5Util.insertSeparatorAtStartIfNotThere;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;
import static com.appdynamics.extensions.f5.util.F5Util.isToMonitor;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.LocalLBSNATPoolMemberSNATPoolMemberStatisticEntry;
import iControl.LocalLBSNATPoolMemberSNATPoolMemberStatistics;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.regex.Pattern;

import com.appdynamics.extensions.f5.F5Metrics;

/**
 * @author Florencio Sarmiento
 *
 */
public class PreVersion11SnatPoolMemberMetricsCollector extends SnatPoolMemberMetricsCollector {
	
	private Pattern excludePatterns;
	private Pattern snatPoolMemberIncludesPattern;
	private Interfaces iControlInterfaces;
	
	public PreVersion11SnatPoolMemberMetricsCollector(Set<String> snatPoolMemberIncludes,
			Set<String> metricExcludes, Interfaces iControlInterfaces) {
		
		super(snatPoolMemberIncludes, metricExcludes, iControlInterfaces);
		this.excludePatterns = createPattern(metricExcludes);
		this.snatPoolMemberIncludesPattern = createPattern(snatPoolMemberIncludes);
		this.iControlInterfaces = iControlInterfaces;

	}
	
	/*
	 * (non-Javadoc)
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__SNATPoolMember__get_all_statistics.ashx
	 *
	 */
	@Override
	public void collectMemberMetrics(String poolMetricPrefix, String[] pools, F5Metrics f5Metrics) {
		try {
			LocalLBSNATPoolMemberSNATPoolMemberStatistics[] poolMembersStats = 
					iControlInterfaces.getLocalLBSNATPoolMember().get_all_statistics(pools);
			
			int index = 0;
			
			for (LocalLBSNATPoolMemberSNATPoolMemberStatistics member : poolMembersStats) {
				String poolName = insertSeparatorAtStartIfNotThere(pools[index++], PATH_SEPARATOR);
				
				for (LocalLBSNATPoolMemberSNATPoolMemberStatisticEntry memStat : member.getStatistics()) {
					String rawMemberName = memStat.getMember();
					String memberName = extractMemberName(rawMemberName, PATH_SEPARATOR);
					String fullMemberName = String.format("%s%s%s", poolName, PATH_SEPARATOR, memberName);
					
					if (!isToMonitor(fullMemberName, snatPoolMemberIncludesPattern)) {
						continue;
					}
					
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
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching snat pool members' statistics", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching snat pool members' statistics", e);
		} 
	}
}

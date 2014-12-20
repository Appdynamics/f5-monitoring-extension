package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.*;
import static com.appdynamics.extensions.f5.util.F5Util.*;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.LocalLBSNATPoolSNATPoolStatisticEntry;
import iControl.LocalLBSNATPoolSNATPoolStatistics;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.appdynamics.extensions.f5.F5Metrics;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;

/**
 * @author Florencio Sarmiento
 *
 */
public class SnatPoolMetricsCollector implements Callable<F5Metrics> {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.f5.collectors.SnatPoolMetricsCollector");
	
	private F5Metrics f5Metrics = new F5Metrics();
	private Interfaces iControlInterfaces;
	private String f5DisplayName;
	private Set<String> snatPoolIncludes;
	private Set<String> snatPoolMemberIncludes;
	private Set<String> metricExcludes;
	private boolean preVersion11;

	public SnatPoolMetricsCollector(Interfaces iControlInterfaces, 
			F5 f5, MetricsFilter metricsFilter) {
		
		this.iControlInterfaces = iControlInterfaces;
		this.f5DisplayName = f5.getDisplayName();
		this.snatPoolIncludes = f5.getSnatPoolIncludes();
		this.snatPoolMemberIncludes = f5.getSnatPoolMemberIncludes();
		this.metricExcludes = metricsFilter.getSnatPoolMetricExcludes();
		this.preVersion11 = f5.isPreVersion11();
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__SNATPool__get_list.ashx
	 *
	 */
	public F5Metrics call() throws Exception {
		LOGGER.info("SNAT Pool metrics collector started...");
		
		if (snatPoolIncludes == null || snatPoolIncludes.isEmpty()) {
			LOGGER.info("No SNAT pools were included for monitoring.");
			return f5Metrics;
		}
		
		try {
			String[] pools = iControlInterfaces.getLocalLBSNATPool().get_list();
			
			if (ArrayUtils.isNotEmpty(pools)) {
				Pattern poolIncludesPattern = createPattern(snatPoolIncludes);
				pools = filterIncludes(pools, poolIncludesPattern);
				
				if (ArrayUtils.isNotEmpty(pools)) {
					collectSnatPoolMetrics(pools);
					collectMemberMetrics(pools);
					
				} else {
					LOGGER.info("No SNAT Pool matched.");
				}
			} else {
				LOGGER.info("No SNAT Pool found.");
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching snat pool list", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching snat pool list", e);
		} 
		
		return f5Metrics;
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__SNATPool__get_statistics.ashx
	 *
	 */
	private void collectSnatPoolMetrics(String[] pools) {
		try {
			LocalLBSNATPoolSNATPoolStatistics stats = iControlInterfaces.getLocalLBSNATPool().get_statistics(pools);
			
			if (stats != null) {
				String poolMetricPrefix = getPoolMetricPrefix();
				Pattern excludePatterns = createPattern(metricExcludes);
				
				for (LocalLBSNATPoolSNATPoolStatisticEntry pool : stats.getStatistics()) {
					String poolName = changePathSeparator(pool.getSnat_pool(), 
							PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);
					
					for (CommonStatistic stat : pool.getStatistics()) {
						if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
							String metricName = String.format("%s%s%s%s", poolMetricPrefix, 
									poolName, METRIC_PATH_SEPARATOR, stat.getType().getValue());
							
							BigInteger value = convertValue(stat.getValue());
							f5Metrics.add(metricName, value);
						}
					}
				}
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching snat pool statistics", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching snat pool statistics", e);
		} 
	}
	
	/*
	 * (non-Javadoc)
	 * Compatible with F5 v11.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__SNATPool__get_all_member_statistics.ashx
	 *
	 */
	private void collectMemberMetrics(String[] pools) {
		if (snatPoolMemberIncludes == null || snatPoolMemberIncludes.isEmpty()) {
			LOGGER.info("No SNAT pool members were included for monitoring.");
			return;
		}
		
		SnatPoolMemberMetricsCollector memberMetricsCollector;
		
		if (preVersion11) {
			memberMetricsCollector = new PreVersion11SnatPoolMemberMetricsCollector(
					snatPoolMemberIncludes, metricExcludes, iControlInterfaces);
		} else {
			memberMetricsCollector = new SnatPoolMemberMetricsCollector(
					snatPoolMemberIncludes, metricExcludes, iControlInterfaces);
		}
		
		memberMetricsCollector.collectMemberMetrics(getPoolMetricPrefix(), pools, f5Metrics);
	}

	private String getPoolMetricPrefix() {
		return String.format("%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, SNAT_POOLS);
	}

}

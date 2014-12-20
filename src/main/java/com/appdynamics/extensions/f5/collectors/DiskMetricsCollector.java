package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.DISKS;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.SYSTEM;
import iControl.Interfaces;
import iControl.SystemDiskLogicalDisk;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.appdynamics.extensions.f5.F5Metrics;
import com.appdynamics.extensions.f5.config.F5;

/**
 * 
 * @author Florencio Sarmiento
 *
 */
public class DiskMetricsCollector implements Callable<F5Metrics> {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.f5.collectors.DiskMetricsCollector");
	
	private F5Metrics f5Metrics = new F5Metrics();
	private Interfaces iControlInterfaces;
	private String f5DisplayName;
	private boolean preVersion11;
	
	public DiskMetricsCollector(Interfaces iControlInterfaces, F5 f5) {
		this.iControlInterfaces = iControlInterfaces;
		this.f5DisplayName = f5.getDisplayName();
		this.preVersion11 = f5.isPreVersion11();
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v10.1
	 * @see https://devcentral.f5.com/wiki/iControl.System__Disk__get_list_of_logical_disks.ashx
	 */
	public F5Metrics call() throws Exception {
		if (preVersion11) {
			LOGGER.info("Disk metrics collector not supported in this version.");
			return f5Metrics;
		}
		
		LOGGER.info("Disk metrics collector started...");
		
		try {
			SystemDiskLogicalDisk[] disks = iControlInterfaces.getSystemDisk().get_list_of_logical_disks();
			
			if (ArrayUtils.isNotEmpty(disks)) {
				collectSpaceAvailableMetrics(disks);
				collectSpaceUsedMetrics(disks);
				
			} else {
				LOGGER.info("No disks info available");
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching disk list", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching disk list", e);
		} 
		
		return f5Metrics;
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v11.0
	 * @see https://devcentral.f5.com/wiki/iControl.System__Disk__get_logical_disk_space_free.ashx
	 */
	private void collectSpaceAvailableMetrics(SystemDiskLogicalDisk[] disks) {
		try {
			long[] diskSpaceAvailable = iControlInterfaces.getSystemDisk().get_logical_disk_space_free(disks);
			
			if (ArrayUtils.isNotEmpty(diskSpaceAvailable)) {
				String diskMetricPrefix = getDiskMetricPrefix();
				int diskIndex = 0;
				
				for (SystemDiskLogicalDisk disk : disks) {
					String metricName = String.format("%s%s%s%s", diskMetricPrefix, disk.getName(),
							METRIC_PATH_SEPARATOR, "Space Available");
					BigInteger value = BigInteger.valueOf(diskSpaceAvailable[diskIndex++]);
					f5Metrics.add(metricName, value);
				}
				
			} else {
				LOGGER.info("No disk space available stat found");
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching disk space available statistics", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching disk space available statistics", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v11.0
	 * @see https://devcentral.f5.com/wiki/iControl.System__Disk__get_logical_disk_space_in_use.ashx
	 */
	private void collectSpaceUsedMetrics(SystemDiskLogicalDisk[] disks) {
		try {
			long[] diskSpaceUsed = iControlInterfaces.getSystemDisk().get_logical_disk_space_in_use(disks);
			
			if (ArrayUtils.isNotEmpty(diskSpaceUsed)) {
				String diskMetricPrefix = getDiskMetricPrefix();
				int diskIndex = 0;
				
				for (SystemDiskLogicalDisk disk : disks) {
					String metricName = String.format("%s%s%s%s", diskMetricPrefix, disk.getName(),
							METRIC_PATH_SEPARATOR, "Space Used");
					BigInteger value = BigInteger.valueOf(diskSpaceUsed[diskIndex++]);
					f5Metrics.add(metricName, value);
				}
				
			}else {
				LOGGER.info("No disk space used stat found");
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching disk space used statistics", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching disk space used statistics", e);
		}
	}
	
	private String getDiskMetricPrefix() {
		return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, 
				SYSTEM, METRIC_PATH_SEPARATOR, DISKS, METRIC_PATH_SEPARATOR);
	}

}

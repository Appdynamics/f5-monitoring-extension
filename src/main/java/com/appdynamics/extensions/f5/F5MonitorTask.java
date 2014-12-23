package com.appdynamics.extensions.f5;

import static com.appdynamics.extensions.f5.F5Constants.DEFAULT_NO_OF_THREADS;
import iControl.Interfaces;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.f5.collectors.CPUMetricsCollector;
import com.appdynamics.extensions.f5.collectors.ClientSSLMetricsCollector;
import com.appdynamics.extensions.f5.collectors.DiskMetricsCollector;
import com.appdynamics.extensions.f5.collectors.HttpCompressionMetricsCollector;
import com.appdynamics.extensions.f5.collectors.HttpMetricsCollector;
import com.appdynamics.extensions.f5.collectors.IRuleMetricsCollector;
import com.appdynamics.extensions.f5.collectors.MemoryMetricsCollector;
import com.appdynamics.extensions.f5.collectors.NetworkInterfaceMetricsCollector;
import com.appdynamics.extensions.f5.collectors.PoolMetricsCollector;
import com.appdynamics.extensions.f5.collectors.ServerSSLMetricsCollector;
import com.appdynamics.extensions.f5.collectors.SnatPoolMetricsCollector;
import com.appdynamics.extensions.f5.collectors.SystemMetricsCollector;
import com.appdynamics.extensions.f5.collectors.TCPMetricsCollector;
import com.appdynamics.extensions.f5.collectors.VirtualServerMetricsCollector;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.google.common.collect.Maps;

/**
 * @author Florencio Sarmiento
 *
 */
public class F5MonitorTask implements Callable<F5Metrics> {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.f5.F5MonitorTask");

	private F5 f5;
	
	private MetricsFilter metricsFilter;
	
	private int noOfThreads;
	
	private Interfaces iControlInterfaces;
	
	private ExecutorService threadPool;
	
	private int TIMEOUT = 30;
	
	public F5MonitorTask(F5 f5, MetricsFilter metricsFilter, int noOfThreads) {
		this.f5 = f5;
		this.metricsFilter = metricsFilter;
		this.noOfThreads = noOfThreads > 0 ? noOfThreads : DEFAULT_NO_OF_THREADS;
	}
	
	public F5Metrics call() throws Exception {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("F5 monitoring task for [%s] has started...",
					f5.getDisplayName()));
		}
		
		F5Metrics f5Metrics = new F5Metrics();
		
		if (!initialise()) {
			LOGGER.error(String.format(
					"Unable to initialise F5 [%s]. Check your connection and credentials." ,
					f5.getDisplayName()));
			
			return f5Metrics;
		}
		
		try {
			threadPool = Executors.newFixedThreadPool(noOfThreads);
			List<Callable<F5Metrics>> metricCollectors = createMetricCollectors();
			CompletionService<F5Metrics> metricCollectorTasks = createConcurrentTasks(metricCollectors);
			collectMetrics(metricCollectorTasks, metricCollectors, f5Metrics);

		} finally {
			if (threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown();
			}
		}
		
		return f5Metrics;
	}
	
	private boolean initialise() {
		iControlInterfaces = new Interfaces();
		iControlInterfaces.initialize(f5.getHostname(), f5.getUsername(), getPassword());
		return checkCredentialsAndSetVersion();
	}
	
	private String getPassword() {
		String password = null;
		
		if (StringUtils.isNotBlank(f5.getPassword())) {
			password = f5.getPassword();
			
		} else {
			try {
				Map<String, String> args = Maps.newHashMap();
				args.put(TaskInputArgs.PASSWORD_ENCRYPTED, f5.getPasswordEncrypted());
				args.put(TaskInputArgs.ENCRYPTION_KEY, f5.getEncryptionKey());
				password = CryptoUtil.getPassword(args);
				
			} catch (IllegalArgumentException e) {
				String msg = "Encryption Key not specified. Please set the value in config.yaml.";
				LOGGER.error(msg);
				throw new IllegalArgumentException(msg);
			}
		}
		
		return password;
	}
	
	private boolean checkCredentialsAndSetVersion() {
		boolean success = false;
		
		try {
			String version = iControlInterfaces.getSystemSystemInfo().get_version();
			f5.setVersion(version);
			LOGGER.info("F5's version is " + version);
			success = true;
			
		} catch (Exception e) {
			if(e.getMessage() != null && e.getMessage().contains("(401)")) {
				LOGGER.error("Unable to connect with the credentials provided.", e);
				
			} else {
				LOGGER.error("An issue occurred while connecting to F5", e);
			}
		}
		
		return success;
	}
	
	private List<Callable<F5Metrics>> createMetricCollectors() {
		List<Callable<F5Metrics>> metricCollectors = new ArrayList<Callable<F5Metrics>>();
		
		PoolMetricsCollector poolMetricsCollector = new PoolMetricsCollector(
				iControlInterfaces, f5, metricsFilter);
		metricCollectors.add(poolMetricsCollector);
		
		SnatPoolMetricsCollector snatPoolMetricsCollector = new SnatPoolMetricsCollector(
				iControlInterfaces, f5, metricsFilter);
		metricCollectors.add(snatPoolMetricsCollector);
		
		VirtualServerMetricsCollector vsMetricsCollector = new VirtualServerMetricsCollector(
				iControlInterfaces, f5, metricsFilter);
		metricCollectors.add(vsMetricsCollector);
		
		IRuleMetricsCollector iRuleMetricsCollector = new IRuleMetricsCollector(
				iControlInterfaces, f5, metricsFilter);
		metricCollectors.add(iRuleMetricsCollector);
		
		ClientSSLMetricsCollector clientSSLMetricsCollector = new ClientSSLMetricsCollector(
				iControlInterfaces, f5, metricsFilter);
		metricCollectors.add(clientSSLMetricsCollector);
		
		ServerSSLMetricsCollector serverSSLMetricsCollector = new ServerSSLMetricsCollector(
				iControlInterfaces, f5, metricsFilter);
		metricCollectors.add(serverSSLMetricsCollector);
		
		NetworkInterfaceMetricsCollector networkInterfaceMetricsCollector = new NetworkInterfaceMetricsCollector(
				iControlInterfaces, f5, metricsFilter);
		metricCollectors.add(networkInterfaceMetricsCollector);
		
		TCPMetricsCollector tcpMetricsCollector = new TCPMetricsCollector(
				iControlInterfaces, f5, metricsFilter);
		metricCollectors.add(tcpMetricsCollector);
		
		HttpMetricsCollector httpMetricsCollector = new HttpMetricsCollector(
				iControlInterfaces, f5, metricsFilter);
		metricCollectors.add(httpMetricsCollector);
		
		HttpCompressionMetricsCollector httpCompressionMetricsCollector = new HttpCompressionMetricsCollector(
				iControlInterfaces, f5, metricsFilter);
		metricCollectors.add(httpCompressionMetricsCollector);
		
		SystemMetricsCollector systemMetricsCollector = new SystemMetricsCollector(iControlInterfaces, f5);
		metricCollectors.add(systemMetricsCollector);
		
		DiskMetricsCollector diskMetricsCollector = new DiskMetricsCollector(iControlInterfaces, f5);
		metricCollectors.add(diskMetricsCollector);
		
		CPUMetricsCollector cpuMetricsCollector = new CPUMetricsCollector(iControlInterfaces, f5);
		metricCollectors.add(cpuMetricsCollector);
		
		MemoryMetricsCollector memoryMetricsCollector = new MemoryMetricsCollector(iControlInterfaces, f5);
		metricCollectors.add(memoryMetricsCollector);	
		
		return metricCollectors;
	}
	
	private CompletionService<F5Metrics> createConcurrentTasks(List<Callable<F5Metrics>> metricCollectors) {
		CompletionService<F5Metrics> metricCollectorTasks = new ExecutorCompletionService<F5Metrics>(threadPool);
		
		for (Callable<F5Metrics> metricCollector : metricCollectors) {
			metricCollectorTasks.submit(metricCollector);	
		}
		
		return metricCollectorTasks;
	}
	
	private void collectMetrics(CompletionService<F5Metrics> parallelTasks, 
			List<Callable<F5Metrics>> metricCollectors, F5Metrics f5Metrics) {
		
		for (int i=0; i< metricCollectors.size(); i++) {
			F5Metrics taskMetrics;
			
			try {
				taskMetrics = parallelTasks.take().get(TIMEOUT, TimeUnit.SECONDS);
				f5Metrics.addAll(taskMetrics.getMetrics());
				
			} catch (InterruptedException e) {
				LOGGER.error("Task interrupted. ", e);
			} catch (ExecutionException e) {
				LOGGER.error("Task execution failed. ", e);
			} catch (TimeoutException e) {
				LOGGER.error("Task timed out. ", e);
			}
		}
	}

}

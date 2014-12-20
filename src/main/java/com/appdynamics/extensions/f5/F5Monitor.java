/**
 * Copyright 2013 AppDynamics
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.f5;

import static com.appdynamics.extensions.f5.F5Constants.DEFAULT_METRIC_PATH;
import static com.appdynamics.extensions.f5.F5Constants.DEFAULT_NO_OF_THREADS;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.util.F5Util.*;
import static com.appdynamics.extensions.yml.YmlReader.readFromFile;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.appdynamics.extensions.f5.config.Configuration;
import com.appdynamics.extensions.f5.config.F5;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class F5Monitor extends AManagedMonitor {

	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.f5.F5Monitor");
	
	private ExecutorService threadPool;
	
	private int TIMEOUT = 60;
	
	public F5Monitor() {
		LOGGER.info(String.format("Using F5 Monitor Version [%s]", 
				getImplementationVersion()));
	}

	public TaskOutput execute(Map<String, String> args,
			TaskExecutionContext arg1) throws TaskExecutionException {
		
		LOGGER.info("Starting F5 Monitoring task");
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Args received were: %s", args));
		}
		
		if (args != null) {
			String configFilename = resolvePath(args.get(F5Constants.CONFIG_ARG));
			
			try {
				Configuration config = readFromFile(configFilename, Configuration.class);
				
				int noOfF5Threads = config.getNumberOfF5Threads() > 0 ? 
						config.getNumberOfF5Threads() : DEFAULT_NO_OF_THREADS;
				threadPool = Executors.newFixedThreadPool(noOfF5Threads);
				
				CompletionService<F5Metrics> f5monitorTasks = createConcurrentTasks(config);
				F5Metrics f5Metrics = collectMetrics(f5monitorTasks, config.getF5s().size());
				uploadMetrics(f5Metrics, getMetricPrefix(config));
				
				return new TaskOutput("F5 Monitoring task successfully completed");
				
			} catch (Exception ex) {
				LOGGER.error("Unfortunately an issue has occurred: ", ex);
				
			} finally {
				if (threadPool != null && !threadPool.isShutdown()) {
					threadPool.shutdown();
				}
			}
			
		}
		
		throw new TaskExecutionException("F5 Monitoring task completed with failures.");
	}
	
	private CompletionService<F5Metrics> createConcurrentTasks(Configuration config) {
		CompletionService<F5Metrics> f5MonitorTasks = new ExecutorCompletionService<F5Metrics>(threadPool);
		
		for (F5 f5 : config.getF5s()) {
			F5MonitorTask task = new F5MonitorTask(f5, config.getMetricsFilter(), config.getNumberOfThreadsPerF5());
			f5MonitorTasks.submit(task);
		}
		
		return f5MonitorTasks;
	}
	
	private F5Metrics collectMetrics(CompletionService<F5Metrics> parallelTasks,
			int noOfF5MonitorTasks) {
		
		F5Metrics f5Metrics = new F5Metrics();
		
		for (int i=0; i< noOfF5MonitorTasks; i++) {
			F5Metrics collectedMetrics;
			
			try {
				collectedMetrics = parallelTasks.take().get(TIMEOUT, TimeUnit.SECONDS);
				f5Metrics.addAll(collectedMetrics.getMetrics());
				
			} catch (InterruptedException e) {
				LOGGER.error("Task interrupted. ", e);
				
			} catch (ExecutionException e) {
				LOGGER.error("Task execution failed. ", e);
				
			} catch (TimeoutException e) {
				LOGGER.error("Task timed out. ", e);
			}
		}
		
		return f5Metrics;
	}
	
	private String getMetricPrefix(Configuration config) {
		String metricPrefix = config.getMetricPrefix();
		
		if (StringUtils.isBlank(metricPrefix)) {
			metricPrefix = DEFAULT_METRIC_PATH;
			
		} else {
			metricPrefix = metricPrefix.trim();
			
			if (!metricPrefix.endsWith(METRIC_PATH_SEPARATOR)) {
				metricPrefix = metricPrefix + METRIC_PATH_SEPARATOR;
			}
		}
		
		return metricPrefix;
	}
	
	private void uploadMetrics(F5Metrics f5Metrics, String metricPrefix) {
		for (Map.Entry<String, BigInteger> metric : f5Metrics.getMetrics().entrySet()) {
			printCollectiveObservedCurrent(metricPrefix + metric.getKey(), metric.getValue());
		}
	}
	
    private void printCollectiveObservedCurrent(String metricName, BigInteger metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }
    
    private void printMetric(String metricName, BigInteger metricValue, String aggregation, 
    		String timeRollup, String cluster) {
    	
		MetricWriter metricWriter = getMetricWriter(metricName, aggregation,
				timeRollup, cluster);
        
        BigInteger valueToReport = convertValueToZeroIfNullOrNegative(metricValue);
        
        if (LOGGER.isDebugEnabled()) {
        	LOGGER.debug(String.format("Sending [%s/%s/%s] metric = %s = %s => %s",
            		aggregation, timeRollup, cluster,
                    metricName, metricValue, valueToReport));
        }
        
        metricWriter.printMetric(valueToReport.toString());
    }
    
	private static String getImplementationVersion() {
		return F5Monitor.class.getPackage().getImplementationTitle();
	}
}

package com.appdynamics.extensions.f5;

import static com.appdynamics.extensions.f5.F5Constants.DEFAULT_NO_OF_THREADS;

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
import iControl.Interfaces;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Florencio Sarmiento
 * @author Satish M
 */
public class F5MonitorTask implements Callable<Boolean> {

    public static final Logger LOGGER = Logger.getLogger(F5MonitorTask.class);

    private F5 f5;


    private MetricsFilter metricsFilter;

    private int noOfThreads;

    private Interfaces iControlInterfaces;

    private ExecutorService threadPool;

    private F5Monitor monitor;
    private String metricPrefix;

    private int f5ThreadTimeout = 30;

    public F5MonitorTask(F5Monitor monitor, String metricPrefix, Interfaces iControlInterfaces, F5 f5, MetricsFilter metricsFilter, int noOfThreads, int f5ThreadTimeout) {
        this.monitor = monitor;
        this.metricPrefix = metricPrefix;
        this.f5 = f5;
        this.metricsFilter = metricsFilter;
        this.noOfThreads = noOfThreads > 0 ? noOfThreads : DEFAULT_NO_OF_THREADS;
        this.iControlInterfaces = iControlInterfaces;
        this.f5ThreadTimeout = f5ThreadTimeout;
    }

    public Boolean call() {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("F5 monitoring task for [%s] has started...",
                    f5.getDisplayName()));
        }

        if (!initialise()) {
            LOGGER.error(String.format(
                    "Unable to initialise F5 [%s]. Check your connection and credentials.",
                    f5.getDisplayName()));

            return Boolean.FALSE;
        }


        try {
            threadPool = Executors.newFixedThreadPool(noOfThreads);
            List<Callable<Void>> metricCollectors = createMetricCollectors();
            runConcurrentTasks(metricCollectors);

        } finally {
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
            }
        }

        return Boolean.TRUE;
    }

    public Boolean callSequential() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("F5 monitoring task has started initialization...");
        }

        if (!initialise()) {
            LOGGER.error(String.format(
                    "Unable to initialise F5 [%s]. Check your connection and credentials.",
                    f5.getDisplayName()));

            return Boolean.FALSE;
        }

        List<Callable<Void>> metricCollectors = createMetricCollectors();

        for (Callable<Void> metricCollector : metricCollectors) {
            try {
                metricCollector.call();
            } catch (Exception e) {
                LOGGER.error("Error in initializing F5 monitor", e);
            }
        }
        return Boolean.TRUE;
    }

    private boolean initialise() {
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
            if (e.getMessage() != null && e.getMessage().contains("(401)")) {
                LOGGER.error("Unable to connect with the credentials provided.", e);

            } else {
                LOGGER.error("An issue occurred while connecting to F5", e);
            }
        }

        return success;
    }

    private List<Callable<Void>> createMetricCollectors() {
        List<Callable<Void>> metricCollectors = new ArrayList<Callable<Void>>();

        PoolMetricsCollector poolMetricsCollector = new PoolMetricsCollector(
                iControlInterfaces, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(poolMetricsCollector);

        SnatPoolMetricsCollector snatPoolMetricsCollector = new SnatPoolMetricsCollector(
                iControlInterfaces, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(snatPoolMetricsCollector);

        VirtualServerMetricsCollector vsMetricsCollector = new VirtualServerMetricsCollector(
                iControlInterfaces, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(vsMetricsCollector);

        IRuleMetricsCollector iRuleMetricsCollector = new IRuleMetricsCollector(
                iControlInterfaces, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(iRuleMetricsCollector);

        ClientSSLMetricsCollector clientSSLMetricsCollector = new ClientSSLMetricsCollector(
                iControlInterfaces, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(clientSSLMetricsCollector);

        ServerSSLMetricsCollector serverSSLMetricsCollector = new ServerSSLMetricsCollector(
                iControlInterfaces, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(serverSSLMetricsCollector);

        NetworkInterfaceMetricsCollector networkInterfaceMetricsCollector = new NetworkInterfaceMetricsCollector(
                iControlInterfaces, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(networkInterfaceMetricsCollector);

        TCPMetricsCollector tcpMetricsCollector = new TCPMetricsCollector(
                iControlInterfaces, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(tcpMetricsCollector);

        HttpMetricsCollector httpMetricsCollector = new HttpMetricsCollector(
                iControlInterfaces, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(httpMetricsCollector);

        HttpCompressionMetricsCollector httpCompressionMetricsCollector = new HttpCompressionMetricsCollector(
                iControlInterfaces, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(httpCompressionMetricsCollector);

        SystemMetricsCollector systemMetricsCollector = new SystemMetricsCollector(iControlInterfaces, f5, monitor, metricPrefix);
        metricCollectors.add(systemMetricsCollector);

        DiskMetricsCollector diskMetricsCollector = new DiskMetricsCollector(iControlInterfaces, f5, monitor, metricPrefix);
        metricCollectors.add(diskMetricsCollector);

        CPUMetricsCollector cpuMetricsCollector = new CPUMetricsCollector(iControlInterfaces, f5, monitor, metricPrefix);
        metricCollectors.add(cpuMetricsCollector);

        MemoryMetricsCollector memoryMetricsCollector = new MemoryMetricsCollector(iControlInterfaces, f5, monitor, metricPrefix);
        metricCollectors.add(memoryMetricsCollector);

        return metricCollectors;
    }

    private void runConcurrentTasks(List<Callable<Void>> metricCollectors) {

        List<Future<Void>> futures = new ArrayList<Future<Void>>();

        for (Callable<Void> metricCollector : metricCollectors) {
            futures.add(threadPool.submit(metricCollector));
        }

        for (Future<Void> future : futures) {
            try {
                future.get(f5ThreadTimeout, TimeUnit.SECONDS);
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

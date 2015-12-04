/**
 * Copyright 2013 AppDynamics
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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
import static com.appdynamics.extensions.f5.util.F5Util.resolvePath;
import static com.appdynamics.extensions.yml.YmlReader.readFromFile;

import com.appdynamics.extensions.f5.config.Configuration;
import com.appdynamics.extensions.f5.config.F5;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import iControl.Interfaces;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class F5Monitor extends AManagedMonitor {

    public static final Logger LOGGER = Logger.getLogger(F5Monitor.class);

    private ExecutorService threadPool;

    final private Interfaces iControlInterfaces;

    private STATE state = STATE.NOT_INITIALIZED;

    public F5Monitor() {
        LOGGER.info(String.format("Using F5 Monitor Version [%s]",
                getImplementationVersion()));
        iControlInterfaces = new Interfaces();
    }

    private enum STATE {
        NOT_INITIALIZED("Not Initialized"), INITIALIZING("Initializing"), INITIALIZED("Initialized");

        private String state;

        STATE(String s) {
            this.state = s;
        }

        public String getState() {
            return state;
        }
    }

    public TaskOutput execute(Map<String, String> args,
                              TaskExecutionContext arg1) throws TaskExecutionException {

        LOGGER.info("Starting F5 Monitoring task");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Args received were: %s", args));
        }

        if (args == null || args.size() <= 0) {

            LOGGER.error("Config file not provided. Exiting the task");
        } else {

            String configFilename = resolvePath(args.get(F5Constants.CONFIG_ARG));

            try {
                Configuration config = readFromFile(configFilename, Configuration.class);

                //As multiple threads are trying to load the classes, CPU is spiking up. As a workaround executing only 1 thread with 1 F5 as initialization process.
                if (state == STATE.NOT_INITIALIZED) {

                    init(config);
                    return new TaskOutput("Initializing F5 Monitoring");
                } else if (state == STATE.INITIALIZED) {

                    int noOfF5Threads = config.getNumberOfF5Threads() > 0 ?
                            config.getNumberOfF5Threads() : DEFAULT_NO_OF_THREADS;
                    threadPool = createThreadPool(noOfF5Threads);

                    runConcurrentTasks(config);

                    return new TaskOutput("F5 Monitoring task successfully completed");
                } else {
                    LOGGER.info("Initializing F5 Monitoring");
                    return new TaskOutput("Initializing F5 Monitoring");
                }

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

    private void init(Configuration config) {
        ExecutorService executorService = null;
        state = STATE.INITIALIZING;
        try {
            List<F5> f5s = config.getF5s();

            if (f5s != null && f5s.size() > 0) {
                F5 f5 = f5s.get(0);
                F5MonitorTask task = new F5MonitorTask(this, getMetricPrefix(config), iControlInterfaces, f5, config.getMetricsFilter(), config.getNumberOfThreadsPerF5(), config.getF5ThreadTimeout());

                final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                        .setNameFormat("F5Monitor-Task-Initializer-Thread-%d")
                        .build();
                executorService = Executors.newSingleThreadExecutor(threadFactory);
                executorService.submit(task);

            } else {
                LOGGER.error("No F5's configured in config.yaml");
                throw new TaskExecutionException("No F5's configured in config.yaml");
            }
            state = STATE.INITIALIZED;
        } catch (Exception e) {
            LOGGER.error("Error while initializing the task", e);
            state = STATE.NOT_INITIALIZED;
        } finally {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }

    }

    private ExecutorService createThreadPool(int numOfThreads) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("F5Monitor-Task-Thread-%d")
                .build();
        return Executors.newFixedThreadPool(numOfThreads,
                threadFactory);
    }

    private void runConcurrentTasks(Configuration config) {
        for (F5 f5 : config.getF5s()) {
            F5MonitorTask task = new F5MonitorTask(this, getMetricPrefix(config), iControlInterfaces, f5, config.getMetricsFilter(), config.getNumberOfThreadsPerF5(), config.getF5ThreadTimeout());
            threadPool.submit(task);
        }
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

    private static String getImplementationVersion() {
        return F5Monitor.class.getPackage().getImplementationTitle();
    }
}

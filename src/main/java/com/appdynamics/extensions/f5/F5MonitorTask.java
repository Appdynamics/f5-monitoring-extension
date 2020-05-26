package com.appdynamics.extensions.f5;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.f5.config.input.Stat;

import java.util.Map;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class F5MonitorTask implements AMonitorTaskRunnable {
    private MonitorContextConfiguration configuration;
    private MetricWriteHelper metricWriteHelper;
    private Map<String, ?> server;
    private Stat[] stats;
    private String token;
    public  F5MonitorTask(MonitorContextConfiguration configuration, MetricWriteHelper metricWriteHelper, Map<String, ?> server, Stat[] stats, String token){
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
        this.server = server;
        this.stats = stats;
        this.token = token;
    }
    public void run() {
//        String token = tokenFetcher.getToken(server);
        for (Stat stat : stats) {
            NewF5MonitorTask task = new NewF5MonitorTask(configuration, metricWriteHelper, server, stat,token);
            configuration.getContext().getExecutorService().submit("NewF5MonitorTask", task);
        }
    }
    public void onTaskComplete() {

    }

}

package com.appdynamics.extensions.f5.models;

import java.math.BigInteger;

/**
 * @author Satish Muddam
 */
public class HostCPUMemoryStats {
    private String hostId;
    private int cpuCount;
    private BigInteger oneMinAvgIdle;
    private BigInteger memoryTotal;
    private BigInteger memoryUsed;


    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public int getCpuCount() {
        return cpuCount;
    }

    public void setCpuCount(int cpuCount) {
        this.cpuCount = cpuCount;
    }

    public BigInteger getOneMinAvgIdle() {
        return oneMinAvgIdle;
    }

    public void setOneMinAvgIdle(BigInteger oneMinAvgIdle) {
        this.oneMinAvgIdle = oneMinAvgIdle;
    }

    public BigInteger getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(BigInteger memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public BigInteger getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(BigInteger memoryUsed) {
        this.memoryUsed = memoryUsed;
    }
}

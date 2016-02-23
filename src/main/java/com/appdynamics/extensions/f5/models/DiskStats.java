package com.appdynamics.extensions.f5.models;

import java.math.BigInteger;

/**
 * @author Satish Muddam
 */
public class DiskStats {

    private String name;
    private BigInteger size;
    private BigInteger free;
    private BigInteger inUse;
    private BigInteger reserved;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigInteger getSize() {
        return size;
    }

    public void setSize(BigInteger size) {
        this.size = size;
    }

    public BigInteger getFree() {
        return free;
    }

    public void setFree(BigInteger free) {
        this.free = free;
    }

    public BigInteger getInUse() {
        return inUse;
    }

    public void setInUse(BigInteger inUse) {
        this.inUse = inUse;
    }

    public BigInteger getReserved() {
        return reserved;
    }

    public void setReserved(BigInteger reserved) {
        this.reserved = reserved;
    }
}

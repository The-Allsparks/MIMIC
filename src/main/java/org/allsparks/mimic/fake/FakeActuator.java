package org.allsparks.mimic.fake;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory actuator for tests. Phase 0 code must never call
 * {@link #setPower(double)} or {@link #setPosition(double)}.
 */
public final class FakeActuator {
    private final AtomicReference<Double> power = new AtomicReference<>(0.0);
    private final AtomicReference<Double> servoPosition = new AtomicReference<>(Double.NaN);
    private final AtomicInteger powerWrites = new AtomicInteger();
    private final AtomicInteger servoWrites = new AtomicInteger();
    private volatile double ticks;
    private volatile double ticksPerSecond;
    private volatile double currentAmps = Double.NaN;

    public void setPower(double power) {
        this.power.set(power);
        powerWrites.incrementAndGet();
    }

    public void setPosition(double servoPosition) {
        this.servoPosition.set(servoPosition);
        servoWrites.incrementAndGet();
    }

    public double power() {
        return power.get();
    }

    public double servoPosition() {
        return servoPosition.get();
    }

    public int powerWriteCount() {
        return powerWrites.get();
    }

    public int servoWriteCount() {
        return servoWrites.get();
    }

    public int outputWriteCount() {
        return powerWriteCount() + servoWriteCount();
    }

    public void simulateMotion(double ticks, double ticksPerSecond) {
        this.ticks = ticks;
        this.ticksPerSecond = ticksPerSecond;
    }

    public void simulateCurrentAmps(double currentAmps) {
        this.currentAmps = currentAmps;
    }

    public double ticks() {
        return ticks;
    }

    public double ticksPerSecond() {
        return ticksPerSecond;
    }

    public double currentAmps() {
        return currentAmps;
    }
}

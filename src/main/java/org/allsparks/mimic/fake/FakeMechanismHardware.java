package org.allsparks.mimic.fake;

import org.allsparks.mimic.clock.MimicClock;
import org.allsparks.mimic.observe.MechanismObserver;
import org.allsparks.mimic.units.MechanismUnits;

/** Convenience bundle of fake sensors and a read-only observer. */
public final class FakeMechanismHardware {
    private final FakeActuator actuator = new FakeActuator();
    private final FakeLimitSwitch lowerLimit = new FakeLimitSwitch();
    private final FakeLimitSwitch upperLimit = new FakeLimitSwitch();
    private final FakeAbsoluteSensor absolute = new FakeAbsoluteSensor();
    private final MechanismObserver observer;

    public FakeMechanismHardware(String mechanismId, MimicClock clock, MechanismUnits units) {
        this.observer = MechanismObserver.builder(mechanismId, clock, units)
                .ticks(actuator::ticks)
                .ticksPerSecond(actuator::ticksPerSecond)
                .requestedOutput(actuator::power)
                .appliedOutput(actuator::power)
                .currentAmps(actuator::currentAmps)
                .lowerLimit(lowerLimit::rawState, false)
                .upperLimit(upperLimit::rawState, false)
                .absoluteSensor(absolute::value, units.canonicalUnitSymbol())
                .build();
    }

    public FakeActuator actuator() {
        return actuator;
    }

    public FakeLimitSwitch lowerLimit() {
        return lowerLimit;
    }

    public FakeLimitSwitch upperLimit() {
        return upperLimit;
    }

    public FakeAbsoluteSensor absolute() {
        return absolute;
    }

    public MechanismObserver observer() {
        return observer;
    }
}

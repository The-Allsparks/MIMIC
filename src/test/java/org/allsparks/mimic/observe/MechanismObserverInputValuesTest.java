package org.allsparks.mimic.observe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.allsparks.contracts.input.InputPriority;
import org.allsparks.contracts.input.InputRegistrar;
import org.allsparks.contracts.input.InputValues;
import org.allsparks.contracts.input.Sample;
import org.allsparks.contracts.input.SamplingPolicy;
import org.allsparks.contracts.input.SignalKey;
import org.allsparks.contracts.observation.Validity;
import org.allsparks.mimic.input.MimicSignals;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class MechanismObserverInputValuesTest {
    @Test
    void readFromUsesPublishedSampleAndDoesNotTouchTheSupplier() {
        AtomicInteger reads = new AtomicInteger();
        PublishedInputs inputs = new PublishedInputs();
        inputs.putDouble(MimicSignals.position("elev"), 250.0);
        MechanismObserver.Builder builder = MechanismObserver.builder(
                        "elev",
                        () -> 10L,
                        MechanismUnits.linearMillimeters("elev", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> {
                    reads.incrementAndGet();
                    return 1.0;
                });
        builder.declareInputs(inputs);
        MechanismObserver observer = builder.readFrom(inputs).build();
        MechanismSnapshot snapshot = observer.capture();
        assertEquals(250.0, snapshot.position(), 1e-9);
        assertEquals(MeasurementValidity.VALID, snapshot.positionSample().validity());
        assertEquals(0, reads.get());
        assertEquals(MimicSignals.position("elev"), inputs.lastRequired);
    }

    @Test
    void publishedMissingIsNotZero() {
        PublishedInputs inputs = new PublishedInputs();
        inputs.missing(MimicSignals.position("arm"));
        MechanismObserver.Builder builder = MechanismObserver.builder(
                        "arm",
                        () -> 1L,
                        MechanismUnits.rotaryRadians("arm", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 99.0);
        builder.declareInputs(inputs);
        MechanismSnapshot snapshot = builder.readFrom(inputs).build().capture();
        assertTrue(Double.isNaN(snapshot.position()));
        assertEquals(MeasurementValidity.MISSING, snapshot.positionSample().validity());
    }

    @Test
    void declareInputsWithoutReadFromIsRejected() {
        PublishedInputs inputs = new PublishedInputs();
        assertThrows(
                IllegalStateException.class,
                () -> MechanismObserver.builder(
                                "intake",
                                () -> 0L,
                                MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                        .ticks(() -> 0.0)
                        .declareInputs(inputs)
                        .build());
    }

    @Test
    void standaloneCaptureStillCallsTheSupplier() {
        AtomicInteger reads = new AtomicInteger();
        MechanismObserver observer = MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> {
                    reads.incrementAndGet();
                    return 40.0;
                })
                .build();
        assertEquals(40.0, observer.capture().position(), 1e-9);
        assertEquals(1, reads.get());
    }

    private static final class PublishedInputs implements InputRegistrar, InputValues {
        private final Map<SignalKey<?>, Sample<?>> samples = new HashMap<>();
        private SignalKey<?> lastRequired;

        void putDouble(SignalKey<Double> key, double value) {
            samples.put(key, Sample.of(Double.valueOf(value), Validity.VALID, 25L, 1L, true, null));
        }

        void missing(SignalKey<?> key) {
            samples.put(key, Sample.missing());
        }

        @Override
        public void require(SignalKey<?> key, SamplingPolicy policy, InputPriority priority) {
            lastRequired = key;
        }

        @Override
        public void requireGroup(
                String groupId, SamplingPolicy policy, InputPriority priority, SignalKey<?>... members) {}

        @Override
        public long currentCycleId() {
            return 1L;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Sample<T> get(SignalKey<T> key) {
            Sample<?> sample = samples.get(key);
            if (sample == null) {
                return Sample.missing();
            }
            return (Sample<T>) sample;
        }

        @Override
        public int getInt(SignalKey<Integer> key) {
            return 0;
        }

        @Override
        public long getLong(SignalKey<Long> key) {
            return 0L;
        }

        @Override
        public double getDouble(SignalKey<Double> key) {
            Sample<Double> sample = get(key);
            Double boxed = sample.orNull();
            return boxed == null ? 0.0 : boxed.doubleValue();
        }

        @Override
        public boolean getBoolean(SignalKey<Boolean> key) {
            return false;
        }
    }
}

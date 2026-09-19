package org.allsparks.mimic.log;

/**
 * Optional observer of MIMIC events. TRACE, tests, or DS glue implement this.
 * MIMIC does not import TRACE. Default is {@link #NOOP}.
 *
 * <p>Called on the OpMode thread from {@code observe()} / {@code requestGoal()} /
 * {@code stop()}. Must not block, write files, or command actuators.
 */
public interface MimicEventSink {
    void onEvent(MimicEvent event);

    MimicEventSink NOOP = new MimicEventSink() {
        @Override
        public void onEvent(MimicEvent event) {
            // intentionally empty
        }
    };
}

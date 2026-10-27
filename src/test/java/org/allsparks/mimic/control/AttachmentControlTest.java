package org.allsparks.mimic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;
import org.allsparks.mimic.MimicSession;
import org.allsparks.mimic.api.GoalDisposition;
import org.allsparks.mimic.api.GoalResult;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.log.MimicEventType;
import org.allsparks.mimic.observe.MechanismObserver;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class AttachmentControlTest {

    @Test
    void unboundSessionDoesNotWriteAndRejectsAttachmentGoals() {
        FakeActuator hardware = new FakeActuator();
        MimicSession session = MimicSession.create(observer(hardware));
        GoalResult result = session.requestAttachment(AttachmentGoal.effort(1.0));
        session.observe();
        session.stop();
        assertFalse(result.accepted());
        assertEquals(MimicSession.NO_ACTIVE_CONTROL, result.reason());
        assertEquals(0, hardware.outputWriteCount());
    }

    @Test
    void boundSessionStillRejectsNumericGoals() {
        RecordingSink sink = new RecordingSink();
        MimicSession session = boundEffort(sink, () -> 0.0, () -> 0.0);
        session.observe();
        GoalResult result = session.requestGoal(50.0);
        assertFalse(result.accepted());
        assertEquals(GoalDisposition.REJECTED, result.disposition());
        assertEquals(MimicSession.NO_ACTIVE_CONTROL, result.reason());
    }

    @Test
    void effortAboveFullScaleIsClampedAndLogged() {
        RecordingSink sink = new RecordingSink();
        MimicSession session = boundEffort(sink, () -> 0.0, () -> 0.0);
        session.requestAttachment(AttachmentGoal.effort(2.0));
        session.observe();
        assertEquals(ActuatorCommand.Kind.EFFORT, sink.last.kind());
        assertEquals(1.0, sink.last.effort(), 1e-9);
        assertTrue(session.logger().exportCsv().contains(MimicEventType.SAFETY_GATE.name()));
        assertTrue(session.logger().exportCsv().contains(ActuatorSafetyGate.CLAMPED));
    }

    @Test
    void velocityHoldStopsWhenTheSampleIsMissing() {
        RecordingSink sink = new RecordingSink();
        VelocityHoldLaw law = new VelocityHoldLaw(70.0);
        MimicSession session = MimicSession.create(observer(new FakeActuator(), () -> 0.0, () -> Double.NaN))
                .controlWith(new AttachmentControl(
                        law,
                        ActuatorSafetyGate.effortAndVelocity(1.0, 2800.0),
                        sink,
                        AttachmentContext.open()));
        session.requestAttachment(AttachmentGoal.holdVelocity(2100.0));
        session.observe();
        assertEquals(ActuatorCommand.Kind.STOP, sink.last.kind());
        assertFalse(law.atSpeed());
    }

    @Test
    void velocityHoldCommandsTheTargetWhenTheWheelIsInBand() {
        RecordingSink sink = new RecordingSink();
        VelocityHoldLaw law = new VelocityHoldLaw(70.0);
        MimicSession session = MimicSession.create(observer(new FakeActuator(), () -> 10.0, () -> 2100.0))
                .controlWith(new AttachmentControl(
                        law,
                        ActuatorSafetyGate.effortAndVelocity(1.0, 2800.0),
                        sink,
                        AttachmentContext.open()));
        session.requestAttachment(AttachmentGoal.holdVelocity(2100.0));
        session.observe();
        assertEquals(ActuatorCommand.Kind.VELOCITY, sink.last.kind());
        assertEquals(2100.0, sink.last.velocity(), 1e-9);
        assertTrue(law.atSpeed());
    }

    @Test
    void indexRequiresLauncherReadyAndDoesNotRepeatWhileHeld() {
        AtomicReference<Double> ticks = new AtomicReference<>(0.0);
        RecordingSink sink = new RecordingSink();
        AttachmentContext context = AttachmentContext.blocked();
        DiscreteIndexLaw law = new DiscreteIndexLaw(537.7, 0.8);
        MimicSession session = MimicSession.create(observer(new FakeActuator(), ticks::get, () -> 0.0))
                .controlWith(new AttachmentControl(
                        law, ActuatorSafetyGate.effortAndPosition(1.0), sink, context));

        session.requestAttachment(AttachmentGoal.trigger(1.0));
        session.observe();
        assertEquals(0, sink.last.positionTicks());
        assertEquals(0.0, sink.last.power(), 1e-9);

        context.permit(true);
        session.observe();
        assertEquals(0, sink.last.positionTicks());

        session.requestAttachment(AttachmentGoal.trigger(0.0));
        session.observe();
        session.requestAttachment(AttachmentGoal.trigger(1.0));
        session.observe();
        assertEquals(ActuatorCommand.Kind.POSITION, sink.last.kind());
        assertEquals(179, sink.last.positionTicks());
        assertEquals(0.8, sink.last.power(), 1e-9);

        session.observe();
        assertEquals(179, sink.last.positionTicks());
    }

    @Test
    void stopParksTheSink() {
        RecordingSink sink = new RecordingSink();
        MimicSession session = boundEffort(sink, () -> 0.0, () -> 0.0);
        session.requestAttachment(AttachmentGoal.effort(0.4));
        session.observe();
        session.stop();
        assertEquals(ActuatorCommand.Kind.STOP, sink.last.kind());
    }

    private static MimicSession boundEffort(RecordingSink sink, DoubleSupplier ticks, DoubleSupplier velocity) {
        return MimicSession.create(observer(new FakeActuator(), ticks, velocity))
                .controlWith(new AttachmentControl(
                        new OpenLoopLaw(),
                        ActuatorSafetyGate.effortAndPosition(1.0),
                        sink,
                        AttachmentContext.open()));
    }

    private static MechanismObserver observer(FakeActuator actuator) {
        return observer(actuator, actuator::ticks, actuator::ticksPerSecond);
    }

    private static MechanismObserver observer(
            FakeActuator actuator, DoubleSupplier ticks, DoubleSupplier velocity) {
        return MechanismObserver.builder(
                        "attachment",
                        () -> 1_000L,
                        MechanismUnits.rotaryRadians("attachment", 1.0, DirectionSign.POSITIVE))
                .ticks(ticks)
                .ticksPerSecond(velocity)
                .requestedOutput(actuator::power)
                .build();
    }

    private static final class RecordingSink implements ActuatorSink {
        private ActuatorCommand last = ActuatorCommand.stop();

        @Override
        public void write(ActuatorCommand command) {
            last = command;
        }
    }
}

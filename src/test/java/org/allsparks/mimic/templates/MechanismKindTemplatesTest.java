package org.allsparks.mimic.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class MechanismKindTemplatesTest {

    @Test
    void everyFamilyHasAtLeastOneConstruct() {
        for (MechanismFamily family : MechanismFamily.values()) {
            assertFalse(MechanismConstruct.ofFamily(family).isEmpty(), family.name());
        }
    }

    @Test
    void everyStandardConstructHasAFamily() {
        for (MechanismConstruct construct : MechanismConstruct.values()) {
            assertTrue(
                    MechanismConstruct.ofFamily(construct.family()).contains(construct),
                    construct.name());
        }
    }

    @Test
    void ofFamilyReturnsOnlyThatFamily() {
        for (MechanismFamily family : MechanismFamily.values()) {
            for (MechanismConstruct construct : MechanismConstruct.ofFamily(family)) {
                assertEquals(family, construct.family());
            }
        }
    }

    @Test
    void transferIsTheInternalPathFamilyNotShoot() {
        assertEquals(
                "Move a piece along a path inside the robot",
                MechanismFamily.TRANSFER.purpose());
        for (MechanismFamily family : MechanismFamily.values()) {
            assertFalse(family.name().contains("SHOOT"));
        }
        List<MechanismConstruct> transfer = MechanismConstruct.ofFamily(MechanismFamily.TRANSFER);
        assertTrue(transfer.contains(MechanismConstruct.BELT));
        assertTrue(transfer.contains(MechanismConstruct.ROLLER_PATH));
        assertTrue(transfer.contains(MechanismConstruct.INDEXER));
        assertTrue(transfer.contains(MechanismConstruct.FEEDER));
    }

    @Test
    void launcherConstructsAreEnergyNotAim() {
        List<MechanismConstruct> launcher = MechanismConstruct.ofFamily(MechanismFamily.LAUNCHER);
        assertTrue(launcher.contains(MechanismConstruct.FLYWHEEL));
        assertTrue(launcher.contains(MechanismConstruct.CATAPULT));
        assertTrue(launcher.contains(MechanismConstruct.SPINAPULT));
        assertTrue(launcher.contains(MechanismConstruct.PUNCHER));
        assertFalse(launcher.contains(MechanismConstruct.TURRET));
        assertFalse(launcher.contains(MechanismConstruct.HOOD));
        assertTrue(MechanismConstruct.FLYWHEEL.isLaunchEnergy());
        assertTrue(MechanismConstruct.PUNCHER.isLaunchEnergy());
        assertFalse(MechanismConstruct.TURRET.isLaunchEnergy());
        assertTrue(MechanismConstruct.TURRET.isLauncherAim());
        assertTrue(MechanismConstruct.HOOD.isLauncherAim());
        assertEquals(MechanismFamily.ARM, MechanismConstruct.TURRET.family());
        assertEquals(MechanismFamily.ARM, MechanismConstruct.HOOD.family());
    }

    @Test
    void elevatorIsALiftConstruct() {
        assertEquals(MechanismFamily.LIFT, MechanismConstruct.ELEVATOR.family());
        assertEquals(MechanismMotionKind.POSITIONED, MechanismConstruct.ELEVATOR.motionKind());
        assertTrue(MechanismConstruct.ofFamily(MechanismFamily.LIFT).contains(MechanismConstruct.LINEAR_SLIDE));
        assertTrue(MechanismConstruct.ofFamily(MechanismFamily.LIFT).contains(MechanismConstruct.CAPSTAN));
        assertTrue(MechanismConstruct.ofFamily(MechanismFamily.LIFT).contains(MechanismConstruct.EXTENSION));
        assertTrue(MechanismConstruct.ofFamily(MechanismFamily.LIFT).contains(MechanismConstruct.LEAD_SCREW));
    }

    @Test
    void clawIsEndEffectorNotIntake() {
        assertEquals(MechanismFamily.END_EFFECTOR, MechanismConstruct.CLAW.family());
        List<MechanismConstruct> intake = MechanismConstruct.ofFamily(MechanismFamily.INTAKE);
        assertTrue(intake.contains(MechanismConstruct.ROLLER_INTAKE));
        assertTrue(intake.contains(MechanismConstruct.SPATULA));
        assertFalse(intake.contains(MechanismConstruct.CLAW));
    }

    @Test
    void blueprintKeepsIdAndConstruct() {
        MechanismBlueprint turret = MechanismBlueprint.of("aim", MechanismConstruct.TURRET);
        assertEquals("aim", turret.mechanismId());
        assertEquals(MechanismConstruct.TURRET, turret.construct());
        assertEquals(MechanismFamily.ARM, turret.family());
        assertEquals("aim[TURRET]", turret.toString());
        assertEquals(turret, MechanismBlueprint.of("aim", MechanismConstruct.TURRET));
    }

    @Test
    void blueprintRejectsEmptyId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MechanismBlueprint.of("", MechanismConstruct.ELEVATOR));
        assertThrows(
                NullPointerException.class,
                () -> MechanismBlueprint.of("lift", null));
    }

    @Test
    void catalogDoesNotNameASeasonOrRobot() {
        EnumSet<MechanismFamily> families = EnumSet.allOf(MechanismFamily.class);
        assertEquals(9, families.size());
        for (MechanismFamily family : MechanismFamily.values()) {
            assertNoSeasonLeak(family.name());
            assertNoSeasonLeak(family.purpose());
        }
        for (MechanismConstruct construct : MechanismConstruct.values()) {
            assertNoSeasonLeak(construct.name());
            assertNoSeasonLeak(construct.summary());
        }
    }

    private static void assertNoSeasonLeak(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        assertFalse(lower.contains("biobuzz"), text);
        assertFalse(lower.contains("nectar"), text);
        assertFalse(lower.contains("decode"), text);
        assertFalse(lower.contains("bumblebee"), text);
        assertFalse(lower.contains("flower"), text);
        assertFalse(lower.contains("pollen"), text);
        assertFalse(lower.contains("artifact"), text);
    }
}

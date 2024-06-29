package org.team100.lib.commands.drivetrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.team100.lib.motion.drivetrain.Fixtured;

class DriveRotationTest extends Fixtured {
    private static final double kDelta = 0.001;
    private final double desiredRotation = 1;

    @Test
    void testSimple() {
        Supplier<Double> rot = () -> desiredRotation;
        DriveRotation command = new DriveRotation(fixture.drive, rot);
        DriveRotation.shutDownForTest();
        command.initialize();
        assertEquals(0, fixture.drive.getState().pose().getX(), kDelta);
        command.execute100(0.02);
        command.end(false);
    }
}

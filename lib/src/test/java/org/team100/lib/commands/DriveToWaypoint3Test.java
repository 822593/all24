package org.team100.lib.commands;

import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.trajectory.Trajectory;

class DriveToWaypoint3Test {
    @Test
    void testSimple() {
        Pose2d goal = new Pose2d();
        MockSwerveDriveSubsystem drivetrain = new MockSwerveDriveSubsystem();
        BiFunction<Pose2d, Pose2d, Trajectory> trajectories = (x, y) ->  new Trajectory(List.of(new Trajectory.State()));;

        DriveToWaypoint3 command = new DriveToWaypoint3(
                goal,
                drivetrain,
                trajectories);

        // TODO: add some assertions
        command.initialize();
        command.execute();
        command.end(false);
    }
}

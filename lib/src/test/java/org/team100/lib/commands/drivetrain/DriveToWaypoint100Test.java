package org.team100.lib.commands.drivetrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.team100.lib.controller.DriveMotionController;
import org.team100.lib.controller.DrivePIDFController;
import org.team100.lib.controller.DrivePursuitController;
import org.team100.lib.controller.DriveRamseteController;
import org.team100.lib.geometry.GeometryUtil;
import org.team100.lib.motion.drivetrain.Fixtured;
import org.team100.lib.timing.TimingConstraint;
import org.team100.lib.timing.TimingConstraintFactory;
import org.team100.lib.trajectory.TrajectoryPlanner;

class DriveToWaypoint100Test extends Fixtured {
    private static final double kDelta = 0.001;


    @Test
    void testWithPID() {
        TrajectoryPlanner planner = new TrajectoryPlanner();
        DriveMotionController controller = new DrivePIDFController(false, 2.4, 2.4);

        List<TimingConstraint> constraints = new TimingConstraintFactory(fixture.swerveKinodynamics).forTest();

        DriveToWaypoint100 command = new DriveToWaypoint100(
                GeometryUtil.kPoseZero,
                fixture.drive,
                planner,
                controller,
                constraints,
                0);

        command.initialize();
        assertEquals(0, fixture.drive.getPose().getX(), kDelta);
        command.execute();
        command.end(false);
    }

    @Test
    void testWithPursuit() {
        TrajectoryPlanner planner = new TrajectoryPlanner();
        DriveMotionController controller = new DrivePursuitController(fixture.swerveKinodynamics);
        List<TimingConstraint> constraints = new TimingConstraintFactory(fixture.swerveKinodynamics).forTest();

        DriveToWaypoint100 command = new DriveToWaypoint100(
                GeometryUtil.kPoseZero,
                fixture.drive,
                planner,
                controller,
                constraints,
                0);

        command.initialize();
        assertEquals(0, fixture.drive.getPose().getX(), kDelta);
        command.execute();
        command.end(false);
    }

    @Test
    void testWithRamsete() {
        TrajectoryPlanner planner = new TrajectoryPlanner();
        DriveMotionController controller = new DriveRamseteController();
        List<TimingConstraint> constraints = new TimingConstraintFactory(fixture.swerveKinodynamics).forTest();

        DriveToWaypoint100 command = new DriveToWaypoint100(
                GeometryUtil.kPoseZero,
                fixture.drive,
                planner,
                controller,
                constraints,
                0);

        command.initialize();
        assertEquals(0, fixture.drive.getPose().getX(), kDelta);
        command.execute();
        command.end(false);
    }

    @Test
    void testWithFF() {
        TrajectoryPlanner planner = new TrajectoryPlanner();
        DriveMotionController controller = new DrivePIDFController(true, 2.4, 2.4);
        List<TimingConstraint> constraints = new TimingConstraintFactory(fixture.swerveKinodynamics).forTest();

        DriveToWaypoint100 command = new DriveToWaypoint100(
                GeometryUtil.kPoseZero,
                fixture.drive,
                planner,
                controller,
                constraints,
                0);

        command.initialize();
        assertEquals(0, fixture.drive.getPose().getX(), kDelta);
        command.execute();
        command.end(false);
    }
}

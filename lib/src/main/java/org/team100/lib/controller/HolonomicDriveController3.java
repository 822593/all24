package org.team100.lib.controller;

import org.team100.lib.config.Identity;
import org.team100.lib.logging.SupplierLogger2;
import org.team100.lib.motion.drivetrain.SwerveState;
import org.team100.lib.motion.drivetrain.kinodynamics.FieldRelativeVelocity;
import org.team100.lib.telemetry.Telemetry.Level;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;

/**
 * Drivetrain control with three independent PID controllers.
 */
public class HolonomicDriveController3 implements HolonomicFieldRelativeController {
    private final SupplierLogger2 m_logger;
    private final PIDController m_xController;
    private final PIDController m_yController;
    private final PIDController m_thetaController;

    public HolonomicDriveController3(SupplierLogger2 parent) {
        this(parent, cartesian(), cartesian(), theta());
    }

    public HolonomicDriveController3(
            SupplierLogger2 parent,
            PIDController xController,
            PIDController yController,
            PIDController thetaController) {
        m_xController = xController;
        m_yController = yController;
        m_thetaController = thetaController;
        m_logger = parent.child(this);
    }

    public static HolonomicDriveController3 withTolerance(
            SupplierLogger2 parent,
            double cartesianPosition,
            double cartesianVelocity,
            double rotationPosition,
            double rotationVelocity) {
        PIDController x = cartesian();
        x.setTolerance(cartesianPosition, cartesianVelocity);
        PIDController y = cartesian();
        y.setTolerance(cartesianPosition, cartesianVelocity);
        PIDController theta = theta();
        theta.setTolerance(rotationPosition, rotationVelocity);
        return new HolonomicDriveController3(parent, x, y, theta);
    }

    @Override
    public boolean atReference() {
        return m_xController.atSetpoint() && m_yController.atSetpoint() && m_thetaController.atSetpoint();
    }

    public Transform2d error() {
        return new Transform2d(m_xController.getPositionError(),
                m_yController.getPositionError(),
                new Rotation2d(m_thetaController.getPositionError()));
    }

    /**
     * Makes no attempt to coordinate the axes or provide feasible output.
     */
    @Override
    public FieldRelativeVelocity calculate(
            Pose2d currentPose,
            SwerveState desiredState) {

        Rotation2d currentRotation = currentPose.getRotation();

        double xFF = desiredState.x().v(); // m/s
        double yFF = desiredState.y().v(); // m/s
        double thetaFF = desiredState.theta().v(); // rad/s

        double xFB = m_xController.calculate(currentPose.getX(), desiredState.x().x());
        double yFB = m_yController.calculate(currentPose.getY(), desiredState.y().x());
        double thetaFB = m_thetaController.calculate(currentRotation.getRadians(), desiredState.theta().x());

        m_logger.doubleLogger(Level.TRACE, "u_FF/x").log( () -> xFF);
        m_logger.doubleLogger(Level.TRACE, "u_FF/y").log( () -> yFF);
        m_logger.doubleLogger(Level.TRACE, "u_FF/theta").log( () -> thetaFF);
        m_logger.doubleLogger(Level.TRACE, "u_FB/x").log( () -> xFB);
        m_logger.doubleLogger(Level.TRACE, "u_FB/y").log(() -> yFB);
        m_logger.doubleLogger(Level.TRACE, "u_FB/theta").log( () -> thetaFB);
        m_logger.pose2dLogger(Level.TRACE, "measurement").log( () -> currentPose);

        m_logger.doubleLogger(Level.TRACE, "setpoint/x").log( m_xController::getSetpoint);
        m_logger.doubleLogger(Level.TRACE, "setpoint/y").log( m_yController::getSetpoint);
        m_logger.doubleLogger(Level.TRACE, "setpoint/theta").log( m_thetaController::getSetpoint);
        m_logger.doubleLogger(Level.TRACE, "error/x").log( m_xController::getPositionError);
        m_logger.doubleLogger(Level.TRACE, "error/y").log( m_yController::getPositionError);
        m_logger.doubleLogger(Level.TRACE, "error/theta").log( m_thetaController::getPositionError);

        return new FieldRelativeVelocity(xFF + xFB, yFF + yFB, thetaFF + thetaFB);
    }

    @Override
    public void reset() {
        m_xController.reset();
        m_yController.reset();
        m_thetaController.reset();
    }

    public static PIDController cartesian() {
        PIDController pid;
        switch (Identity.instance) {
            case COMP_BOT:
                pid = new PIDController(3, 2, 0);
                pid.setIntegratorRange(-0.1, 0.1);
                pid.setTolerance(0.01); // 1 cm
                return pid;
            case SWERVE_ONE:
                pid = new PIDController(0.15, 0, 0);
                pid.setIntegratorRange(-0.1, 0.1);
                pid.setTolerance(0.01); // 1 cm
                return pid;
            case SWERVE_TWO:
                pid = new PIDController(2, 0.1, 0.15);
                pid.setIntegratorRange(-0.1, 0.1);
                pid.setTolerance(0.01); // 1 cm
                return pid;
            case BETA_BOT:
                pid = new PIDController(3, 2, 0);
                pid.setIntegratorRange(-0.1, 0.1);
                pid.setTolerance(0.01); // 1 cm
                return pid;
            case BLANK:
                // for testing
                pid = new PIDController(3, 1, 0);
                pid.setIntegratorRange(-0.1, 0.1);
                pid.setTolerance(0.01); // 1 cm
                return pid;
            default:
                // these RoboRIO's are have no drivetrains
                return new PIDController(1, 0.0, 0.0);
        }

    }

    public static PIDController theta() {
        PIDController pid = new PIDController(2, 0, 0);
        pid.setIntegratorRange(-0.01, 0.01);
        pid.setTolerance(0.01); // 0.5 degrees
        pid.enableContinuousInput(-1.0 * Math.PI, Math.PI);
        return pid;
    }

    @Override
    public String getGlassName() {
        return "HolonomicDriveController3";
    }

}

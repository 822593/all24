package org.team100.lib.motion.drivetrain.manual;

import org.team100.lib.commands.drivetrain.ModuleStateDriver;
import org.team100.lib.hid.DriverControl;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamics;
import org.team100.lib.telemetry.Logger;
import org.team100.lib.telemetry.Telemetry.Level;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveModuleState100;

import edu.wpi.first.math.geometry.Rotation2d;

/**
 * Transform manual input into swerve module states, in a simpler way than
 * ManualModuleStates does.
 * 
 * The input dtheta is exactly the module angle.
 * The input dx is exactly the wheel speed.
 * The input dy is ignored.
 */
public class SimpleManualModuleStates implements ModuleStateDriver {
    private final Logger m_logger;
    private final SwerveKinodynamics m_swerveKinodynamics;

    public SimpleManualModuleStates(Logger parent, SwerveKinodynamics swerveKinodynamics) {
        m_swerveKinodynamics = swerveKinodynamics;
        m_logger = parent.child(this);
    }

    /**
     * There's no conflict between translation and rotation velocities in this mode.
     */
    @Override
    public SwerveModuleState100[] apply(DriverControl.Velocity input) {
        // dtheta is from [-1, 1], so angle is [-pi, pi]
        Rotation2d angle = Rotation2d.fromRadians(Math.PI * input.theta());
        double speedM_S = m_swerveKinodynamics.getMaxDriveVelocityM_S() * input.x();
        m_logger.logDouble(Level.TRACE, "speed m_s", ()->speedM_S);
        m_logger.logDouble(Level.TRACE, "angle rad", ()->angle.getRadians());
        return new SwerveModuleState100[] {
                new SwerveModuleState100(speedM_S, angle),
                new SwerveModuleState100(speedM_S, angle),
                new SwerveModuleState100(speedM_S, angle),
                new SwerveModuleState100(speedM_S, angle)
        };
    }

    @Override
    public String getGlassName() {
        return "SimpleManualModuleStates";
    }
}

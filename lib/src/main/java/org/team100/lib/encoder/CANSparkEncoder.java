package org.team100.lib.encoder;

import java.util.OptionalDouble;

import org.team100.lib.logging.SupplierLogger2;
import org.team100.lib.motor.CANSparkMotor;
import org.team100.lib.telemetry.Telemetry.Level;

/**
 * The built-in encoder in Neo motors.
 * 
 * This encoder simply senses the 14 rotor magnets in 3 places, so it's 42 ticks
 * per turn.
 */
public class CANSparkEncoder implements IncrementalBareEncoder {
    private final SupplierLogger2 m_logger;
    private final CANSparkMotor m_motor;

    public CANSparkEncoder(
            SupplierLogger2 parent,
            CANSparkMotor motor) {
        m_logger = parent.child(this);
        m_motor = motor;
    }

    // /** Position in meters. */
    // @Override
    // public void setPosition(double positionM) {
    // double motorPositionRev = positionM / m_distancePerTurn;
    // m_motor.setEncoderPosition(motorPositionRev);
    // }

    @Override
    public void reset() {
        m_motor.resetEncoderPosition();
    }

    @Override
    public void close() {
        //
    }

    //////////////////////////////////

    @Override
    public OptionalDouble getPositionRad() {
        // raw position is in rotations
        // this is fast so we don't need to cache it
        double motorPositionRev = m_motor.getPositionRot();
        double positionRad = motorPositionRev * 2 * Math.PI;
        m_logger.doubleLogger(Level.TRACE, "motor position (rev)").log( () -> motorPositionRev);
        m_logger.doubleLogger(Level.TRACE, "position (rad)").log( () -> positionRad);
        return OptionalDouble.of(positionRad);
    }

    @Override
    public OptionalDouble getVelocityRad_S() {
        // raw velocity is in RPM
        // this is fast so we don't need to cache it
        double velocityRad_S = m_motor.getRateRPM() * 2 * Math.PI / 60;
        m_logger.doubleLogger(Level.TRACE, "velocity (rad_s)").log( () -> velocityRad_S);
        return OptionalDouble.of(velocityRad_S);
    }

    @Override
    public void setEncoderPositionRad(double motorPositionRad) {
        m_motor.setEncoderPositionRad(motorPositionRad);
    }

    @Override
    public void periodic() {
        m_logger.optionalDoubleLogger(Level.TRACE, "position (rad)").log( this::getPositionRad);
        m_logger.optionalDoubleLogger(Level.TRACE, "velocity (rad_s)").log( this::getVelocityRad_S);
    }
}

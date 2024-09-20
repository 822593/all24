package org.team100.lib.encoder;

import java.util.OptionalDouble;

import org.team100.lib.logging.SupplierLogger2;
import org.team100.lib.motor.BareMotor;
import org.team100.lib.telemetry.Telemetry.Level;

/** encoder implementation that supports only velocity measurement. */
public class VelocityBareEncoder implements IncrementalBareEncoder {
    private final SupplierLogger2 m_logger;
    private final BareMotor m_motor;

    public VelocityBareEncoder(
            SupplierLogger2 parent,
            BareMotor motor) {
        m_logger = parent.child(this);
        m_motor = motor;
    }

    @Override
    public OptionalDouble getVelocityRad_S() {
        double m_rate = m_motor.getVelocityRad_S();
        m_logger.doubleLogger(Level.TRACE, "velocity (rad_s)").log( () -> m_rate);
        return OptionalDouble.of(m_rate);
    }

    @Override
    public OptionalDouble getPositionRad() {
        return OptionalDouble.empty();
    }

    @Override
    public void reset() {
        //
    }

    @Override
    public void close() {
        //
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

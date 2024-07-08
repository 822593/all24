package org.team100.lib.motor;

import java.util.function.DoubleSupplier;

import org.team100.lib.config.Feedforward100;
import org.team100.lib.config.PIDConstants;
import org.team100.lib.motor.model.TorqueModel;
import org.team100.lib.telemetry.Logger;
import org.team100.lib.telemetry.Telemetry.Level;
import org.team100.lib.units.Measure100;

import com.ctre.phoenix6.configs.TalonFXConfigurator;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;

/**
 * Superclass for TalonFX motors.
 */
public abstract class Talon6Motor<T extends Measure100>
        implements DutyCycleMotor100, VelocityMotor100<T>, PositionMotor100<T>, TorqueModel {
    protected final Logger m_logger;
    private final TalonFX m_motor;
    private final Feedforward100 m_ff;

    // caching these status signals saves a lookup
    protected final DoubleSupplier m_position;
    protected final DoubleSupplier m_velocity;
    protected final DoubleSupplier m_dutyCycle;
    protected final DoubleSupplier m_error;
    protected final DoubleSupplier m_supply;
    protected final DoubleSupplier m_stator;
    protected final DoubleSupplier m_temp;
    protected final DoubleSupplier m_torque;

    // caching the control requests saves allocation
    private final VelocityVoltage m_velocityVoltage = new VelocityVoltage(0);
    private final DutyCycleOut m_dutyCycleOut = new DutyCycleOut(0);
    private final PositionVoltage m_PositionVoltage = new PositionVoltage(0);

    protected Talon6Motor(
            Logger parent,
            int canId,
            MotorPhase motorPhase,
            double supplyLimit,
            double statorLimit,
            PIDConstants lowLevelVelocityConstants,
            Feedforward100 ff) {
        m_logger = parent.child(this);
        m_motor = new TalonFX(canId);
        m_ff = ff;

        TalonFXConfigurator talonFXConfigurator = m_motor.getConfigurator();
        Phoenix100.baseConfig(talonFXConfigurator);
        Phoenix100.motorConfig(talonFXConfigurator, motorPhase);
        Phoenix100.currentConfig(talonFXConfigurator, supplyLimit, statorLimit);
        Phoenix100.pidConfig(talonFXConfigurator, lowLevelVelocityConstants);

        Phoenix100.crash(() -> m_motor.getPosition().setUpdateFrequency(50));
        Phoenix100.crash(() -> m_motor.getVelocity().setUpdateFrequency(50));
        Phoenix100.crash(() -> m_motor.getTorqueCurrent().setUpdateFrequency(50));

        m_position = () -> m_motor.getPosition().refresh().getValueAsDouble();
        m_velocity = () -> m_motor.getVelocity().refresh().getValueAsDouble();
        m_dutyCycle = () -> m_motor.getDutyCycle().refresh().getValueAsDouble();
        m_error = () -> m_motor.getClosedLoopError().refresh().getValueAsDouble();
        m_supply = () -> m_motor.getSupplyCurrent().refresh().getValueAsDouble();
        m_stator = () -> m_motor.getStatorCurrent().refresh().getValueAsDouble();
        m_temp = () -> m_motor.getDeviceTemp().refresh().getValueAsDouble();
        m_torque = () -> m_motor.getTorqueCurrent().refresh().getValueAsDouble();
    }

    @Override
    public void setDutyCycle(double output) {
        Phoenix100.warn(() -> m_motor.setControl(m_dutyCycleOut
                .withOutput(output)));
        m_logger.logDouble(Level.TRACE, "desired duty cycle [-1,1]", () -> output);
        log();
    }

    /**
     * Set motor output using motor quantities (rev/s, rev/s^2, Nm).
     */
    protected void setMotorVelocity(
            double motorRev_S,
            double motorRev_S2,
            double torqueNm) {
        double currentMotorRev_S = m_velocity.getAsDouble();

        double frictionFFVolts = m_ff.frictionFFVolts(currentMotorRev_S, motorRev_S);
        double velocityFFVolts = m_ff.velocityFFVolts(motorRev_S);
        double accelFFVolts = m_ff.accelFFVolts(motorRev_S2);
        double torqueFFVolts = getTorqueFFVolts(torqueNm);

        double kFFVolts = frictionFFVolts + velocityFFVolts + accelFFVolts + torqueFFVolts;

        // VelocityVoltage has an acceleration field for kA feedforward but we use
        // arbitrary feedforward for that.
        Phoenix100.warn(() -> m_motor.setControl(
                m_velocityVoltage
                        .withVelocity(motorRev_S)
                        .withFeedForward(kFFVolts)));

        m_logger.logDouble(Level.TRACE, "desired speed (rev_s)", () -> motorRev_S);
        m_logger.logDouble(Level.TRACE, "desired accel (rev_s2)", () -> motorRev_S2);
        m_logger.logDouble(Level.TRACE, "friction feedforward (v)", () -> frictionFFVolts);
        m_logger.logDouble(Level.TRACE, "velocity feedforward (v)", () -> velocityFFVolts);
        m_logger.logDouble(Level.TRACE, "accel feedforward (v)", () -> accelFFVolts);
        m_logger.logDouble(Level.TRACE, "torque feedforward (v)", () -> torqueFFVolts);
        log();
    }

    /**
     * Set motor output using motor quantities (rev, Nm).
     * 
     * Motor revolutions wind up, so setting 0 revs and 1 rev are different.
     * 
     * TODO: this is not done; finish it
     */
    protected void setMotorPosition(
            double motorRev,
            double motorRev_S,
            double torqueNm) {
        double currentMotorRev_S = m_velocity.getAsDouble();

        double frictionFFVolts = m_ff.frictionFFVolts(currentMotorRev_S, motorRev_S);
        double velocityFFVolts = m_ff.velocityFFVolts(motorRev_S);
        double torqueFFVolts = getTorqueFFVolts(torqueNm);

        double kFFVolts = frictionFFVolts + velocityFFVolts + torqueFFVolts;

        // PositionVoltage has a velocity field for kV feedforward but we use arbitrary
        // feedforward for that.
        Phoenix100.warn(() -> m_motor.setControl(
                m_PositionVoltage
                        .withPosition(motorRev)
                        .withFeedForward(kFFVolts)));

        m_logger.logDouble(Level.TRACE, "desired position (rev)", () -> motorRev);
        m_logger.logDouble(Level.TRACE, "desired speed (rev_s)", () -> motorRev_S);
        m_logger.logDouble(Level.TRACE, "friction feedforward (v)", () -> frictionFFVolts);
        m_logger.logDouble(Level.TRACE, "velocity feedforward (v)", () -> velocityFFVolts);
        m_logger.logDouble(Level.TRACE, "torque feedforward (v)", () -> torqueFFVolts);
        log();
    }

    @Override
    public void stop() {
        m_motor.stopMotor();
    }

    @Override
    public void close() {
        m_motor.close();
    }

    /**
     * Sets integrated sensor position to zero.
     */
    public void resetEncoderPosition() {
        Phoenix100.warn(() -> m_motor.setPosition(0));
    }

    /**
     * Set integrated sensor position in rotations.
     */
    public void setEncoderPosition(double motorPositionRev) {
        Phoenix100.warn(() -> m_motor.setPosition(motorPositionRev));
    }

    public double getVelocityRev_S() {
        return m_velocity.getAsDouble();
    }

    public double getPositionRev() {
        return m_position.getAsDouble();
    }

    protected void log() {
        // suppliers here are never touched in the non-logging case.
        m_logger.logInt(Level.TRACE, "Device ID", m_motor::getDeviceID);
        m_logger.logDouble(Level.TRACE, "velocity (rev_s)", m_velocity);
        m_logger.logDouble(Level.TRACE, "output [-1,1]", m_dutyCycle);
        m_logger.logDouble(Level.TRACE, "error (rev_s)", m_error);
        m_logger.logDouble(Level.TRACE, "supply current (A)", m_supply);
        m_logger.logDouble(Level.TRACE, "stator current (A)", m_stator);
        m_logger.logDouble(Level.TRACE, "torque (Nm)", this::getMotorTorque);
        m_logger.logDouble(Level.TRACE, "temperature (C)", m_temp);
    }

    private double getMotorTorque() {
        // I looked into latency compensation of this signal but it doesn't seem
        // possible. latency compensation requires a signal and its time derivative,
        // e.g. position and velocity, or yaw and angular velocity. There doesn't seem
        // to be such a thing for current.
        return m_torque.getAsDouble() * kTNm_amp();
    }
}

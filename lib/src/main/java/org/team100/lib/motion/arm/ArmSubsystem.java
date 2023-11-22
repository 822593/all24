package org.team100.lib.motion.arm;

import org.team100.lib.telemetry.Telemetry;
import org.team100.lib.telemetry.Telemetry.Level;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.math.filter.LinearFilter;
import edu.wpi.first.wpilibj.AnalogEncoder;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj2.command.Subsystem;

public class ArmSubsystem extends Subsystem {
    public static class Config {
        public double filterTimeConstantS = 0.06;
        public double filterPeriodS = 0.02;
        public double lowerEncoderZero = 0.861614;
        public double upperEncoderZero = 0.266396;
    }

    private final Config m_config = new Config();
    private final Telemetry t = Telemetry.get();
    private final LinearFilter m_lowerMeasurementFilter;
    private final LinearFilter m_upperMeasurementFilter;
    private final CANSparkMax m_lowerArmMotor;
    private final CANSparkMax m_upperArmMotor;

    private final AnalogEncoder m_lowerArmEncoder;
    private final AnalogEncoder m_upperArmEncoder;
    private ArmAngles m_previousPosition;

    public ArmSubsystem() {
        m_lowerMeasurementFilter = LinearFilter.singlePoleIIR(m_config.filterTimeConstantS, m_config.filterPeriodS);
        m_upperMeasurementFilter = LinearFilter.singlePoleIIR(m_config.filterTimeConstantS, m_config.filterPeriodS);

        m_lowerArmMotor = new CANSparkMax(4, MotorType.kBrushless);
        m_lowerArmMotor.restoreFactoryDefaults();
        m_lowerArmMotor.setSmartCurrentLimit(8);
        m_lowerArmMotor.setSecondaryCurrentLimit(8);
        m_lowerArmMotor.setIdleMode(IdleMode.kBrake);

        m_upperArmMotor = new CANSparkMax(30, MotorType.kBrushless);
        m_upperArmMotor.restoreFactoryDefaults();
        m_upperArmMotor.setSmartCurrentLimit(1);
        m_upperArmMotor.setSecondaryCurrentLimit(1);
        m_upperArmMotor.setIdleMode(IdleMode.kBrake);

        m_lowerArmEncoder = new AnalogEncoder(new AnalogInput(1));
        m_upperArmEncoder = new AnalogEncoder(new AnalogInput(0));
        m_previousPosition = getPosition();
    }

    /** Arm angles (radians), 0 up, positive forward. */
    public ArmAngles getPosition() {
        ArmAngles result = new ArmAngles(
                m_lowerMeasurementFilter.calculate(getLowerArmAngleRadians()),
                m_upperMeasurementFilter.calculate(getUpperArmAngleRadians()));
        t.log(Level.DEBUG, "/arm/Lower Encoder Pos: ", result.th1);
        t.log(Level.DEBUG, "/arm/Upper Encoder Pos: ", result.th2);
        return result;
    }

    /** Joint velocities in radians per second. */
    public ArmAngles getVelocity() {
        ArmAngles position = getPosition();
        double th1 = position.th1 - m_previousPosition.th1;
        double th2 = position.th2 - m_previousPosition.th2;
        m_previousPosition = position;
        ArmAngles result = new ArmAngles(th1 * 50, th2 * 50);
        t.log(Level.DEBUG, "/arm/Lower Encoder Vel: ", result.th1);
        t.log(Level.DEBUG, "/arm/Upper Encoder Vel: ", result.th2);
        return result;
    }

    /**
     * Set motor controller duty cycle in range [-1, 1]
     * 
     * @param u1 lower, proximal
     * @param u2 upper, distal
     */
    public void set(double u1, double u2) {
        m_lowerArmMotor.set(u1);
        m_upperArmMotor.set(u2);
    }

    public void close() {
        m_lowerArmMotor.close();
        m_upperArmMotor.close();
        m_lowerArmEncoder.close();
        m_upperArmEncoder.close();
    }

    /** Lower arm angle (radians), 0 up, positive forward. */
    private double getLowerArmAngleRadians() {
        double x = (m_lowerArmEncoder.getAbsolutePosition() - m_config.lowerEncoderZero) * 360;
        return (-1.0 * x) * Math.PI / 180;
    }

    /** Upper arm angle (radians), 0 up, positive forward. */
    private double getUpperArmAngleRadians() {
        double x = (m_upperArmEncoder.getAbsolutePosition() - m_config.upperEncoderZero) * 360;
        return x * Math.PI / 180;
    }

}

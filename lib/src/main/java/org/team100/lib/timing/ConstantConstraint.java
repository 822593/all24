package org.team100.lib.timing;

import org.team100.lib.geometry.Pose2dWithMotion;

/** Trivial constraint for testing. */
public class ConstantConstraint implements TimingConstraint {
    private final double m_maxVelocity;
    private final double m_maxAccel;
    

    public ConstantConstraint(double m_maxVelocity, double m_maxAccel) {
        this.m_maxVelocity = m_maxVelocity;
        this.m_maxAccel = m_maxAccel;
    }

    @Override
    public double getMaxVelocity(Pose2dWithMotion state) {
        return m_maxVelocity;
    }

    @Override
    public MinMaxAcceleration getMinMaxAcceleration(Pose2dWithMotion state, double velocityM_S) {
        return new MinMaxAcceleration(-m_maxAccel, m_maxAccel);
    }
    
}

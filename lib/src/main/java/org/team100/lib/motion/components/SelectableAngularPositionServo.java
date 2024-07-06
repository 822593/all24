package org.team100.lib.motion.components;

import java.util.OptionalDouble;
import java.util.function.BooleanSupplier;

import org.team100.lib.controller.State100;

public class SelectableAngularPositionServo implements AngularPositionServo {
    private final AngularPositionServo m_whenTrue;
    private final AngularPositionServo m_whenFalse;
    private final BooleanSupplier m_selector;

    public SelectableAngularPositionServo(
            AngularPositionServo whenTrue,
            AngularPositionServo whenFalse,
            BooleanSupplier selector) {
        m_whenTrue = whenTrue;
        m_whenFalse = whenFalse;
        m_selector = selector;
    }

    @Override
    public void reset() {
        if (m_selector.getAsBoolean()) {
            m_whenTrue.reset();
        } else {
            m_whenFalse.reset();
        }
    }

    @Override
    public void setPosition(double goal, double feedForwardTorqueNm) {
        if (m_selector.getAsBoolean()) {
            m_whenTrue.setPosition(goal, feedForwardTorqueNm);
        } else {
            m_whenFalse.setPosition(goal, feedForwardTorqueNm);
        }
    }

    @Override
    public OptionalDouble getPosition() {
        if (m_selector.getAsBoolean()) {
            return m_whenTrue.getPosition();
        } else {
            return m_whenFalse.getPosition();
        }
    }

    @Override
    public OptionalDouble getVelocity() {
        if (m_selector.getAsBoolean()) {
            return m_whenTrue.getVelocity();
        } else {
            return m_whenFalse.getVelocity();
        }
    }

    @Override
    public boolean atSetpoint() {
        if (m_selector.getAsBoolean()) {
            return m_whenTrue.atSetpoint();
        } else {
            return m_whenFalse.atSetpoint();
        }
    }

    @Override
    public boolean atGoal() {
        if (m_selector.getAsBoolean()) {
            return m_whenTrue.atGoal();
        } else {
            return m_whenFalse.atGoal();
        }
    }

    @Override
    public double getGoal() {
        if (m_selector.getAsBoolean()) {
            return m_whenTrue.getGoal();
        } else {
            return m_whenFalse.getGoal();
        }
    }

    @Override
    public void stop() {
        if (m_selector.getAsBoolean()) {
            m_whenTrue.stop();
        } else {
            m_whenFalse.stop();
        }
    }

    @Override
    public void close() {
        if (m_selector.getAsBoolean()) {
            m_whenTrue.close();
        } else {
            m_whenFalse.close();
        }
    }

    @Override
    public State100 getSetpoint() {
        if (m_selector.getAsBoolean()) {
            return m_whenTrue.getSetpoint();
        } else {
            return m_whenFalse.getSetpoint();
        }
    }

}

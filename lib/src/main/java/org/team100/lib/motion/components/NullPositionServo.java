package org.team100.lib.motion.components;

import org.team100.lib.controller.State100;
import org.team100.lib.units.Measure100;

/** For testing. */
public class NullPositionServo<T extends Measure100> implements PositionServo<T> {

    @Override
    public void reset() {
        //
    }

    @Override
    public void setPosition(double goal, double feedForwardTorqueNm) {
        //
    }

    @Override
    public double getPosition() {
        return 0.0;
    }

    @Override
    public double getVelocity() {
        return 0.0;
    }

    @Override
    public boolean atSetpoint() {
        return true;
    }

    @Override
    public boolean atGoal() {
        return true;
    }

    @Override
    public double getGoal() {
        return 0.0;
    }

    @Override
    public void stop() {
        //
    }

    @Override
    public void close() {
        //
    }

    @Override
    public State100 getSetpoint() {
        return new State100();
    }    
}

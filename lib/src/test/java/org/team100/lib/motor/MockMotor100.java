package org.team100.lib.motor;

import org.team100.lib.units.Measure100;

public class MockMotor100<T extends Measure100> implements Motor100<T> {
    public double output = 0;
    public double velocity = 0;
    public double position = 0;

    @Override
    public void setDutyCycle(double output) {
        this.output = output;
    }

    @Override
    public void setVelocity(double velocity, double accel, double torque) {
        this.velocity = velocity;
    }

    @Override
    public void setPosition(double position, double velocity, double torque) {
        this.position = position;
    }

    @Override
    public void stop() {
        this.output = 0;
        this.velocity = 0;
    }

    @Override
    public void close() {
        //
    }

}

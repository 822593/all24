package org.team100.lib.motor.turning;

import org.team100.lib.config.Feedforward100;
import org.team100.lib.config.PIDConstants;
import org.team100.lib.motor.MotorPhase;
import org.team100.lib.motor.model.KrakenTorqueModel;
import org.team100.lib.telemetry.Telemetry.Logger;

public class Kraken6TurningMotor extends Talon6TurningMotor implements KrakenTorqueModel {

    protected Kraken6TurningMotor(
            String name,
            Logger parent,
            int canId,
            MotorPhase motorPhase,
            double kGearRatio,
            PIDConstants pid,
            Feedforward100 ff) {
        super(name, parent, canId, motorPhase, kGearRatio, pid, ff);
    }
}

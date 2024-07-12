package org.team100.lib.motion.drivetrain.kinodynamics;

import java.util.Optional;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Rotation2d;

/**
 * Just like ChassisSpeeds, but field-relative, to avoid mixing them up.
 */
public record FieldRelativeVelocity(double x, double y, double theta) {
    public double norm() {
        return Math.hypot(x, y);
    }

    public Optional<Rotation2d> angle() {
        if (Math.abs(x) < 1e-6 && Math.abs(y) < 1e-6)
            return Optional.empty();
        return Optional.of(new Rotation2d(x, y));
    }

    public FieldRelativeVelocity plus(FieldRelativeVelocity other) {
        return new FieldRelativeVelocity(x + other.x, y + other.y, theta + other.theta);
    }

    public FieldRelativeVelocity minus(FieldRelativeVelocity other) {
        return new FieldRelativeVelocity(x - other.x, y - other.y, theta - other.theta);
    }

    public FieldRelativeVelocity times(double scalar) {
        return new FieldRelativeVelocity(x * scalar, y * scalar, theta * scalar);
    }

    public FieldRelativeVelocity times(double cartesian, double angular) {
        return new FieldRelativeVelocity(x * cartesian, y * cartesian, theta * angular);
    }

    /** Dot product of translational part. */
    public double dot(FieldRelativeVelocity other) {
        return x * other.x + y * other.y;
    }

    public FieldRelativeVelocity clamp(double maxVelocity, double maxOmega) {
        double norm = Math.hypot(x, y);
        double ratio = 1.0;
        if (norm > 1e-3 && norm > maxVelocity) {
            ratio = maxVelocity / norm;
        }
        return new FieldRelativeVelocity(ratio * x, ratio * y, MathUtil.clamp(theta, -maxOmega, maxOmega));
    }

    @Override
    public String toString() {
        return String.format("(%5.2f, %5.2f, %5.2f)", x, y, theta);
    }
}
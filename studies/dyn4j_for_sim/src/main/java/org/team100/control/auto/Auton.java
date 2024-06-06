package org.team100.control.auto;

import java.util.Arrays;
import java.util.Optional;

import org.team100.control.Pilot;
import org.team100.field.StagedNote;
import org.team100.subsystems.CameraSubsystem;
import org.team100.subsystems.CameraSubsystem.NoteSighting;
import org.team100.subsystems.DriveSubsystem;
import org.team100.subsystems.IndexerSubsystem;
import org.team100.util.Arg;
import org.team100.util.Latch;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;

/**
 * Fetch a staged note, shoot it into the speaker, repeat.
 * 
 * TODO: make a special "shoot preload" command.
 * 
 * The new design here will have an array for "state" of notes that remain to be
 * picked; each note state is a belief informed by observations and decayed by
 * the passage of time. The state unit is probability of presence, a crisp value
 * for the estimate.
 * 
 * So the choice of note to pursue depends on this belief.
 * 
 * Does not improvise (uses the staged notes only), because otherwise it would
 * always choose the close ones instead of its strategic goal, which might be
 * far away.
 * 
 * TODO: add opportunistic diversion.
 */
public class Auton implements Pilot {
    private static final double kBeliefUpdateTolerance = 0.1;

    // tolerance for go-to-staged-note paths.
    private static final double kStageTolerance = 0.6;

    private final DriveSubsystem m_drive;
    private final CameraSubsystem m_camera;
    private final IndexerSubsystem m_indexer;
    private final Pose2d m_shooting;
    private final boolean m_debug;
    /** List of note id's we should go get. */
    private final Integer[] m_agenda;
    /** Parallel list of presence probability. */
    private final double[] m_beliefs;
    /**
     * Which note within the agenda we're currently looking for. Increments when we
     * start looking, i.e. when the indexer becomes *empty*.
     */
    // private final Counter m_noteIndex;
    /** Latches the pick event. */
    private final Latch m_picked;

    private boolean m_enabled = false;
    /** hm the note counter is problematic, just remember the current goal. */
    private int m_goalNoteIdx;

    public Auton(
            DriveSubsystem drive,
            CameraSubsystem camera,
            IndexerSubsystem indexer,
            Pose2d shooting,
            boolean debug,
            Integer... agenda) {
        Arg.nonnull(drive);
        Arg.nonnull(camera);
        Arg.nonnull(indexer);
        Arg.nonempty(agenda);
        m_drive = drive;
        m_camera = camera;
        m_indexer = indexer;
        m_agenda = agenda;
        m_beliefs = new double[agenda.length];
        // initially, each position is full.
        Arrays.fill(m_beliefs, 1);
        m_shooting = shooting;
        m_debug = debug;
        // increment on empty.
        // m_noteIndex = new Counter(() -> !m_indexer.full());
        // the first index is 0 so the starting index is -1
        // m_noteIndex.set(-1);
        m_goalNoteIdx = -1;
        m_picked = new Latch(m_indexer::full);
    }

    // first go to the right place, ignoring nearby notes on the way.
    @Override
    public boolean driveToStaged() {
        return m_enabled && !m_indexer.full();
    }

    // ... and intake it
    @Override
    public boolean intake() {
        return m_enabled && nearGoal() && !m_indexer.full();
    }

    // if we have one, go score it.
    // there needs to be room for multiple scorers
    @Override
    public boolean scoreSpeaker() {
        return m_enabled && m_indexer.full();
    }

    @Override
    public Pose2d shootingLocation() {
        return m_shooting;
    }

    @Override
    public int goalNote() {
        if (m_indexer.full()) {
            // no goal if we already have one
            return 0;
        }
        // int idx = m_noteIndex.getAsInt();
        int idx = m_goalNoteIdx;
        if (idx > m_agenda.length - 1) {
            // no goal if we're done
            return 0;
        }
        if (idx < 0) {
            // no goal if we haven't started yet
            return 0;
        }
        Integer noteId = m_agenda[idx];
        if (m_debug)
            System.out.printf(" agenda note id %d", noteId);
        return noteId;
    }

    @Override
    public void begin() {
        m_enabled = true;
    }

    @Override
    public void reset() {
        m_enabled = false;
        // m_noteIndex.reset();
        m_goalNoteIdx = -1;
    }

    @Override
    public void periodic() {
        if (m_debug) {
            System.out.print("periodic beliefs: ");
            for (int i = 0; i < m_beliefs.length; ++i) {
                System.out.printf(" %5.3f", m_beliefs[i]);
            }
            System.out.println();
        }
        // m_noteIndex.periodic();
        m_picked.periodic();
        updateBeliefs();
        // beliefs decay to zero over time.
        for (int i = 0; i < m_beliefs.length; ++i) {
            m_beliefs[i] *= 0.9999;
        }
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * Use vision to find nearby notes.
     * 
     * This provides both positive evidence (notes sighted) and negative evidence
     * (notes missing), if we know what the vision radius is.
     */
    private void updateBeliefs() {
        if (m_debug)
            System.out.printf("idx %d\n", m_goalNoteIdx);
        // System.out.printf("idx %d\n", m_noteIndex.getAsInt());
        Pose2d robotPose = m_drive.getPose();
        if (m_debug)
            System.out.printf("pose %5.3f %5.3f\n", robotPose.getX(), robotPose.getY());
        // make this smaller to account for lag
        double visionRadiusM = CameraSubsystem.kMaxNoteDistance - 1;
        // left-join the agenda to the sightings.
        for (int i = 0; i < m_agenda.length; ++i) {
            int noteId = m_agenda[i];
            Optional<StagedNote> n = StagedNote.get(noteId);
            if (n.isEmpty())
                continue;
            Translation2d noteLocation = n.get().getLocation();
            double lookingDistance = noteLocation.getDistance(robotPose.getTranslation());
            if (lookingDistance > visionRadiusM) {
                // too far to see, do not update belief
                continue;
            }

            if (m_debug)
                System.out.printf("looking distance %5.3f\n", lookingDistance);
            findit(i, noteLocation);
        }
        if (m_picked.getAsBoolean()) {
            // if we just picked a note then it's gone; this overrides whatever the camera
            // says.
            int idx = m_goalNoteIdx;
            // int idx = m_noteIndex.getAsInt();
            if (m_debug)
                System.out.printf("picked %d\n", idx);
            if (idx >= 0 && idx < m_beliefs.length)
                m_beliefs[idx] = 0;
        }
        // update the index so we don't look for missing notes
        // for (int i = m_noteIndex.getAsInt(); i < m_beliefs.length; ++i) {
        for (int i = m_goalNoteIdx; i < m_beliefs.length; ++i) {
            if (i < 0)
                continue;
            if (m_beliefs[i] > 0.1) {
                m_goalNoteIdx = i;
                break;
            }
            // if (m_beliefs[i] < 0.1) {
            // // skip this one
            // m_noteIndex.set(i + 1);
            // } else {
            // // this is the one we want.
            // break;
            // }
        }
        if (m_debug) {
            System.out.print("beliefs: ");
            for (int i = 0; i < m_beliefs.length; ++i) {
                System.out.printf(" %5.3f", m_beliefs[i]);
            }
            System.out.println();
        }

    }

    /**
     * Look through recent sightings for note i at location l. if we find it, update
     * belief to true; if not, update to false.
     */
    private void findit(int i, Translation2d noteLocation) {
        if (m_debug)
            System.out.printf("trying to find location %5.3f %5.3f\n", noteLocation.getX(), noteLocation.getY());
        for (NoteSighting sighting : m_camera.recentNoteSightings().values()) {
            if (m_debug)
                System.out.printf("sighting %5.3f %5.3f\n", sighting.position().getX(), sighting.position().getY());
            // can we see it?
            if (noteLocation.getDistance(sighting.position()) < kBeliefUpdateTolerance) {
                // found it!
                if (m_debug)
                    System.out.printf("found note %d\n", i);
                m_beliefs[i] = 1;
                return;
            }
        }
        // it's gone!
        if (m_debug)
            System.out.printf("missing note %d\n", i);
        m_beliefs[i] = 0;
    }

    private boolean nearGoal() {
        Pose2d pose = m_drive.getPose();
        // int idx = m_noteIndex.getAsInt();
        int idx = m_goalNoteIdx;
        if (idx < 0) {
            if (m_debug)
                System.out.printf("skip idx %d\n", idx);
            return false;
        }
        if (idx > m_agenda.length - 1) {
            if (m_debug)
                System.out.printf("skip idx %d > agenda\n", idx);
            return false;
        }
        Integer noteId = m_agenda[idx];
        Optional<StagedNote> n = StagedNote.get(noteId);
        if (n.isEmpty()) {
            if (m_debug)
                System.out.printf("bad id");
            return false;
        }
        Translation2d goal = n.get().getLocation();
        double distance = goal.getDistance(pose.getTranslation());
        if (m_debug)
            System.out.printf("distance %5.3f\n", distance);
        return distance < kStageTolerance;
    }

}

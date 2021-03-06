package frc.robot.auto.functions;

import com.ctre.phoenix.motion.TrajectoryPoint;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.StatusFrame;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import edu.wpi.first.wpilibj.Notifier;
import frc.robot.auto.setup.RobotFunction;
import frc.robot.subsystems.Drivetrain;
import frc.robot.utils.PrettyPrint;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Follows a motion profiling path saved in a csv
 * <p>{@code values[0]} is the filepath to the spline to be ran</p>
 */
public class FollowPath extends RobotFunction {
    private static final int minPoints = 5;
    private static final int msPerPoint = 10;
    private static final double kP = 0.5;
    private static final double kI = 0.0;
    private static final double kD = 0.0;
    private static final double kF = 0.0;

    private boolean started;

    /**
     * @param filepath to spline, "_left.csv" or "_right.csv" is appended to it
     */
    public FollowPath(String filepath) {
        started = false;

        for (TrajectoryPoint point : fromFile(filepath + "_left.csv"))
            Drivetrain.frontLeft.pushMotionProfileTrajectory(point);

        for (TrajectoryPoint point : fromFile(filepath + "_right.csv"))
            Drivetrain.frontRight.pushMotionProfileTrajectory(point);

        //push points three times as fast as robot's loop runs
        new Notifier(() -> {
            Drivetrain.frontLeft.processMotionProfileBuffer();
            Drivetrain.frontRight.processMotionProfileBuffer();
        }).startPeriodic(msPerPoint / 3000.0);
    }

    @Override
    public void init() {
        configTalons();
    }

    /**
     * puts the motion profile in hold mode, holds last pair of velocities
     */
    @Override
    public void stop() {
        Drivetrain.motionProfile(false);
    }

    @Override
    public void run() {
        // ensure that buffer is sufficiently filled
        started |= Drivetrain.getLeftProfileStatus().btmBufferCnt >= minPoints &&
                Drivetrain.getRightProfileStatus().btmBufferCnt >= minPoints;

        Drivetrain.motionProfile(started);

        PrettyPrint.put("MP left vel", Drivetrain.frontLeft.getActiveTrajectoryVelocity());
        PrettyPrint.put("MP right vel", Drivetrain.frontRight.getActiveTrajectoryVelocity());
    }

    @Override
    public boolean isFinished() {
        return Drivetrain.frontLeft.getMotionProfileTopLevelBufferCount() == 0 &&
                Drivetrain.frontRight.getMotionProfileTopLevelBufferCount() == 0 &&
                started;
    }

    private TrajectoryPoint[] fromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            TrajectoryPoint[] points = reader
                    .lines()
                    .skip(1)
                    .map(s -> s.split(","))
                    .map(strArray -> Arrays
                            .stream(strArray)
                            .mapToDouble(Double::parseDouble)
                            .toArray())
                    .map(vals -> {
                        TrajectoryPoint p = new TrajectoryPoint();
                        p.profileSlotSelect0 = 0;
                        p.position = vals[0];
                        p.velocity = vals[1];
                        return p;
                    })
                    .toArray(TrajectoryPoint[]::new);
            points[0].zeroPos = true;
            points[points.length - 1].isLastPoint = true;
            return points;
        } catch (IOException e) {
            PrettyPrint.error("PathFollower file not found");
        }
        return new TrajectoryPoint[0];
    }

    /**
     * sets all values of the drivetrain talons so that motion profiling will work
     */
    private void configTalons() {
        // clear old motion profiling stuff
        Drivetrain.frontLeft.clearMotionProfileHasUnderrun();
        Drivetrain.frontLeft.clearMotionProfileTrajectories();

        Drivetrain.frontRight.clearMotionProfileTrajectories();
        Drivetrain.frontRight.clearMotionProfileHasUnderrun();

        // config motion profile values
        Drivetrain.frontLeft.configMotionProfileTrajectoryPeriod(msPerPoint);
        Drivetrain.frontLeft.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, 1000);
        Drivetrain.frontLeft.setStatusFramePeriod(StatusFrameEnhanced.Status_9_MotProfBuffer, msPerPoint);
        Drivetrain.frontLeft.setStatusFramePeriod(StatusFrame.Status_10_Targets, msPerPoint);
        Drivetrain.frontLeft.setStatusFramePeriod(StatusFrame.Status_12_Feedback1, msPerPoint);
        Drivetrain.frontLeft.setStatusFramePeriod(StatusFrame.Status_17_Targets1, msPerPoint);

        Drivetrain.frontRight.configMotionProfileTrajectoryPeriod(msPerPoint);
        Drivetrain.frontRight.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, 1000);
        Drivetrain.frontRight.setStatusFramePeriod(StatusFrameEnhanced.Status_9_MotProfBuffer, msPerPoint);
        Drivetrain.frontRight.setStatusFramePeriod(StatusFrame.Status_10_Targets, msPerPoint);
        Drivetrain.frontRight.setStatusFramePeriod(StatusFrame.Status_12_Feedback1, msPerPoint);
        Drivetrain.frontRight.setStatusFramePeriod(StatusFrame.Status_17_Targets1, msPerPoint);

        // pid constants for motion profile
        Drivetrain.frontLeft.config_kP(0, kP);
        Drivetrain.frontLeft.config_kI(0, kI);
        Drivetrain.frontLeft.config_kD(0, kD);
        Drivetrain.frontLeft.config_kF(0, kF);

        Drivetrain.frontLeft.config_kP(0, kP);
        Drivetrain.frontLeft.config_kI(0, kI);
        Drivetrain.frontLeft.config_kD(0, kD);
        Drivetrain.frontLeft.config_kF(0, kF);
    }
}
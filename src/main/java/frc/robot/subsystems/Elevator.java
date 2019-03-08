package frc.robot.subsystems;

import com.revrobotics.CANEncoder;
import com.revrobotics.CANPIDController.AccelStrategy;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.ControlType;

public class Elevator {
    private static final CANSparkMax elevator;
    private static final CANEncoder elevatorEncoder;

    private static double setPosition, prevPosition, zeroPosition;

    private static final double STAGE_THRESHOLD = 30.0;
    private static final double MAX_POSITION = 47.5;
    private static final double DEADBAND = 0.5;
    private static final double HATCH_DROP_OFFSET = 3.2;
    private static final double HATCH_PLACE_OFFSET = 0.7;
    public static boolean setHatchDrop, setHatchPlace;

    public enum Position {
        ELEVATOR_FLOOR(0.0),
        HATCH_LEVEL_ONE(1.5), //3.0
        HATCH_LEVEL_TWO(22.0), //21.0
        HATCH_LEVEL_THREE(44.5), //43.5
        CARGO_LEVEL_ONE(17.0),
        CARGO_LEVEL_TWO(39.5),
        CARGO_LEVEL_THREE(45.5),
        CARGO_SHIP(30.0),
        ELEVATOR_COLLECT_CARGO(7.8),
        //        ELEVATOR_COLLECT_HATCH(11.5); //top hatch mechanism
        ELEVATOR_COLLECT_HATCH(HATCH_DROP_OFFSET + HATCH_PLACE_OFFSET);

        double value;

        Position(double position) {
            value = position;
        }
    }

    /*
     * Initializes the elevator motor, sets PID values, and zeros the elevator
     * encoder
     */
    static {
        elevator = new CANSparkMax(5, MotorType.kBrushless);
        elevator.setInverted(false);
        elevator.getPIDController().setOutputRange(-1.0, 1.0);
        elevator.getPIDController().setSmartMotionAccelStrategy(AccelStrategy.kTrapezoidal, 0);
        elevator.getPIDController().setSmartMotionAllowedClosedLoopError(0.0, 0);
        elevator.getPIDController().setSmartMotionMaxAccel(8000.0, 0);
        elevator.getPIDController().setSmartMotionMaxVelocity(15000.0, 0);
        elevator.getPIDController().setSmartMotionMinOutputVelocity(0.1, 0);

        elevator.getPIDController().setReference(0, ControlType.kSmartMotion);
        
        elevator.setSmartCurrentLimit(55);

        setPID(0.0003, 0.0, 0.0, 0.0);

        elevatorEncoder = new CANEncoder(elevator);

        elevator.setEncPosition(0);
        setPosition = elevatorEncoder.getPosition();
        prevPosition = elevatorEncoder.getPosition();
        zeroPosition = elevatorEncoder.getPosition();

        setHatchDrop = false;
        setHatchPlace = false;
    }

    /**
     * inserts the p i d f values into the Talon SRX
     *
     * @param p: proportional value
     * @param i: integral value
     * @param d: derivative value
     * @param f: feed forward value
     */
    private static void setPID(double p, double i, double d, double f) {
        elevator.getPIDController().setP(p);
        elevator.getPIDController().setI(i);
        elevator.getPIDController().setD(d);
        elevator.getPIDController().setFF(f);
    }

    /**
     * sets the elevator set value to its current value
     */
    public static void stop() {
        setPosition = elevatorEncoder.getPosition();
        setPosition(setPosition);
    }

    /**
     * sets the elevator set value to its current value
     */
    public static double getCurrent() {
        return elevator.getOutputCurrent();
    }

    /**
     * @return the set point of the elevator
     */
    public static double getSetPosition() {
        return setPosition - zeroPosition;
    }

    /**
     * @return the position of the elevator
     */
    public static double getPosition() {
        return elevatorEncoder.getPosition() - zeroPosition;
    }

    /**
     * @return true if the elevator is above the stationary stage
     */
    public static boolean aboveStageThreshold() {
        return elevatorEncoder.getPosition() > STAGE_THRESHOLD+zeroPosition;
    }

    /**
     * @return true if the elevator is within deadband of its set value
     */
    public static boolean doneMoving() {
        if (Math.abs(elevatorEncoder.getPosition() - prevPosition) > DEADBAND) {
            prevPosition = elevatorEncoder.getPosition();
            return true;
        } else {
            return false;
        }
    }

    /**
     * moves the elevator at a speed (percent output)
     *
     * @param speed: speed of the elevator (-1.0 to 1.0)
     */
    public static void moveSpeed(double speed) {
        elevator.set(speed);
    }

    /**
     * Sets the elevator to the entered value
     *
     * @param position: desired elevator value
     */
    public static void setPosition(double position) {
        setPosition = position + zeroPosition;
        limitPosition();
    }

    /**
     * Sets the elevator to the entered position
     *
     * @param position desired elevator position
     */
    public static void setPosition(Position position) {
        if (position.name().toLowerCase().contains("hatch")) {
            setPosition(position.value + HATCH_DROP_OFFSET);
        } else {
            setPosition(position.value);
        }
    }

    /**
     * Sets the elevator lower by the hatch offset to drop the hatch panel
     */
    public static void dropHatch() {
        if (!setHatchDrop) {
            setPosition -= HATCH_DROP_OFFSET;
            setPosition(setPosition);
            setHatchDrop = true;
        }
    }

    /**
     * Sets the elevator lower by the hatch offset to drop the hatch panel
     */
    public static void placeHatch() {
        if (!setHatchPlace) {
            setPosition -= HATCH_PLACE_OFFSET;
            setPosition(setPosition);
            setHatchPlace = true;
        }
    }


    /**
     * Sets the current elevator position to the new zero
     */
    public static void resetEncoder() {
//        elevator.setEncPosition(0);
        zeroPosition = elevatorEncoder.getPosition();
        setPosition = Position.ELEVATOR_FLOOR.value;
        setPosition(setPosition);
    }

    /**
     * Prevents the user from going past the maximum value of the elevator
     */
    private static void limitPosition() {
        setPosition = Math.min(setPosition, MAX_POSITION + zeroPosition);
        setPosition = Math.max(setPosition, zeroPosition);
        elevator.getPIDController().setReference(setPosition, ControlType.kSmartMotion);
    }

    public static double getVelocity() {
        return elevatorEncoder.getVelocity();
    }
}
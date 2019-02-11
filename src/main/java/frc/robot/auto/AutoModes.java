package frc.robot.auto;

import frc.robot.auto.functions.DriveTime;
import frc.robot.auto.setup.RocketScript;

public class AutoModes {
    private RocketScript rocketScript;

    /**
     * Initializes RocketScript
     */
    public AutoModes() {
        rocketScript = new RocketScript();
    }

    /**
     * Autonomous: crosses the HAB line
     */
    public void crossLine() {
        rocketScript.runFunction(new DriveTime(), 0.5, 2.0);
    }

    /**
     * Runs the set autonomous - must be run in autonomous periodic
     */
    public void runAuto() {
        rocketScript.run();
    }
}
package org.wfrobotics.robot.subsystems;

import org.wfrobotics.reuse.background.BackgroundUpdate;
import org.wfrobotics.reuse.hardware.TalonSRXFactory;
import org.wfrobotics.robot.RobotState;
import org.wfrobotics.robot.commands.lift.LiftAutoZeroThenManual;
import org.wfrobotics.robot.config.LiftHeight;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.LimitSwitchNormal;
import com.ctre.phoenix.motorcontrol.LimitSwitchSource;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.VelocityMeasPeriod;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;


public class LiftSubsystem extends Subsystem implements BackgroundUpdate
{
    private final static double kSprocketDiameterInches = 1.35;  // 1.29 16 tooth 25 chain
    private final static double kTicksPerRev = 4096;
    private final static double kRevsPerInch = 1 / (kSprocketDiameterInches * Math.PI);

    private final LimitSwitchNormal[][] limitSwitchNormally = {
        // forward, then reverse
        {LimitSwitchNormal.NormallyClosed, LimitSwitchNormal.NormallyClosed},
        {LimitSwitchNormal.NormallyClosed, LimitSwitchNormal.NormallyClosed}
    };

    // TODO List of present heights
    // TODO Preset heights in configuration file

    private final RobotState state = RobotState.getInstance();
    private TalonSRX[] motors = new TalonSRX[2];

    private ControlMode desiredMode;
    private double desiredSetpoint;
    private double heightStart;

    public double todoRemoveLast;
    
    private static double kMaxPossibleUp = 2250;
    private static double kMaxPossibleDown = 4000;

    enum LimitSwitch
    {
        Bottom,
        Top
    }

    public LiftSubsystem()
    {
        final int kTimeout = 10;
        final int kSlot = 0;
        final int[] addresses = {11, 10};
        final boolean[] inverted = {true, true};
        final boolean[] sensorPhase = {false, true};

        //final double kP = 0.1 * 1023.0 / 189.7 * 2 * 2 * 2;  // DRL also works if max velocity multiplied by .75 instead of .8
        final double kMaxPossibleVelocity = kMaxPossibleUp;
        final double kP = .1 * 1023.0 / 250.0;
        final double kI = kP * .001;
        final double kD = kP * 10.0;
        final double kF = 1023.0 / kMaxPossibleVelocity;
        final int kMaxVelocity = (int) (kMaxPossibleVelocity * .75);
        final int kAcceleration = (int) (kMaxVelocity * .75);
        // TODO Make into config file?
        // TODO Use talon software limit switches

        for (int index = 0; index < motors.length; index++)
        {
            motors[index] = TalonSRXFactory.makeConstAccelControlTalon(addresses[index], kP, kI, kD, kF, kSlot, kMaxVelocity, kAcceleration);
            motors[index].configForwardLimitSwitchSource(LimitSwitchSource.FeedbackConnector, limitSwitchNormally[index][0], kTimeout);
            motors[index].configReverseLimitSwitchSource(LimitSwitchSource.FeedbackConnector, limitSwitchNormally[index][1], kTimeout);
            motors[index].overrideLimitSwitchesEnable(true);
            motors[index].set(ControlMode.PercentOutput, 0);
            motors[index].setInverted(inverted[index]);
            motors[index].setSensorPhase(sensorPhase[index]);
            motors[index].setNeutralMode(NeutralMode.Brake);
            motors[index].setSelectedSensorPosition(0, kSlot, kTimeout);
            motors[index].configVelocityMeasurementPeriod(VelocityMeasPeriod.Period_100Ms, kTimeout);
            motors[index].configVelocityMeasurementWindow(64, kTimeout);
            motors[index].setStatusFramePeriod(StatusFrameEnhanced.Status_2_Feedback0, 2, kTimeout);
            motors[index].setStatusFramePeriod(StatusFrameEnhanced.Status_10_MotionMagic, 160, kTimeout);
            motors[index].setStatusFramePeriod(StatusFrameEnhanced.Status_13_Base_PIDF0, 2, kTimeout);
            //            motors[index].configAllowableClosedloopError(0, 100, kTimeout);
        }
        desiredMode = ControlMode.PercentOutput;
        desiredSetpoint = 0;
        heightStart = 0;

        todoRemoveLast = Timer.getFPGATimestamp();
    }

    public void initDefaultCommand()
    {
        setDefaultCommand(new LiftAutoZeroThenManual());
    }

    public synchronized void onBackgroundUpdate()
    {
        double todoRemoveNow = Timer.getFPGATimestamp();

        if (zeroPositionIfNeeded())
        {
            SmartDashboard.putString("Lift", "Zeroing");
        }
        else if (goToTransportIfNeeded())
        {
            SmartDashboard.putString("Lift", "Transport");
        }
        //        else if (applyBrakeAtTarget())
        //        {
        //            SmartDashboard.putString("Lift", "Brake at target");
        //        }

        set(desiredMode, desiredSetpoint);

        debug();
        SmartDashboard.putNumber("Background Period", (todoRemoveNow - todoRemoveLast) * 1000);
        todoRemoveLast = todoRemoveNow;
    }

    /**
     * Initialize the Go To Height mode
     * @param heightInches desired height in inches
     */
    public synchronized void goToHeightInit(double heightInches)
    {
        desiredMode = ControlMode.MotionMagic;
        desiredSetpoint = inchesToTicks(heightInches);
        heightStart = getHeight();
    }

    /**
     * Initialize the Go To Speed mode
     * @param percent speed in percent, -1 to 1
     */
    public synchronized void goToSpeedInit(double percent)
    {
        desiredMode = ControlMode.PercentOutput;
        desiredSetpoint = percent;
    }

    /**
     * Set both of the motors
     * @param mode Talon Control Mode
     * @param val
     */
    private void set(ControlMode mode, double val)
    {
        for (int index = 0; index < motors.length; index++)
        {
            motors[index].set(mode, val);
        }
    }

    /**
     * Convert inches to ticks
     * @param inches
     * @return ticks
     */
    private static double inchesToTicks(double inches)
    {
        return inches * kRevsPerInch * kTicksPerRev;
    }

    /**
     * Convert ticks to inches
     * @param ticks
     * @return inches
     */
    private double ticksToInches(double ticks)
    {
        return ticks / kRevsPerInch / kTicksPerRev;
    }

    private double getHeight()
    {
        return (motors[0].getSelectedSensorPosition(0) + motors[1].getSelectedSensorPosition(0)) / 2;
    }

    /**
     * print debug information
     */
    private void debug()
    {
        TalonSRX motor = motors[0];
        double position0 = motors[0].getSelectedSensorPosition(0);
        double position1 = motors[1].getSelectedSensorPosition(0);
        double error0 = motors[0].getClosedLoopError(0);
        double error1 = motors[1].getClosedLoopError(0);

        SmartDashboard.putNumber("Position0", position0);
        SmartDashboard.putNumber("Position1", position1);
        SmartDashboard.putNumber("Velocity", motor.getSelectedSensorVelocity(0));
        SmartDashboard.putNumber("TargetPosition", desiredSetpoint);

        SmartDashboard.putNumber("Error0", error0);
        SmartDashboard.putNumber("Error1", error1);

        SmartDashboard.putNumber("Height", ticksToInches(position0));

        SmartDashboard.putNumber("Delta E", error0 - error1);
        SmartDashboard.putNumber("Delta P", position0 - position1);


        SmartDashboard.putBoolean("AtBottom", isAtBottom());
        SmartDashboard.putBoolean("AtTop", isAtTop());
    }

    /**
     * If we're going fast enough or in high gear, move the lift to Transport height (a safe position)
     * @return true if moved to transport mode
     */
    private boolean goToTransportIfNeeded()
    {
        //        if (state.robotVelocity.getMag() > .5 || state.robotGear)
        //        {
        //            desiredMode = ControlMode.MotionMagic;
        //            desiredSetpoint = inchesToTicks(LiftHeight.Transport.get());
        //            return true;
        //        }
        return false;
    }

    /**
     * Zero the encoder position if both sides are at the bottom
     * @return true if both sides are at the bottom
     */
    private boolean zeroPositionIfNeeded()
    {
        if(isAtBottom())
        {
            for (int index = 0; index < motors.length; index++)
            {
                motors[index].setSelectedSensorPosition(0, 0, 0);
            }

            // Override with valid + safe command
            if (desiredMode == ControlMode.MotionMagic && desiredSetpoint < LiftHeight.Intake.get())
            {
                desiredMode = ControlMode.MotionMagic;
                desiredSetpoint = inchesToTicks(LiftHeight.Intake.get());
            }
            return true;
        }
        return false;
    }

    protected boolean applyBrakeAtTarget()
    {
        if (desiredMode == ControlMode.MotionMagic)
        {
            if (Math.abs(getHeight()) - Math.abs(desiredSetpoint) < Math.abs(heightStart - desiredSetpoint) * .01)
            {
                desiredMode = ControlMode.MotionMagic;
                desiredSetpoint = 0;
                return true;
            }
        }
        return false;
    }

    /**
     * Are all sides at the top?
     * @return
     */
    public boolean isAtTop()
    {
        return isAtLimit(LimitSwitch.Top);
    }

    /**
     * Are all sides at the bottom?
     * @return
     */
    public boolean isAtBottom()
    {
        return isAtLimit(LimitSwitch.Bottom);
    }

    /**
     * Is one side at the top?
     * @param index
     * @return
     */
    public boolean isAtTop(int index)
    {
        return isAtLimit(LimitSwitch.Top, index);
    }

    /**
     * Is one side at the bottom?
     * @param index
     * @return
     */
    public boolean isAtBottom(int index)
    {
        return isAtLimit(LimitSwitch.Bottom, index);
    }

    /**
     * Are all sides at one of the limits?
     * @param limit
     * @return
     */
    public boolean isAtLimit(LimitSwitch limit)
    {
        boolean allAtLimit = true;
        for (int index = 0; index < motors.length; index++)
        {
            allAtLimit &= isAtLimit(limit, index);
        }
        return allAtLimit;
    }

    /**
     * Is one side at one of the limits?
     * @param limit
     * @param index
     * @return
     */
    public boolean isAtLimit(LimitSwitch limit, int index)
    {
        if(limit == LimitSwitch.Bottom)
        {
            return !motors[index].getSensorCollection().isRevLimitSwitchClosed();
        }
        else
        {
            return !motors[index].getSensorCollection().isFwdLimitSwitchClosed();
        }
    }


    // TODO Report fommatted state to RobotState. Not the height, but instead something like what the Robot can do. Ex: isSafeToExhaustScale

    // TODO Automatically zero whenever we pass by that sensor(s)

    // TODO What's the most automatic way we can score on the first layer of cube (on scale/switch) vs the second? What are the easiest xbox controls for that?

    // TODO Beast mode - The fastest lift possible probably dynamically changes it's control strategy to get to it's destination fastest
    //                   This might mean a more aggressive PID (profile) on the way down
    //                   Could go as far as using both closed and open loop control modes
}

package org.wfrobotics.robot;

import org.wfrobotics.reuse.hardware.led.LEDs;
import org.wfrobotics.reuse.hardware.led.LEDs.Color;
import org.wfrobotics.reuse.hardware.led.LEDs.Effect;
import org.wfrobotics.reuse.hardware.led.LEDs.Effect.EFFECT_TYPE;
import org.wfrobotics.reuse.hardware.led.LEDs.LEDController;
import org.wfrobotics.reuse.hardware.led.MindsensorCANLight;
import org.wfrobotics.reuse.hardware.sensors.Gyro;
import org.wfrobotics.reuse.utilities.DashboardView;
import org.wfrobotics.robot.config.Autonomous;
import org.wfrobotics.robot.config.IO;
import org.wfrobotics.robot.config.RobotMap;
import org.wfrobotics.robot.config.Autonomous.AUTO_COMMAND;
import org.wfrobotics.robot.config.Autonomous.POSITION_ROTARY;
import org.wfrobotics.robot.subsystems.Auger;
import org.wfrobotics.robot.subsystems.CameraGear;
import org.wfrobotics.robot.subsystems.CameraShooter;
import org.wfrobotics.robot.subsystems.Climber;
import org.wfrobotics.robot.subsystems.Intake;
import org.wfrobotics.robot.subsystems.Lifter;
import org.wfrobotics.robot.subsystems.Shooter;
import org.wfrobotics.robot.subsystems.SwerveDriveSteamworks;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.SampleRobot;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends SampleRobot
{
    public static SwerveDriveSteamworks driveSubsystem;
    public static Auger augerSubsystem;
    public static Climber climberSubsystem;
    public static DashboardView dashboardView;
    public static Intake intakeSubsystem;
    public static LEDController leds;
    public static IO oi;
    public static Lifter lifterSubsystem;
    public static Shooter shooterSubsystem;
    public static CameraShooter targetShooterSubsystem;
    public static CameraGear targetGearSubsystem;

    Command autonomousCommand;
    SendableChooser<AUTO_COMMAND> autoChooser;
    double startAngle = 0;
    static POSITION_ROTARY autonomousStartPosition;

    boolean gyroInitialZero = false;
    public static Effect defaultLEDEffect;
    public static Effect teamLEDEffect;

    /**
     * This method is run when the robot is first started up and should be used for any initialization code
     */
    public void robotInit()
    {
        driveSubsystem = new SwerveDriveSteamworks();
        augerSubsystem = new Auger();
        targetShooterSubsystem = new CameraShooter();
        targetGearSubsystem = new CameraGear();

        climberSubsystem = new Climber();
        dashboardView = new DashboardView();
        intakeSubsystem = new Intake();
        lifterSubsystem = new Lifter(true);
        shooterSubsystem = new Shooter();
        leds = new MindsensorCANLight(RobotMap.CAN_LIGHT);
        //leds.enable(false); // TODO Remove this when we have LEDs on the robot!!!

        oi = new IO();  // IMPORTANT: Initialize OI after subsystems, so all subsystem parameters passed to commands are initialized

        autoChooser = new SendableChooser<AUTO_COMMAND>();

        autoChooser.addDefault("Auto None", AUTO_COMMAND.NONE); // TODO pick gear/shoot as the default autonomous command
        autoChooser.addObject("Auto Forward (LOW GEAR)", AUTO_COMMAND.DRIVE);
        autoChooser.addObject("Auto Forward  (HIGH GEAR)", AUTO_COMMAND.DRIVE_HG);
        //autoChooser.addObject("Auto Shoot (NOT WORKING YET)", AUTO_COMMAND.SHOOT);
        autoChooser.addObject("Auto Shoot then Hopper", AUTO_COMMAND.SHOOT_THEN_HOPPER);
        autoChooser.addObject("Auto Shoot then Gear", AUTO_COMMAND.SHOOT_THEN_GEAR);
        autoChooser.addObject("Auto Gear Vision", AUTO_COMMAND.GEAR_VISION);
        //autoChooser.addObject("Auto Gear Dead Reckoning", AUTO_COMMAND.GEAR_DR);
        SmartDashboard.putData("Auto Mode", autoChooser);

        defaultLEDEffect = new Effect(EFFECT_TYPE.FADE, LEDs.COLORS_THE_HERD, 1);
    }

    public void operatorControl()
    {
        if (autonomousCommand != null) autonomousCommand.cancel();

        leds.set(defaultLEDEffect);

        while (isOperatorControl() && isEnabled())
        {
            driveSubsystem.printDash();
            SmartDashboard.putNumber("Battery", DriverStation.getInstance().getBatteryVoltage());
            Scheduler.getInstance().run();
        }
    }

    public void autonomous()
    {
        DriverStation.Alliance team = DriverStation.getInstance().getAlliance();
        Color[] teamDefaultColors = (team == DriverStation.Alliance.Red) ? LEDs.COLORS_RED_ALLIANCE : LEDs.COLORS_BLUE_ALLIANCE;
        AUTO_COMMAND command =  autoChooser.getSelected();

        autonomousCommand = command.getCommand(autonomousStartPosition);

        // Zero the Gyro based on starting orientation of the selected autonomous mode
        Gyro.getInstance().zeroYaw(command.getGyroOffset(autonomousStartPosition));
        Robot.driveSubsystem.setLastHeading(command.getGyroOffset(autonomousStartPosition));

        Robot.leds.set(new Effect(EFFECT_TYPE.CYCLE, teamDefaultColors, 1));

        // Schedule the autonomous command
        if (autonomousCommand != null) autonomousCommand.start();

        while (isAutonomous() && isEnabled())
        {
            driveSubsystem.printDash();
            SmartDashboard.putNumber("Battery", DriverStation.getInstance().getBatteryVoltage());
            Scheduler.getInstance().run();
        }
    }

    public void disabled()
    {
        //leds.set(new Effect(EFFECT_TYPE.FADE, colors, 4));
        leds.set(defaultLEDEffect);

        while (isDisabled())
        {
            lifterSubsystem.disabled();
            autonomousStartPosition = Autonomous.getRotaryStartingPosition();
            disabledDoGyro();

            AUTO_COMMAND command =  autoChooser.getSelected();
            SmartDashboard.putNumber("StartingAngle", command.getGyroOffset(autonomousStartPosition));
            SmartDashboard.putBoolean("TeamRed", DriverStation.getInstance().getAlliance() == Alliance.Red);

            driveSubsystem.printDash();
            SmartDashboard.putNumber("Battery", DriverStation.getInstance().getBatteryVoltage());

            Scheduler.getInstance().run();
        }
    }

    public void test()
    {
        while (isTest() && isEnabled())
        {
            LiveWindow.run();
        }
    }

    private void disabledDoGyro()
    {
        // It takes some time before the gyro initializes so we have to wait before we can actually zero the first time
        if(!gyroInitialZero)
        {
            if(Math.abs(Gyro.getInstance().getYaw()) > 0.1)
            {
                Gyro.getInstance().zeroYaw();
                gyroInitialZero = true;
            }
        }
        else
        {
            if(IO.xboxDrive.getStartButton())
                Gyro.getInstance().zeroYaw();
            //Gyro.getInstance().zeroYaw();
        }
    }
}

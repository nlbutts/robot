package com.taurus.robot;

import edu.wpi.first.wpilibj.GenericHID.Hand;
import edu.wpi.first.wpilibj.buttons.Button;

import com.taurus.controller.Xbox;
import com.taurus.controller.XboxButton;
import com.taurus.commands.*;

/**
 * This class is the glue that binds the controls on the physical operator
 * interface to the commands and command groups that allow control of the robot.
 */
public class OI 
{
    static Xbox xbox1 = new Xbox(0);
    static Xbox xbox2 = new Xbox(1);
    
    //first controller
    Button buttonA1 = new XboxButton(xbox1, Xbox.ButtonType.kA);
    Button buttonB1 = new XboxButton(xbox1, Xbox.ButtonType.kB);

    Button buttonRB1 = new XboxButton(xbox1, Xbox.ButtonType.kRB);
    Button buttonLB1 = new XboxButton(xbox1, Xbox.ButtonType.kLB);
    
    Button buttonY1 = new XboxButton(xbox1, Xbox.ButtonType.kY);
    Button buttonX1 = new XboxButton(xbox1, Xbox.ButtonType.kX);

    Button buttonBack1 = new XboxButton(xbox1, Xbox.ButtonType.kBack);
    Button buttonStart1 = new XboxButton(xbox1, Xbox.ButtonType.kStart);
    
    //second controller
    Button buttonA2 = new XboxButton(xbox2, Xbox.ButtonType.kA);
    Button buttonB2 = new XboxButton(xbox2, Xbox.ButtonType.kB);

    Button buttonRB2 = new XboxButton(xbox2, Xbox.ButtonType.kRB);
    Button buttonLB2 = new XboxButton(xbox2, Xbox.ButtonType.kLB);
    
    Button buttonY2 = new XboxButton(xbox2, Xbox.ButtonType.kY);
    Button buttonX2 = new XboxButton(xbox2, Xbox.ButtonType.kX);

    Button buttonBack2 = new XboxButton(xbox2, Xbox.ButtonType.kBack);
    Button buttonStart2 = new XboxButton(xbox2, Xbox.ButtonType.kStart);
    
    public OI() 
    {
        buttonBack1.toggleWhenPressed(new DriveArcadeWithXbox());
        
        buttonA2.whileHeld(new ShooterGrab());
        buttonB2.whenPressed(new ShooterFire());

        buttonRB2.whenPressed(new LiftToTop());
        buttonLB2.whenPressed(new LiftToBottom());
        buttonStart2.toggleWhenPressed(new LiftStop());
        
        buttonX2.whileHeld(new AimerCW_Continous());
        buttonY2.whileHeld(new AimerCWC_Continous());
       
    }
    
    public static boolean getTractionControl()
    {
        return xbox1.getTrigger(Hand.kRight);
    }
    
    public static double getSpeedLeft()
    {
        return xbox1.getY(Hand.kLeft);
    }
    
    public static double getSpeedRight()
    {
        return xbox1.getY(Hand.kRight);        
    }
    
    public static double getThrottleY()
    {
      return xbox1.getY(Hand.kLeft);
    }
    
    public static double getThrottleX()
    {
        return xbox1.getX(Hand.kLeft);
    }
     
    public static double getThrottleHighSpeed()
    {
        return xbox1.getTriggerVal(Hand.kLeft);
    }
    
    public static double getShooterSpeedAdjust()
    {
        return xbox2.getTriggerVal(Hand.kRight);
    }
    
    public static int getDpad2()
    {
        return xbox2.getPOV(0);
    }
    
}


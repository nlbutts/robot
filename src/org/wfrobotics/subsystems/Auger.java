package org.wfrobotics.subsystems;

import org.wfrobotics.commands.Conveyer;
import org.wfrobotics.robot.RobotMap;

import com.ctre.CANTalon;

import edu.wpi.first.wpilibj.command.Subsystem;

/**
 * Screw conveyer or auger conveyer
 * This subsystem controls the flighting/auger that acts as a ball conveyer
 *
 */
public class Auger extends Subsystem {

    private CANTalon m_motor; 
    
    public Auger() 
    {
        m_motor = new CANTalon(RobotMap.AUGER_MOTOR);
        m_motor.setInverted(true);  //is this needed?
    }
    
    @Override
    protected void initDefaultCommand()
    {
        setDefaultCommand(new Conveyer(Conveyer.MODE.OFF));
    }
    
    /**
     * control speed of the auger wheels
     * @param rpm speed of the motor
     */
    public void setSpeed (double rpm)
    {
        m_motor.set(rpm);
    }
}
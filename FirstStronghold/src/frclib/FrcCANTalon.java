/*
 * Titan Robotics Framework Library
 * Copyright (c) 2015 Titan Robotics Club (http://www.titanrobotics.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package frclib;

import edu.wpi.first.wpilibj.CANTalon;
import hallib.HalMotorController;

public class FrcCANTalon extends CANTalon implements HalMotorController
{
    private boolean feedbackDeviceIsPot = false;
    private boolean limitSwitchesSwapped = false;
    private boolean revLimitSwitchNormalOpen = false;
    private boolean fwdLimitSwitchNormalOpen = false;
    private double zeroPosition = 0.0;
    private boolean softLowerLimitEnabled = false;
    private boolean softUpperLimitEnabled = false;
    private double softLowerLimit = 0.0;
    private double softUpperLimit = 0.0;

    /**
     * Constructor: Create an instance of the object.
     *
     * @param deviceNumber specifies the CAN ID of the device.
     * @param controlPeriodMs specifies the period in msec to send the CAN control frame.
     *                        Period is bounded to 1 msec to 95 msec.
     * @param enablePeriodMs specifies the period in msec to send the enable control frame.
     *                       Period is bounded to 1 msec to 95 msec. This typically is not
     *                       required to specify, however, this could be used to minimize the
     *                       time between robot-enable and talon-motor-drive.
     */
    public FrcCANTalon(int deviceNumber, int controlPeriodMs, int enablePeriodMs)
    {
        super(deviceNumber, controlPeriodMs, enablePeriodMs);
        resetPosition();
    }   //FrcCANTalon

    /**
     * Constructor: Create an instance of the object.
     *
     * @param deviceNumber specifies the CAN ID of the device.
     * @param controlPeriodMs specifies the period in msec to send the CAN control frame.
     *                        Period is bounded to 1 msec to 95 msec.
     */
    public FrcCANTalon(int deviceNumber, int controlPeriodMs)
    {
        super(deviceNumber, controlPeriodMs);
        resetPosition();
    }   //FrcCANTalon

    /**
     * Constructor: Create an instance of the object.
     *
     * @param deviceNumber specifies the CAN ID of the device.
     */
    public FrcCANTalon(int deviceNumber)
    {
        super(deviceNumber);
        resetPosition();
    }   //FrcCANTalon

    public void setLimitSwitchesSwapped(boolean swapped)
    {
        limitSwitchesSwapped = swapped;
    }   //setLimitSwitchesSwapped
    
    @Override
    public void ConfigFwdLimitSwitchNormallyOpen(boolean normalOpen)
    {
        super.ConfigFwdLimitSwitchNormallyOpen(normalOpen);
        fwdLimitSwitchNormalOpen = normalOpen;
    }
    
    @Override
    public void ConfigRevLimitSwitchNormallyOpen(boolean normalOpen)
    {
        super.ConfigRevLimitSwitchNormallyOpen(normalOpen);
        revLimitSwitchNormalOpen = normalOpen;
    }

    @Override
    public void setFeedbackDevice(FeedbackDevice devType)
    {
        super.setFeedbackDevice(devType);
        feedbackDeviceIsPot = devType == FeedbackDevice.AnalogPot;
    }

    //
    // Implements HalMotorController interface.
    //

    /**
     * This method returns the state of the motor controller direction.
     *
     * @return true if the motor direction is inverted, false otherwise.
     */
    @Override
    public boolean getInverted()
    {
        return super.getInverted();
    }   //getInverted

    /**
     * This method returns the motor position by reading the position sensor. The position
     * sensor can be an encoder or a potentiometer.
     *
     * @return current motor position.
     */
    @Override
    public double getPosition()
    {
        double pos = super.getPosition();
        
        if (feedbackDeviceIsPot)
        {
            pos -= zeroPosition;
        }

        return pos;
    }   //getPosition

    /**
     * This method returns the speed of the motor rotation.
     *
     * @return motor rotation speed.
     */
    @Override
    public double getSpeed()
    {
        return super.getSpeed();
    }   //getSpeed

    /**
     * This method returns the state of the lower limit switch.
     *
     * @return true if lower limit switch is active, false otherwise.
     */
    @Override
    public boolean isLowerLimitSwitchActive()
    {
        return limitSwitchesSwapped? !(fwdLimitSwitchNormalOpen^isFwdLimitSwitchClosed()):
                                     !(revLimitSwitchNormalOpen^isRevLimitSwitchClosed());
    }   //isLowerLimitSwitchClosed

    /**
     * This method returns the state of the upper limit switch.
     *
     * @return true if upper limit switch is active, false otherwise.
     */
    @Override
    public boolean isUpperLimitSwitchActive()
    {
        return limitSwitchesSwapped? !(revLimitSwitchNormalOpen^isRevLimitSwitchClosed()):
                                     !(fwdLimitSwitchNormalOpen^isFwdLimitSwitchClosed());
    }   //isUpperLimitSwitchActive

    /**
     * This method resets the motor position sensor, typically an encoder.
     */
    public void resetPosition()
    {
        if (feedbackDeviceIsPot)
        {
            zeroPosition = super.getPosition();
        }
        else
        {
            super.setPosition(0.0);
        }
    }   //resetPosition

    /**
     * This method enables/disables motor brake mode. In motor brake mode, set power to 0 would
     * stop the motor very abruptly by shorting the motor wires together using the generated
     * back EMF to stop the motor. When brakMode is false (i.e. float/coast mode), the motor wires
     * are just disconnected from the motor controller so the motor will stop gradually.
     *
     * @param brakeMode specifies true to enable brake mode, false otherwise.
     */
    public void setBrakeModeEnabled(boolean enabled)
    {
        super.enableBrakeMode(enabled);
    }   //setBrakeModeEnabled

    /**
     * This method inverts the motor direction.
     *
     * @param inverted specifies true to invert motor direction, false otherwise.
     */
    @Override
    public void setInverted(boolean inverted)
    {
        super.setInverted(inverted);
    }   //setInverted

    /**
     * This method sets the output power of the motor controller.
     *
     * @param output specifies the output power for the motor controller in the range of
     * -1.0 to 1.0.
     */
    public void setPower(double power)
    {
        if (softLowerLimitEnabled && power < 0.0 && getPosition() <= softLowerLimit ||
            softUpperLimitEnabled && power > 0.0 && getPosition() >= softUpperLimit)
        {
            power = 0.0;
        }

        super.set(power);
    }   //setPower

    /**
     * This method inverts the position sensor direction. This may be rare but
     * there are scenarios where the motor encoder may be mounted somewhere in
     * the power train that it rotates opposite to the motor rotation. This will
     * cause the encoder reading to go down when the motor is receiving positive
     * power. This method can correct this situation.
     *
     * @param inverted specifies true to invert position sensor direction,
     *                 false otherwise.
     */
    public void setPositionSensorInverted(boolean inverted)
    {
        super.reverseSensor(inverted);
    }   //setPositionSensorInverted

    /**
     * This method enables/disables soft limit switches.
     *
     * @param lowerLimitEnabled specifies true to enable lower soft limit switch, false otherwise.
     * @param upperLimitEnabled specifies true to enable upper soft limit switch, false otherwise.
     */
    public void setSoftLimitEnabled(boolean lowerLimitEnabled, boolean upperLimitEnabled)
    {
        softLowerLimitEnabled = lowerLimitEnabled;
        softUpperLimitEnabled = upperLimitEnabled;
    }   //setSoftLimitEnabled

    /**
     * This method sets the lower soft limit.
     *
     * @param position specifies the position of the lower limit.
     */
    public void setSoftLowerLimit(double position)
    {
        softLowerLimit = position;
    }   //setSoftLowerLimit

    /**
     * This method sets the upper soft limit.
     *
     * @param position specifies the position of the upper limit.
     */
    public void setSoftUpperLimit(double position)
    {
        softUpperLimit = position;
    }   //setSoftUpperLimit

}   //class FrcCANTalon

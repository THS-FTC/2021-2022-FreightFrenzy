/*
- 1 is first
Controls:
    Gamepad 1 (all random functions):
        left BUMPER BUTTON - reset all carousel and intake servos
        right BUMPER BUTTON - turn on the carousel motor
        left TRIGGER - intake forward
        right TRIGGER - intake backward
        x button - arm 1st position
        a button - arm 2nd position
        b button - arm 3rd position
    Gamepad 2 (drive):
        left analog stick - all controls that make logical
        right analog stick - radial turns in logical order and drift
        right BUMPER BUTTON - turn all motors off

Point sequence
    1st part:
        score freight to shipping hub level 3
    End Game:
        deliver all ducks
        fully park in warehouse
        if extra time:
            ***try to have shared shipping hub touching floor on our side

- Servo_3 = claw tilt
- Servo_4 = claw open/close
*/
//import FTC packages and all needed libraries for opencv, vuforia, etc
package org.firstinspires.ftc.teamcode.vision;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.LED;
import com.qualcomm.robotcore.hardware.Light;
import com.qualcomm.robotcore.hardware.CRServo;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.Blinker;
import com.qualcomm.robotcore.hardware.Gyroscope;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;
import java.util.concurrent.TimeUnit;
import java.util.Date;
//import com.vuforia.Vuforia;
//import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;

@TeleOp//set code mode to TeleOp (driver control)

public class oneRed extends LinearOpMode {
    //junction height values represented as motor encoder values for 4-stage Viper Slide Kit
    int groundJunction = 600;
    int lowJunction = 2900;
    int midJunction = 5400;
    int highJunction = 5400;
    double slideSpeed = 2250.0;//2787 PPR is max encoder PPR of Gobilda 435 rpm motor
    int armTarget = 0;//as encoder values
    int slidePosition = 0;
    boolean calibrated = false;
    //hardware classes + names
    private Blinker Control_Hub;//NEEDED - DON'T DELETE!!
    private DcMotorEx Motor_1;//front left
    private DcMotorEx Motor_2;//front right
    private DcMotorEx Motor_3;//back left
    private DcMotorEx Motor_4;//back right
    private DcMotorEx armMotor;
    private Gyroscope imu;
    private Servo intakeServo;
    //motor variables for mecanum drive
    double motor_reduction = 0.4;//for drivetrain
    double motor_1_pwr = 0.0;
    double motor_2_pwr = 0.0;
    double motor_3_pwr = 0.0;
    double motor_4_pwr = 0.0;
    double motor_denom;
    //inputs from controllers
    double left_stick2_x;//triggers and bumpers
    double left_stick2_y;
    double right_stick2_x;
    /*double left_stick1_y;
    boolean left_bump1;
    boolean right_bump1;
    boolean left_bump2;
    boolean right_bump2;
    double left_trig1;
    double right_trig1;*/
    double left_trig2;
    double right_trig2;
    boolean a1;//a,b,x,y buttons
    boolean b1;
    boolean x1;
    boolean y1;
    boolean a2;
    boolean b2;
    boolean x2;
    //boolean y2;
    //boolean dpad_left;
    //boolean dpad_down;
    //boolean dpad_up;
    //boolean dpad_right;

    @Override
    public void runOpMode() {
        //hardware intializing
        Control_Hub = hardwareMap.get(Blinker.class, "Control Hub");
        Motor_1 = hardwareMap.get(DcMotorEx.class, "Motor_1");
        Motor_2 = hardwareMap.get(DcMotorEx.class, "Motor_2");
        Motor_3 = hardwareMap.get(DcMotorEx.class, "Motor_3");
        Motor_4 = hardwareMap.get(DcMotorEx.class, "Motor_4");
        armMotor = hardwareMap.get(DcMotorEx.class, "armMotor");
        intakeServo = hardwareMap.get(Servo.class, "intakeServo");//to move the claw, grab the cone.
        //setting hardware directions, etc
        Motor_1.setDirection(DcMotorEx.Direction.REVERSE);
        Motor_3.setDirection(DcMotorEx.Direction.REVERSE);
        Motor_2.setDirection(DcMotorEx.Direction.FORWARD);
        Motor_4.setDirection(DcMotorEx.Direction.FORWARD);
        armMotor.setDirection(DcMotorSimple.Direction.REVERSE);
        /*Motor_1.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        Motor_2.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        Motor_3.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        Motor_4.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);*/
        //imu = hardwareMap.get(Gyroscope.class, "imu");
        telemetry.addData("Status", "Initialized");
        //while (calibrated == false){//calibrate slide motor to encoder val = 0
        //    slideCalibrate();
        //}
        //sleep(1500);
        armMotor.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);//reset encoder of slideMotor when slide is fully retracted to encoder = 0
        sleep(50);
        armMotor.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        telemetry.addData("armEncoder", armMotor.getCurrentPosition());
        telemetry.addData("intake servo", intakeServo.getPosition());
        telemetry.update();
        armMotor.setTargetPosition(0);//make sure the slide starts at position = 0
        armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        armMotor.setVelocity(slideSpeed);
        //intakeServo.setPosition(20.0/270.0);//"zero" the intake servo
        waitForStart();

        //main loop. call functions that do different tasks
        while (opModeIsActive()) {
            normal_motor();//mecanum wheel drive
            intake();//operate intake 180deg turns
            slide();//operate slide up/down
            //telemetry.addData("motor1", left_stick2_y);
            telemetry.update();//send telemetry data to driver hub
        }

    }





    void normal_motor(){//mecanum wheel motor control maths + telemetry
        right_stick2_x = this.gamepad2.right_stick_x;
        left_stick2_x = this.gamepad2.left_stick_x;
        left_stick2_y = -this.gamepad2.left_stick_y;
        //drivetrain
        telemetry.addData("lefty:", left_stick2_y);
        motor_denom = Math.max(Math.abs(left_stick2_y) + Math.abs(left_stick2_x) + Math.abs(right_stick2_x), 1.0);
        motor_1_pwr = (left_stick2_y + left_stick2_x + right_stick2_x)/motor_denom;//LF
        motor_2_pwr = (left_stick2_y - left_stick2_x - right_stick2_x)/motor_denom;//RF
        motor_3_pwr = (left_stick2_y - left_stick2_x + right_stick2_x)/motor_denom;//LB
        motor_4_pwr = (left_stick2_y + left_stick2_x - right_stick2_x)/motor_denom;//LR
        Motor_1.setPower(motor_1_pwr  * motor_reduction);
        Motor_2.setPower(motor_2_pwr  * motor_reduction);
        Motor_3.setPower(motor_3_pwr  * motor_reduction);
        Motor_4.setPower(motor_4_pwr  * motor_reduction);
        telemetry.addData("motor_1", motor_1_pwr);
        telemetry.addData("motor_2", motor_2_pwr);
        telemetry.addData("motor_3", motor_3_pwr);
        telemetry.addData("motor_4", motor_4_pwr);
        telemetry.addData("encoder-left", Motor_1.getCurrentPosition());
        telemetry.addData("encoder-mid", Motor_2.getCurrentPosition());
        telemetry.addData("encoder-right", Motor_3.getCurrentPosition());
        //telemetry.update();
    }

    void slideCalibrate(){//not used
        armMotor.setPower(-0.75);
        //sleep(100);
        telemetry.addData("velocity", armMotor.getVelocity(AngleUnit.DEGREES));
        telemetry.update();
        if (armMotor.getVelocity(AngleUnit.DEGREES) >= -0.05 && calibrated == false){
            calibrated = true;
            armMotor.setPower(0.0);
        }
        if (calibrated == true) {
            telemetry.addData("calibrated", true);
            telemetry.addData("armEncoder:", armMotor.getCurrentPosition());
            telemetry.update();
        }
    }

    void slide(){//make slide move up/down using encoder values to calculate position
        left_trig2 = this.gamepad2.left_trigger;
        right_trig2 = this.gamepad2.right_trigger;
        telemetry.addData("righttrig", right_trig2);
        if(left_trig2 > 0.0 && armTarget > 0){
            armTarget -= 20;
            //armMotor.setTargetPosition(armTarget);
        }
        else if(right_trig2 > 0.0 && armTarget <= 2900){
            armTarget += 20;
            //armMotor.setTargetPosition(armTarget);
            telemetry.addData("targetpos", armMotor.getTargetPosition());
        }
        /*if (armMotor.getCurrentPosition() > armTarget){
            armMotor.setPower(0.1);
        }
        else{*/
            armMotor.setTargetPosition(armTarget);
            armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            armMotor.setVelocity(slideSpeed);
        //}
        telemetry.addData("armTarget: ", armTarget);
        telemetry.addData("armPos: ", armMotor.getCurrentPosition());
        //telemetry.update();
    }

    void intake(){//turn the entire intake mechanism around 180 degrees
        x2 = this.gamepad2.x;
        b2 = this.gamepad2.b;

        if (b2 && armMotor.getCurrentPosition() >= 675){//675 is predetermined as the height to clear the motors
            intakeServo.setPosition(200.0/270.0);
        }
        if (x2 && armMotor.getCurrentPosition() >= 675){//675 is predetermined as the height to clear the motors
            intakeServo.setPosition(20.0/270.0);
        }
        telemetry.addData("intakeServoPos: ", intakeServo.getPosition());
    }
}



package org.firstinspires.ftc.teamcode.components.live;

import android.graphics.Color;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.ColorRangeSensor;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.components.Component;
import org.firstinspires.ftc.teamcode.robots.Robot;
import org.firstinspires.ftc.teamcode.util.qus.DcMotorQUS;
import org.firstinspires.ftc.teamcode.util.qus.ServoQUS;

@Config
class IntakeConfig {
    // Servo position for the grabber in the open state
    public static double GRABBER_OPEN = 0;
    // Servo position for the grabber in the closed state
    public static double GRABBER_CLOSED = 1;

    public static double CRADLE_INTAKE_POSITION = 0.1;
    public static double CRADLE_MOVE_POSITION = 0.15;
    public static double CRADLE_LIFT_POSITION = 0.25;
    public static double CRADLE_DUMP_POSITION = 0.4;
    public static double CRADLE_HALF_DUMP_POSITION = 0.33;

    public static int COLOR_SENSOR_UPDATE_INTERVAL = 5;
    public static int COLOR_SENSOR_DISTANCE_CUTOFF = 75; // mm
}

public class Intake extends Component {
    /**
     * Component for retrieving elements (blocks, balls, ducks, and the team marker) from the ground, and depositing them into their scoring positions
     */

    //// MOTORS ////
    public DcMotorQUS spinner;

    //// SERVOS ////
    public ServoQUS grabber;
    public ServoQUS cradle;

    //// SENSORS ////
    public ColorRangeSensor color_sensor;
    public boolean color_sensor_enabled = false;
    public double last_distance = 0;

    {
        name = "Intake";
    }

    public Intake(Robot robot)
    {
        super(robot);
    }

    @Override
    public void registerHardware (HardwareMap hwmap)
    {
        super.registerHardware(hwmap);

        //// MOTORS ////
        spinner = new DcMotorQUS(hwmap.get(DcMotorEx.class, "spinner"));

        //// SERVOS ////
        grabber = new ServoQUS(hwmap.get(Servo.class, "grabber"));
        cradle = new ServoQUS(hwmap.get(Servo.class, "cradle"));

        //// SENSORS ////
        color_sensor = hwmap.get(ColorRangeSensor.class, "color_sensor");
    }

    @Override
    public void update(OpMode opmode) {
        super.update(opmode);

        spinner.update();
        grabber.update();
        cradle.update();

        if (color_sensor_enabled && robot.cycle % IntakeConfig.COLOR_SENSOR_UPDATE_INTERVAL == 0) {
            last_distance = color_sensor.getDistance(DistanceUnit.MM);
        }
    }

    @Override
    public void startup() {
        super.startup();

        spinner.motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        spinner.motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        cradle_move();
    }

    public void shutdown() {
        spinner.queue_power(0);
    }

    @Override
    public void updateTelemetry(Telemetry telemetry) {
        super.updateTelemetry(telemetry);

        telemetry.addData("PROX", last_distance);
        telemetry.addData("HAS", has_element());
    }

    public void spin(double speed) {
        /**
         * Spin the spinner motor at a specific speed
         */
        spinner.queue_power(speed);
    }

    public void grab() {
        /**
         * Set the grabber servo to its grabbed position
         */
        grabber.queue_position(IntakeConfig.GRABBER_CLOSED);
    }

    public void ungrab() {
        /**
         * Set the grabber servo to its ungrabbed position
         */
        grabber.queue_position(IntakeConfig.GRABBER_OPEN);
    }

    public void cradle_intake() {
        cradle.queue_position(IntakeConfig.CRADLE_INTAKE_POSITION);
    }

    public void cradle_move() {
        cradle.queue_position(IntakeConfig.CRADLE_MOVE_POSITION);
    }

    public void cradle_lift() {
        cradle.queue_position(IntakeConfig.CRADLE_LIFT_POSITION);
    }

    public void cradle_dump() {
        cradle.queue_position(IntakeConfig.CRADLE_DUMP_POSITION);
    }

    public void cradle_half_dump() { cradle.queue_position(IntakeConfig.CRADLE_HALF_DUMP_POSITION); }

    public void enable_color_sensor(boolean enabled) {
        if (enabled) {
            last_distance = color_sensor.getDistance(DistanceUnit.MM);
        }
        color_sensor_enabled = enabled;
    }

    public boolean has_element() {
        return last_distance <= IntakeConfig.COLOR_SENSOR_DISTANCE_CUTOFF;
    }
}
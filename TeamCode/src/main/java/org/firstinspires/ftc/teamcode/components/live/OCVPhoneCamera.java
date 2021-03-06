package org.firstinspires.ftc.teamcode.components.live;

import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.components.Component;
import org.firstinspires.ftc.teamcode.robots.Robot;
import org.firstinspires.ftc.teamcode.util.MathUtil;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.IntStream;

import static org.opencv.core.CvType.CV_8UC1;

@Config
class OCVPhoneCameraConfig {
    // Normalized offset of center rect x from left of image
    public static double rect_offset_x = 0.55;
    // Normalized offset of center rect y from top of image
    public static double rect_offset_y = 0.50;
    // Normalized x distance from left rect / right rect to center rect
    public static double rect_separation = 0.25;
    // Width of all rects
    public static double rect_size = 0.05;
}

public class OCVPhoneCamera extends Component {

    OpenCvCamera phone_camera;

    public CapstonePipline capstone_pipline;

    private boolean streaming;

    {
        name = "Phone Camera (OCV)";
    }

    public OCVPhoneCamera(Robot robot) {
        super(robot);
    }

    @Override
    public void registerHardware(HardwareMap hwmap) {
        // Load and get an instance of the phone camera from the hardware map
        int cameraMonitorViewId = hwmap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hwmap.appContext.getPackageName());
        phone_camera = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
    }

    @Override
    public void startup() {
        super.startup();

        phone_camera.openCameraDevice();

        // Instantiate all possible pipelines
        capstone_pipline = new CapstonePipline();

        // Load the default pipeline
        set_pipeline(capstone_pipline);
    }

    @Override
    public void updateTelemetry(Telemetry telemetry) {
        super.updateTelemetry(telemetry);
        telemetry.addData("FRAME", phone_camera.getFrameCount());
        telemetry.addData("FPS", String.format("%.2f", phone_camera.getFps()));
        telemetry.addData("L SAT", capstone_pipline.sat[0]);
        telemetry.addData("M SAT", capstone_pipline.sat[0]);
        telemetry.addData("R SAT", capstone_pipline.sat[0]);
        telemetry.addData("PATTERN", capstone_pipline.pattern);
    }

    public void set_pipeline(OpenCvPipeline pl) {
        phone_camera.setPipeline(pl);
    }

    public void start_streaming() {
        /**
         * Start the camera device and display the output of the camera to the robot controller phone
         */
        if (!streaming) {
            phone_camera.startStreaming(320, 240, OpenCvCameraRotation.UPRIGHT);
            streaming = true;
        }
    }

    public int get_randomization_pattern() {
        /**
         * Get the current pattern from the team marker pipeline
         */
        return capstone_pipline.pattern;
    }

    public void stop_streaming() {
        /**
         * Close the camera device, stop sending output through the pipeline
         */
        phone_camera.stopStreaming();
        streaming = false;
    }

    class CapstonePipline extends OpenCvPipeline {
        /**
         * Pipeline for recognizing position of team marker on the 3 pieces of tape, using the color saturation
         */

        int[] sat = new int[3];
        int pattern;

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public Mat processFrame(Mat input) {

            input.convertTo(input, CV_8UC1, 1, 10);

            // Denormalize positions and sizes of the 3 rects
            int[] r_rect = {
                    (int) (input.cols() * (OCVPhoneCameraConfig.rect_offset_x - OCVPhoneCameraConfig.rect_size/2)),
                    (int) (input.rows() * (OCVPhoneCameraConfig.rect_offset_y - OCVPhoneCameraConfig.rect_size/2 + OCVPhoneCameraConfig.rect_separation)),
                    (int) (input.cols() * (OCVPhoneCameraConfig.rect_offset_x + OCVPhoneCameraConfig.rect_size/2)),
                    (int) (input.rows() * (OCVPhoneCameraConfig.rect_offset_y + OCVPhoneCameraConfig.rect_size/2 + OCVPhoneCameraConfig.rect_separation))
            };

            int[] m_rect = {
                    (int) (input.cols() * (OCVPhoneCameraConfig.rect_offset_x - OCVPhoneCameraConfig.rect_size/2)),
                    (int) (input.rows() * (OCVPhoneCameraConfig.rect_offset_y - OCVPhoneCameraConfig.rect_size/2)),
                    (int) (input.cols() * (OCVPhoneCameraConfig.rect_offset_x + OCVPhoneCameraConfig.rect_size/2)),
                    (int) (input.rows() * (OCVPhoneCameraConfig.rect_offset_y + OCVPhoneCameraConfig.rect_size/2))
            };

            int[] l_rect = {
                    (int) (input.cols() * (OCVPhoneCameraConfig.rect_offset_x - OCVPhoneCameraConfig.rect_size/2)),
                    (int) (input.rows() * (OCVPhoneCameraConfig.rect_offset_y - OCVPhoneCameraConfig.rect_size/2 - OCVPhoneCameraConfig.rect_separation)),
                    (int) (input.cols() * (OCVPhoneCameraConfig.rect_offset_x + OCVPhoneCameraConfig.rect_size/2)),
                    (int) (input.rows() * (OCVPhoneCameraConfig.rect_offset_y + OCVPhoneCameraConfig.rect_size/2 - OCVPhoneCameraConfig.rect_separation))
            };

            // Load the rects into matrices
            Mat l_mat = input.submat(l_rect[1], l_rect[3], l_rect[0], l_rect[2]);
            Mat m_mat = input.submat(m_rect[1], m_rect[3], m_rect[0], m_rect[2]);
            Mat r_mat = input.submat(r_rect[1], r_rect[3], r_rect[0], r_rect[2]);

            // Get the average color of each rect
            Scalar l_mean = Core.mean(l_mat);
            Scalar m_mean = Core.mean(m_mat);
            Scalar r_mean = Core.mean(r_mat);

            // Convert the RGB color to HSV color
            float[] l_hsv = new float[3];
            float[] m_hsv = new float[3];
            float[] r_hsv = new float[3];
            Color.RGBToHSV((int) l_mean.val[0], (int) l_mean.val[1], (int) l_mean.val[2], l_hsv);
            Color.RGBToHSV((int) m_mean.val[0], (int) m_mean.val[1], (int) m_mean.val[2], m_hsv);
            Color.RGBToHSV((int) r_mean.val[0], (int) r_mean.val[1], (int) r_mean.val[2], r_hsv);

            // Get the saturation (S) of each HSV color
            sat[0] = (int)(100.0 * l_hsv[1]);
            sat[1] = (int)(100.0 * m_hsv[1]);
            sat[2] = (int)(100.0 * r_hsv[1]);

            // Find which rect has the least saturation
            int min_index = 0;
            for (int i = 0; i < sat.length; i++) {
                min_index = sat[i] < sat[min_index] ? i : min_index;
            }

            // The pattern is the index of the rect + 1, as 0 is reserved for a null output
            pattern = min_index + 1;

            // Draw the rects on he image for visualization and lineup purposess
            Imgproc.rectangle(input, new Point(l_rect[0], l_rect[1]), new Point(l_rect[2], l_rect[3]), new Scalar(0, 0, 255), 1);
            Imgproc.rectangle(input, new Point(m_rect[0], m_rect[1]), new Point(m_rect[2], m_rect[3]), new Scalar(0, 0, 255), 1);
            Imgproc.rectangle(input, new Point(r_rect[0], r_rect[1]), new Point(r_rect[2], r_rect[3]), new Scalar(0, 0, 255), 1);

            return input;
        }
    }
}
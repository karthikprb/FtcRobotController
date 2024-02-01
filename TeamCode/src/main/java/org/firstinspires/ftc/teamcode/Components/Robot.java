package org.firstinspires.ftc.teamcode.Components;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.Odometry.S4T_Encoder;
import org.firstinspires.ftc.teamcode.Odometry.S4T_Localizer;
import org.firstinspires.ftc.teamcode.Odometry.S4T_Localizer_3;
import org.firstinspires.ftc.teamcode.OpModes.LinearTeleOp;
import org.firstinspires.ftc.teamcode.Vision.PixelDetector;
import org.firstinspires.ftc.teamcode.Vision.SleeveDetectorV2;
import org.firstinspires.ftc.teamcode.Vision.pixels;
import org.firstinspires.ftc.teamcode.Wrapper.GamepadEx;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.List;

public class Robot {

    public Mecanum_Drive drive;

    public boolean robotCentric = false;
    public Intake intake;

    public Airplane plane;

    public V4B_Arm v4b;

    public Slides slides;

    public S4T_Localizer localizer;
    private S4T_Encoder encoderLY;
    private S4T_Encoder encoderLX;
    private S4T_Encoder encoderRY;
    private S4T_Encoder encoderRX;

    private Pose2d OFFSET = new Pose2d(0,0,0);



    OpenCvCamera webcam;
    OpenCvPipeline detector;


    private HardwareMap hardwareMap;

    private Telemetry telemetry;


    public static Pose2d startPos = new Pose2d(0, 0, 0);

    List<LynxModule> allHubs;

    public Robot(HardwareMap map, Telemetry telemetry){
        this.hardwareMap = map;
        this.telemetry = telemetry;
        startPos = new Pose2d(0, 0, 0);


        allHubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule module : allHubs) {
            module.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }

        encoderLY = new S4T_Encoder(map, "bleft");
        encoderLX = new S4T_Encoder(map, "fleft");
        encoderRY = new S4T_Encoder(map, "bright");
        encoderRX = new S4T_Encoder(map, "fright");

        localizer = new S4T_Localizer(telemetry);

        slides = new Slides(map,telemetry);
        intake = new Intake(map,telemetry);
        drive = new Mecanum_Drive(map, telemetry);
        v4b = new V4B_Arm(map,telemetry);
        plane = new Airplane(map, telemetry);

        telemetry.update();
    }

    public void operate(GamepadEx gamepad1ex, GamepadEx gamepad2ex) {
        telemetry.addLine("MAKE SURE TO HIT RIGHT TRIGGER");

        drive.drive(gamepad1ex.gamepad,1,0.7);

        drive.write();


        intake.intake(gamepad1ex,gamepad2ex,telemetry);
        intake.write();


        slides.operate(gamepad1ex,gamepad2ex);
        slides.write();


        v4b.operate(gamepad1ex,gamepad2ex,telemetry);
        v4b.write();

        plane.operate(gamepad2ex,telemetry);
        plane.write();

        gamepad1ex.loop();
        gamepad2ex.loop();

        telemetry.addData("Heading",Math.toDegrees(getPos().getHeading()));
        telemetry.addData("Field Centric:", !robotCentric);


        if(gamepad1ex.isPress(GamepadEx.Control.start)){
            telemetry.addLine("Resetting...");
            localizer.reset();
        }else{
            update();
        }



    }
    public void initializeWebcam(){
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        webcam = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, "Webcam 1"), cameraMonitorViewId);

        detector = new pixels();
        webcam.setPipeline(detector);

        webcam.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener()
        {
            @Override
            public void onOpened()
            {
                FtcDashboard.getInstance().startCameraStream(webcam, 30);
                webcam.startStreaming(640, 360, OpenCvCameraRotation.UPRIGHT);
            }

            @Override
            public void onError(int errorCode) {

            }
        });
    }

    public double getPixelCase(){
        return((pixels)detector).getPixelPosition();
    }



    public void stopWebcam(){
        webcam.stopStreaming();
    }

    public void updatePos(){
        encoderLX.update();
        encoderLY.update();
        encoderRX.update();
        encoderRY.update();
        localizer.update(getRawLeft_X_Dist(),getRawLeft_Y_Dist(), getRawRight_X_Dist(), getRawRight_Y_Dist());
    }

    public double getRawRight_X_Dist(){
        return encoderRX.distance;
    }


    public double getRawLeft_X_Dist(){
        return encoderLX.distance;
    }

    public double getRawLeft_Y_Dist(){
        return -encoderLY.distance;
    }

    public double getRawRight_Y_Dist(){
        return -encoderRY.distance;
    }


    public void resetOdo(){
        encoderLX.reset();
        encoderLY.reset();
        encoderRY.reset();
        encoderRX.reset();
    }
    public void stopAndResetEncoders(){
        encoderLX.stopandreset();
        encoderLY.stopandreset();
        encoderRY.stopandreset();
        encoderRX.stopandreset();

        localizer.reset();
    }

    public void setStartPose(Pose2d startPos){
        this.startPos = startPos;
        localizer.setHeading(startPos.getHeading());
    }

    public Pose2d getPos(){
        return new Pose2d((localizer.getPose().getX() + startPos.getX() - OFFSET.getX()), (localizer.getPose().getY() + startPos.getY() - OFFSET.getY()), localizer.getPose().getHeading());
    }

    public Pose2d getStartPos(){
        return startPos;
    }

    public void setOffset(Pose2d offset){
        this.OFFSET = offset;
    }

    public void GoTo(Pose2d pose, Pose2d speedLimits){
        updateGoTo(pose, speedLimits);
    }

    public void GoTo(double x, double y, double heading, double maxspeed_x, double maxspeed_y, double maxspeed_z){
        updateGoTo(new Pose2d(x, y, heading), new Pose2d(maxspeed_x, maxspeed_y, maxspeed_z));
    }

    public void GoToAlign(double x, double y, double heading, double maxspeed_x, double maxspeed_y, double maxspeed_z){
        updateGoTo(new Pose2d(x, y, heading), new Pose2d(maxspeed_x, maxspeed_y, maxspeed_z));
    }

    public void setAngle(double heading){
        localizer.setHeading(heading);
    }



    private void updateGoTo(Pose2d pose, Pose2d speedLimits){
        drive.goToPoint(pose, getPos(), speedLimits.getX(), speedLimits.getY(), speedLimits.getHeading());
        telemetry.addData("Position: ", getPos());
        telemetry.addData("Target Position: ", pose);
        drive.write();
        updatePos();
    }


    public void update(){
        for (LynxModule module : allHubs) {
            module.clearBulkCache();
        }

        updatePos();
    }







}




package com.alan.face.javacv;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javax.swing.*;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/*
 * 调用摄像头
 */
public class CameraDemo {
    /*
     * 人脸识别
     */
    public void findFace() throws Exception, InterruptedException {
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();//开始获取摄像头数据
        CanvasFrame canvas = new CanvasFrame("摄像头");//新建一个窗口
        canvas.setSize(600, 400);
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);

        FaceDetector detector = new FaceDetector();
        AgeDetector ageDetector = new AgeDetector();
        GenderDetector genderDetector = new GenderDetector();
        while (true) {
            if (!canvas.isDisplayable()) {
                //窗口是否关闭
                grabber.stop();//停止抓取
                System.exit(2);//退出
            }
            final Frame frame = grabber.grab();
            Mat image = converter.convertToMat(frame);
            Map<Rect, Mat> map = detector.detect(image, false);
            map.entrySet().forEach(rectMatEntry -> {
                String age = ageDetector.predictAge(rectMatEntry.getValue());

                rectangle(image, rectMatEntry.getKey(), Scalar.RED, 1, 8, 0);
                int posX = Math.max(rectMatEntry.getKey().x() - 10, 0);
                int posY = Math.max(rectMatEntry.getKey().y() - 10, 0);
                String gender =  genderDetector.predictGender(rectMatEntry.getValue()).name();
                putText(image, String.format("%s:%s",gender,age), new Point(posX, posY), CV_FONT_HERSHEY_PLAIN, 1.0,
                        new Scalar(255, 255, 255, 2.0));
            });
            canvas.showImage(frame);//获取摄像头图像并放到窗口上显示，表示是一帧图像
            Thread.sleep(25);//25帧图像
        }
    }

    public static void main(String[] args) throws Exception, InterruptedException {

//        CameraDemo camera = new CameraDemo();
//		camera.findFace();
        Mat image = imread("F:\\opencv\\yc2.jpg") ;
        AgeDetector ageDetector = new AgeDetector();
        FaceDetector detector = new FaceDetector();
        GenderDetector genderDetector = new GenderDetector();
        Map<Rect, Mat> faces = detector.detect(image);
        faces.entrySet().forEach(rectMatEntry -> {
            imwrite("c:\\"+(Math.random()*1000)+".jpg",rectMatEntry.getValue());
            String age = ageDetector.predictAge(rectMatEntry.getValue());
            System.out.println(age);
           GenderDetector.Gender gender =  genderDetector.predictGender(rectMatEntry.getValue());
            System.out.println(gender.name());
        });
    }

}
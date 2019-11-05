package com.alan.face.javacv;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javax.swing.*;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.IntBuffer;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_face.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;


public class Training {

    //读取opencv人脸检测器，参考我的路径改为自己的路径
    static CascadeClassifier cascade = new CascadeClassifier(
            "F:\\opencv\\xml\\haarcascade_frontalface_alt.xml");
    public static void main(String[] args) throws Exception, InterruptedException {
        //准备两个人的训练图片，每个人脸十张
        //需要注意，训练的图片必须是相同大小的灰度图
        String baseDir = "C:\\";
        //读取图片保存到mat
        Mat y1 = opencv_imgcodecs.imread(baseDir + "face_01.jpg", 0);


        MatVector images = new MatVector(4);//一共20个训练样本
        Mat lables = new Mat(4, 1, CV_32SC1);//对应20个标签值
        //写入标签值，前十个为1，后十个为2
        IntBuffer lablesBuf = lables.createBuffer();
        lablesBuf.put(0, 1);


        //写入图片
        images.put(0, y1);


        //创建人脸分类器，有Fisher、Eigen、LBPH，选哪种自己决定，这里使用FisherFaceRecognizer
        FaceRecognizer fr = createLBPHFaceRecognizer();
        //训练
        fr.train(images, lables);
        //保存训练结果
        fr.save("FisherRecognize.xml");

        //读取训练出的xml文件
        fr.load("FisherRecognize.xml");
        //设置阈值，阈值为0则任何人都不认识，阈值特别大的时候任何人都认识（返回和样本最相似的结果，永远不会返回-1）
        //前面忘记说了，检测返回-1代表不能和训练结果匹配
        ((LBPHFaceRecognizer)fr).setThreshold(3000.0);

        //*********************测试部分************************

        //开启摄像头，获取图像（得到的图像为frame类型，需要转换为mat类型进行检测和识别）
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        grabber.start();
        OpenCVFrameConverter.ToMat convertor = new OpenCVFrameConverter.ToMat();//用于类型转换
        CanvasFrame canvas = new CanvasFrame("人脸检测");//新建一个窗口
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);
        Frame frame = null;
        Mat scr =null;
        while (true) {
            if (!canvas.isDisplayable()) {//窗口是否关闭
                grabber.stop();//停止抓取
                System.exit(0);//退出
            }
            frame = grabber.grab();
            scr = convertor.convertToMat(frame);//将获取的frame转化成mat数据类型
            convert(scr,fr);
            //显示
            frame = convertor.convert(scr);//将检测结果重新的mat重新转化为frame
            canvas.showImage(frame);//获取摄像头图像并放到窗口上显示，frame是一帧视频图像
            Thread.sleep(30);//30毫秒刷新一次图像
        }
    }
    private static void convert(Mat scr,FaceRecognizer fr){
        if (scr.empty())
            return;
        Mat grayscr = new Mat();
        Mat face = new Mat();
        Mat roi = null;
        cvtColor(scr, grayscr, COLOR_BGRA2GRAY);//摄像头是彩色图像，所以先灰度化下
        equalizeHist(grayscr, grayscr);//均衡化直方图


        //检测人脸
        RectVector faces = new RectVector();
        cascade.detectMultiScale(grayscr, faces);
        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);

        //识别人脸
        for (int i = 0; i < faces.size(); i++) {
            Rect face_i = faces.get(i);
            rectangle(scr, face_i, new Scalar(0, 255, 0, 1));

            roi = new Mat(grayscr, face_i);
            resize(roi, face, new Size(300, 300));//我的训练样本是350*350，要对应的进行修改
            fr.predict(face, label, confidence);
            int predictedLabel = label.get(0);//得到识别的标签值

            //判断并显示
            if (predictedLabel == 1) {
                String box_text = "WB";
                int pos_x = Math.max(face_i.tl().x() - 10, 0);
                int pos_y = Math.max(face_i.tl().y() - 10, 0);
                putText(scr, box_text, new Point(pos_x, pos_y),
                        FONT_HERSHEY_PLAIN, 1.0, new Scalar(0, 255, 0, 2.0));
            } else if (predictedLabel == 2) {
                int pos_x = Math.max(face_i.tl().x() - 10, 0);
                int pos_y = Math.max(face_i.tl().y() - 10, 0);
                // And now put it into the image:
                putText(scr, "TX", new Point(pos_x, pos_y),
                        FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 255, 0, 2.0));
            } else {
                int pos_x = Math.max(face_i.tl().x() - 10, 0);
                int pos_y = Math.max(face_i.tl().y() - 10, 0);
                // And now put it into the image:
                putText(scr, "UnknownPeople!", new Point(pos_x, pos_y),
                        FONT_HERSHEY_PLAIN, 1.0, new Scalar(0, 0, 255, 2.0));
            }
        }
    }
}
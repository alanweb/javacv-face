package com.alan.face.javacv;

import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGRA2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.equalizeHist;

/**
 * 人脸识别
 */
public class FaceDetector {
    private static final Logger logger = LoggerFactory.getLogger(FaceDetector.class);


    /**
     * 人脸探测器
     */
    private CascadeClassifier faceCascade;
    /**
     * 眼睛检测器
     */
    private CascadeClassifier eyeCascade;

    public FaceDetector() {
        try {
            //初始化人脸检测器
            faceCascade = new CascadeClassifier("F:\\opencv\\xml\\lbpcascade_frontalface_improved.xml");
            //初始化眼睛检测器
            eyeCascade = new CascadeClassifier("F:\\opencv\\xml\\haarcascade_eye.xml");
        } catch (Exception e) {
            logger.error("构建检测器失败!", e);
            throw new IllegalStateException("构建检测器失败!", e);
        }
    }

    /**
     * 从给定的图中检测并返回裁剪后的人脸图 校验眼睛
     *
     * @param image 图片
     * @return 人脸以及在画面中对应的坐标
     */
    public Map<Rect, Mat> detect(Mat image) {
        return detect(image, true);
    }

    /**
     * 从给定的图中检测并返回裁剪后的人脸图
     *
     * @param image 图片
     * @param flag  是否需要校验眼睛
     * @return 人脸以及在画面中对应的坐标
     */
    public Map<Rect, Mat> detect(Mat image, boolean flag) {
        Map<Rect, Mat> detectedFaces = new HashMap<>();
        //人脸坐标信息集合
        RectVector faces = new RectVector();
        //眼睛坐标信息集合
        RectVector eyes = new RectVector();
        //创建识别副本（防止污染底片）
        Mat grayImage = new Mat();
        //灰度处理
        cvtColor(image, grayImage, COLOR_BGRA2GRAY);
        //直方均衡
        equalizeHist(grayImage, grayImage);
        //人脸识别
        faceCascade.detectMultiScale(grayImage, faces);
        //临时人脸
        Mat croppedMat = null;
        //人脸数量
        long faceNum = faces.size();
        //迭代所有人脸 由于识别可能存在误差 需要对识别出来的人脸进行眼睛识别 才能确认是人脸 flag为false 忽略眼睛校验
        for (int i = 0; i < faceNum; i++) {
            //人脸坐标信息
            Rect face = faces.get(i);
            //抠出灰色人脸图案
            croppedMat = grayImage.apply(new Rect(face.x(), face.y(), face.width(), face.height()));
            //识别眼睛
            faceCascade.detectMultiScale(croppedMat, eyes);
            //一对眼睛说明是人脸了
            if (flag && eyes.size() == 1 || !flag) {
                //人脸图案
                croppedMat = image.apply(new Rect(face.x(), face.y(), face.width(), face.height()));
                //集合存放
                detectedFaces.put(face, croppedMat);
            }
        }
        return detectedFaces;
    }
}

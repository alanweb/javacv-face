package com.alan.face.javacv;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_dnn.Importer;
import org.bytedeco.javacpp.opencv_dnn.Net;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;

import static org.bytedeco.javacpp.opencv_core.NORM_MINMAX;
import static org.bytedeco.javacpp.opencv_core.minMaxLoc;
import static org.bytedeco.javacpp.opencv_core.normalize;
import static org.bytedeco.javacpp.opencv_dnn.*;
import static org.bytedeco.javacpp.opencv_dnn.createCaffeImporter;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 *使用卷积神经网络的年龄预测器
 */
public class AgeDetector {
    private static final Logger logger = LoggerFactory.getLogger(AgeDetector.class);
    private static final String[] AGES = new String[]{"0-2", "4-6", "8-13", "15-20", "25-32", "38-43", "48-53", "60-"};

    private  Net ageNet;

    public AgeDetector() {
        try {
            ageNet = new Net();
            File protobuf = new File(getClass().getResource("/caffe/deploy_agenet.prototxt").toURI());
            File caffeModel = new File(getClass().getResource("/caffe/age_net.caffemodel").toURI());
            Importer importer = createCaffeImporter(protobuf.getAbsolutePath(), caffeModel.getAbsolutePath());
            importer.populateNet(ageNet);
            importer.close();
        } catch (URISyntaxException e) {
            logger.error("无法加载caffe模型", e);
            throw new IllegalStateException("无法加载caffe模型", e);
        }
    }
    public String predictAge(Mat face) {
        try {
            //副本
            Mat resizedMat = new Mat();
            //复制原图替换副本
            resize(face, resizedMat, new Size(256,256));
            //对副本进行归一处理 置灰
            normalize(resizedMat, resizedMat, 0, Math.pow(2,  face.arrayDepth()), NORM_MINMAX, -1, null);
            //封装识别图像数据
            Blob inputBlob = new Blob(resizedMat);
            //设置入口参数
            ageNet.setBlob(".data", inputBlob);
            //探测
            ageNet.forward();
            //获取输出数据
            Blob prob = ageNet.getBlob("prob");

            DoublePointer pointer = new DoublePointer(new double[1]);
            Point max = new Point();
            //
            minMaxLoc(prob.matRefConst(), null, pointer, null, max, null);
            return AGES[max.x()];
        } catch (Exception e) {
            logger.error("探测年龄异常", e);
        }
        return null;
    }
}

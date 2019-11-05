package com.alan.face.javacv;

import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_dnn.Blob;
import org.bytedeco.javacpp.opencv_dnn.Importer;
import org.bytedeco.javacpp.opencv_dnn.Net;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.bytedeco.javacpp.opencv_core.NORM_MINMAX;
import static org.bytedeco.javacpp.opencv_core.normalize;
import static org.bytedeco.javacpp.opencv_dnn.createCaffeImporter;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 *
 */
public class GenderDetector {

    private static final Logger logger = LoggerFactory.getLogger(GenderDetector.class);

    private  Net genderNet;

    public GenderDetector() {
        try {
            genderNet = new Net();
            File protobuf = new File(getClass().getResource("/caffe/deploy_gendernet.prototxt").toURI());
            File caffeModel = new File(getClass().getResource("/caffe/gender_net.caffemodel").toURI());
            Importer importer = createCaffeImporter(protobuf.getAbsolutePath(), caffeModel.getAbsolutePath());
            importer.populateNet(genderNet);
            importer.close();
        } catch (Exception e) {
            logger.error("无法加载caffe模型", e);
            throw new IllegalStateException("无法加载caffe模型", e);
        }
    }
    public Gender predictGender(Mat face) {
        try {
            Mat croppedMat = new Mat();
            resize(face, croppedMat, new Size(256, 256));
            normalize(croppedMat, croppedMat, 0, Math.pow(2, face.arrayDepth()), NORM_MINMAX, -1, null);

            Blob inputBlob = new Blob(croppedMat);
            genderNet.setBlob(".data", inputBlob);
            genderNet.forward();
            Blob prob = genderNet.getBlob("prob");
            Indexer indexer = prob.matRefConst().createIndexer();
            logger.debug("识别结果 results  男：{}, 女：{}", indexer.getDouble(0, 0), indexer.getDouble(0, 1));
            if (indexer.getDouble(0, 0) > indexer.getDouble(0, 1)) {
                return Gender.MALE;
            } else {
                return Gender.FEMALE;
            }
        } catch (Exception e) {
            logger.error("性别识别异常", e);
        }
        return Gender.NOT_RECOGNIZED;
    }

    public enum Gender {
        MALE,
        FEMALE,
        NOT_RECOGNIZED
    }
}

package com.alan.face.javacv;

import com.alan.util.JdbcUtil;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_face.*;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.*;

import javax.swing.*;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.IntBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgproc.putText;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 * 人脸库
 */
public class FaceDB {

    //人脸分类器
    private static LBPHFaceRecognizer fr = createLBPHFaceRecognizer();
    //读取opencv人脸检测器
    static CascadeClassifier cascade;

    static {
        try {
            cascade = new CascadeClassifier(new File(Training.class.getResource(
                    "/detection/haarcascade_frontalface_alt.xml").toURI()).getAbsolutePath());
            fr.setThreshold(3000.0);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    static AgeDetector ageDetector = new AgeDetector();
    private static void convert(Mat scr) {
        if (scr.empty())
            return;
        opencv_core.Mat grayscr = new opencv_core.Mat();
        opencv_core.Mat face = new opencv_core.Mat();
        opencv_core.Mat roi = null;
        cvtColor(scr, grayscr, COLOR_BGRA2GRAY);//摄像头是彩色图像，所以先灰度化下
        equalizeHist(grayscr, grayscr);//均衡化直方图
        //检测人脸
        opencv_core.RectVector faces = new opencv_core.RectVector();
        cascade.detectMultiScale(grayscr, faces);
        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);

        //识别人脸
        for (int i = 0; i < faces.size(); i++) {
            opencv_core.Rect face_i = faces.get(i);
            rectangle(scr, face_i, new opencv_core.Scalar(0, 255, 0, 1));

            roi = new opencv_core.Mat(grayscr, face_i);
            //我的训练样本是350*350，要对应的进行修改
            resize(roi, face, new opencv_core.Size(300, 300));
            String age = ageDetector.predictAge(face);
            System.out.println(age);
            fr.predict(face, label, confidence);
            int predictedLabel = label.get(0);//得到识别的标签值

            //判断并显示
            if (predictedLabel == 1) {
                String box_text = "WB";
                int pos_x = Math.max(face_i.tl().x() - 10, 0);
                int pos_y = Math.max(face_i.tl().y() - 10, 0);
                putText(scr, box_text, new opencv_core.Point(pos_x, pos_y),
                        FONT_HERSHEY_PLAIN, 1.0, new opencv_core.Scalar(0, 255, 0, 2.0));
            } else if (predictedLabel == 2) {
                int pos_x = Math.max(face_i.tl().x() - 10, 0);
                int pos_y = Math.max(face_i.tl().y() - 10, 0);
                // And now put it into the image:
                putText(scr, "TX", new opencv_core.Point(pos_x, pos_y),
                        FONT_HERSHEY_PLAIN, 1.0, new opencv_core.Scalar(255, 255, 0, 2.0));
            } else {
                int pos_x = Math.max(face_i.tl().x() - 10, 0);
                int pos_y = Math.max(face_i.tl().y() - 10, 0);
                // And now put it into the image:
                putText(scr, "UnknownPeople!", new opencv_core.Point(pos_x, pos_y),
                        FONT_HERSHEY_PLAIN, 1.0, new opencv_core.Scalar(0, 0, 255, 2.0));
            }
        }
    }

    private static String queryNameByUserId(Long userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            //创建连接
            conn = JdbcUtil.getConnection();
            //创建prepareStatement对象，用于执行SQL
            ps = conn.prepareStatement("select name from t_user where user_id = ?");
            ps.setLong(1, userId);
            //获取查询结果集
            result = ps.executeQuery();
            while (result.next()) {
                return result.getString(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JdbcUtil.close(result, ps, conn);
        }
        return null;
    }

    private static List<String> queryPicsByUserId(Long userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        List<String> list = new ArrayList<>();
        try {
            //创建连接
            conn = JdbcUtil.getConnection();
            //创建prepareStatement对象，用于执行SQL
            ps = conn.prepareStatement("select pic_url from t_user_pic where user_id = ?");
            ps.setLong(1, userId);
            //获取查询结果集
            result = ps.executeQuery();
            while (result.next()) {
                list.add(result.getString(1));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JdbcUtil.close(result, ps, conn);
        }
        return null;
    }

    private static Map<Long, List<String>> queryAllPic() {
        Map<Long, List<String>> map = new HashMap<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            //创建连接
            conn = JdbcUtil.getConnection();
            //创建prepareStatement对象，用于执行SQL
            ps = conn.prepareStatement("select user_id,pic_url from t_user_pic");
            //获取查询结果集
            result = ps.executeQuery();
            Long userId = null;
            String pic = null;
            List<String> pics = null;
            while (result.next()) {
                userId = result.getLong(1);
                pic = result.getString(2);
                if (map.containsKey(userId)) {
                    map.get(userId).add(pic);
                } else {
                    pics = new ArrayList<>();
                    pics.add(pic);
                    map.put(userId, pics);
                }
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JdbcUtil.close(result, ps, conn);
        }
        return null;
    }

    private static void camera() throws Exception {
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
        Mat scr = null;
        while (true) {
            if (!canvas.isDisplayable()) {//窗口是否关闭
                grabber.stop();//停止抓取
                System.exit(0);//退出
            }
            frame = grabber.grab();
            scr = convertor.convertToMat(frame);//将获取的frame转化成mat数据类型
            convert(scr);
            //显示
            frame = convertor.convert(scr);//将检测结果重新的mat重新转化为frame
            canvas.showImage(frame);//获取摄像头图像并放到窗口上显示，frame是一帧视频图像
            Thread.sleep(30);//30毫秒刷新一次图像
        }
    }

    public static void main(String[] args) throws Exception {
        Map<Long, List<String>> map = queryAllPic();
        MatVector images = new MatVector(4);//一共训练size个样本
        Mat lables = new Mat(4, 1, CV_32SC1);//对应size个标签值
        IntBuffer lablesBuf = lables.createBuffer();
        final int[] index = {0};
        //写入标签值，前十个为1，后十个为2
        map.entrySet().forEach(e -> {
            for (String pic : e.getValue()) {
                lablesBuf.put(index[0], e.getKey().intValue());
                images.put(index[0], imread(pic, 0));
                index[0]++;
            }
        });
        fr.train(images, lables);
        camera();
//        Mat mat = imread("C:\\Users\\User\\Desktop\\opencv\\1.jpeg");
//        convert(mat);
//        imwrite("c:\\111111.jpg",mat);
    }
}

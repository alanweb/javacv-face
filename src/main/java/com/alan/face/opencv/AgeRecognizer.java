package com.alan.face.opencv;

import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_TRIPLEX;
import static org.opencv.imgproc.Imgproc.cvtColor;


public class AgeRecognizer extends JPanel {
    private static Logger log = LoggerFactory.getLogger(AgeRecognizer.class);
    private static final String TAG = "AgeRecognizer";
    private static Net mAgeNet;
    private static CascadeClassifier mFaceDetector;
    private static final String[] AGES = new String[]{"0-2", "4-6", "8-13", "15-20", "25-32", "38-43", "48-53", "60+"};
    private BufferedImage mImg;
    public static void main(String[] args) throws Exception{
        //加载opencv库
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        File proto = new File(AgeRecognizer.class.getResource("/assets/deploy_age.prototxt").toURI());
        File weights = new File(AgeRecognizer.class.getResource("/assets/age_net.caffemodel").toURI());
        mAgeNet = Dnn.readNetFromCaffe(proto.getAbsolutePath(), weights.getAbsolutePath());
        if (mAgeNet.empty()) {
            log.info(TAG, "Network loading failed");
        } else {
            log.info(TAG, "Network loading success");
        }
        mFaceDetector = new CascadeClassifier(new File(AgeRecognizer.class.getResource("/detection/lbpcascade_frontalface_improved.xml").toURI()).getAbsolutePath());
       openCamera();
    }

    private static String predictAge(Mat mRgba, Rect[] facesArray) {
        try {
            for (Rect face : facesArray) {
                Mat capturedFace = new Mat(mRgba, face);
                //Resizing pictures to resolution of Caffe model
                Imgproc.resize(capturedFace, capturedFace, new Size(227, 227));
                //Converting RGBA to BGR
                cvtColor(capturedFace, capturedFace, Imgproc.COLOR_RGBA2BGR);

                //Forwarding picture through Dnn
                Mat inputBlob = Dnn.blobFromImage(capturedFace, 1.0f, new Size(227, 227),
                        new Scalar(78.4263377603, 87.7689143744, 114.895847746), false, false);
                mAgeNet.setInput(inputBlob, "data");
                Mat probs = mAgeNet.forward("prob").reshape(1, 1);
                Core.MinMaxLocResult mm = Core.minMaxLoc(probs); //Getting largest softmax output

                double result = mm.maxLoc.x; //Result of age recognition prediction
                log.info(TAG, "Result is: " + result);
                return AGES[(int) result];
            }
        } catch (Exception e) {
            log.error(TAG, "Error processing age", e);
        }
        return null;
    }

    public static void openCamera() throws  Exception {
        try{
            //加载opencv库
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

            //获取摄像头视频流
            VideoCapture capture = new VideoCapture(0);
            int height = (int)capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
            int width = (int)capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
            if(height == 0||width == 0){
                throw new Exception("camera not found!");
            }

            //使用Swing生成GUI
            JFrame frame = new JFrame("camera");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            AgeRecognizer panel = new AgeRecognizer();
            frame.setContentPane(panel);
            frame.setVisible(true);
            frame.setSize(width+frame.getInsets().left+frame.getInsets().right,
                    height+frame.getInsets().top+frame.getInsets().bottom);

            Mat capImg = new Mat();
            //Random r = new Random();

            while(frame.isShowing()){
                //获取视频帧
                capture.read(capImg);
                //识别人脸
                Mat image = convert(capImg);
                //转为图像显示
                panel.mImg = panel.mat2BI(image);
                panel.repaint();
            }
            capture.release();
            frame.dispose();
        }catch(Exception e){
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(sw.toString());
        }
        finally{
            System.out.println("Exit");
        }
    }
    @Override
    public void paint(Graphics g){
        if(mImg!=null){
            g.drawImage(mImg, 0, 0, mImg.getWidth(),mImg.getHeight(),this);
        }
    }

    private static Mat convert(Mat mRgba){
        Mat mGray = new Mat();
        cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY);
        MatOfRect faces = new MatOfRect();

        //人脸检测 获取人脸
        if (mFaceDetector != null) {
            mFaceDetector.detectMultiScale(mGray, faces);
        } else {
            log.error(TAG, "Detection is not selected!");
        }

        //Drawing rectangle around detected face
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
        }

        //If one face is detected, method predictAge is executed
        if (facesArray.length == 1) {
            String age = predictAge(mRgba, facesArray);
            log.info(TAG, "Age is: " + age);

            //The result of age recognition
            for (Rect face : facesArray) {
                int posX = (int) Math.max(face.tl().x - 10, 0);
                int posY = (int) Math.max(face.tl().y - 10, 0);

                Imgproc.putText(mRgba, "Age: " + age, new Point(posX, posY), FONT_HERSHEY_TRIPLEX,
                        1.5, new Scalar(0, 255, 0, 255));
            }
        }
        return mRgba;
    }
    /**
     * 转换图像
     * @param mat
     * @return
     */
    private BufferedImage mat2BI(Mat mat){
        int dataSize = mat.cols()*mat.rows()*(int)mat.elemSize();
        byte[] data = new byte[dataSize];
        mat.get(0, 0,data);

        int type = mat.channels()==1? BufferedImage.TYPE_BYTE_GRAY:BufferedImage.TYPE_3BYTE_BGR;
        if(type == BufferedImage.TYPE_3BYTE_BGR){
            for(int i=0;i<dataSize;i+=3){
                byte blue=data[i+0];
                data[i+0]=data[i+2];
                data[i+2]=blue;
            }
        }
        BufferedImage image=new BufferedImage(mat.cols(),mat.rows(),type);
        image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
        return image;
    }
}

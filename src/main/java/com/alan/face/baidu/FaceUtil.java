package com.alan.face.baidu;

import com.alan.util.Base64Util;
import com.alan.util.FileUtil;
import com.alan.util.HttpUtils;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author wayne wei
 * @date 2019年10月28日13点45分
 * @description 百度人脸识别工具类
 */
public class FaceUtil {
    private static Logger log = LoggerFactory.getLogger(FaceUtil.class);
    private static AtomicReference<String> token = new AtomicReference<>();
    private static String appKey = "LcRmFmtnGC5jTHRvsCYWcN9H";
    private static String secretKey = "xP4NZef5yPZM5GE5I1kdW2pZLWmmvQLx";

    /**
     * 获取百度api的token
     * @return
     */
    public static String getToken() {
        String result = token.get();
        if (StringUtils.isNotBlank(result)) {
            return result;
        }
        String param = "grant_type=client_credentials&client_id=%s&client_secret=%s";
        param = String.format(param, appKey, secretKey);
        result = HttpUtils.sendGet("https://aip.baidubce.com/oauth/2.0/token", param);
        Map map = (Map) JSON.parse(result);
        try {
            result = map.get("access_token").toString();
            token.set(result);
        } catch (Exception e) {
            log.error("获取百度token失败！", e.getMessage(), result);
            e.printStackTrace(System.err);
        }
        return result;
    }

    /**
     * 检查人脸质量
     * @return
     */
    public static String detect() {
        // 请求url
        String url = String.format("https://aip.baidubce.com/rest/2.0/face/v3/detect?access_token=%s", getToken());
        try {
            Map<String, Object> map = new HashMap<>();

            byte[] bytes = FileUtil.readFileByBytes("C:\\Users\\User\\Desktop\\opencv\\2.jpeg");
            String encode = Base64Util.encode(bytes);
            map.put("image", encode);
            map.put("image_type", "BASE64");
//            map.put("image","a7f89a249fd88bde937b2f2b6ec11c1d");
//            map.put("image_type", "FACE_TOKEN");
            map.put("face_field", "quality");
            map.put("face_type", "LIVE");
            String result = HttpUtils.sendPost(url, map);
            return result;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
    public static String match() {
        // 请求url
        String url = String.format("https://aip.baidubce.com/rest/2.0/face/v3/match?access_token=%s", getToken());
        try {
            byte[] bytes1 =  FileUtil.readFileByBytes("C:\\Users\\User\\Downloads\\1.jpeg");
            String image1 = Base64Util.encode(bytes1);
            String image2 = "a7f89a249fd88bde937b2f2b6ec11c1d";
            List<Map<String, Object>> images = new ArrayList<>();
            Map<String, Object> map1 = new HashMap<>();
            map1.put("image", image1);
            map1.put("image_type", "BASE64");
            map1.put("face_type", "LIVE");
            map1.put("quality_control", "NORMAL");
            Map<String, Object> map2 = new HashMap<>();
            map2.put("image", image2);
            map2.put("image_type", "FACE_TOKEN");
            map2.put("face_type", "LIVE");
            map2.put("quality_control", "NORMAL");
            images.add(map1);
            images.add(map2);
            String result = HttpUtils.sendPost(url,images);
            System.out.println(result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
//        System.out.println(FaceUtil.getToken());
        System.out.println(FaceUtil.detect());
//        FaceUtil.match();
    }
}

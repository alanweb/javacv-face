package com.alan.util;

import com.alibaba.fastjson.JSON;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class HttpUtils {
    public static final String CHARSET = "UTF-8";

    // 发送get请求 url?a=x&b=xx形式
    public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlName = "";
            if (param.length() != 0) {
                urlName = url + "?" + param;
            } else
                urlName = url;
            URL resUrl = new URL(urlName);
            URLConnection urlConnec = resUrl.openConnection();
            urlConnec.setRequestProperty("accept", "*/*");
            urlConnec.setRequestProperty("connection", "Keep-Alive");
            urlConnec.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            urlConnec.connect();
//            Map<String, List<String>> map = urlConnec.getHeaderFields();
//            for (String key : map.keySet()) {
//                System.out.println(key + "--->" + map.get(key));
//            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    urlConnec.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送get请求失败" + e);
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    // 发送post请求
    public static String sendPost(String url, MultipartHttpServletRequest param) {
        String result = "";
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            URL resUrl = new URL(url);
            URLConnection urlConnec = resUrl.openConnection();
            urlConnec.setRequestProperty("accept", "*/*");
            urlConnec.setRequestProperty("connection", "Keep-Alive");
            urlConnec.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            urlConnec.setDoInput(true);
            urlConnec.setDoOutput(true);

            out = new PrintWriter(urlConnec.getOutputStream());
            out.print(param);// 发送post参数
            out.flush();
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    urlConnec.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("post请求发送失败" + e);
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    // post请求方法
    public static String sendPost(String url, Object param) {
        final String CONTENT_TYPE_TEXT_JSON = "text/json";
        String response = null;
        try {
            CloseableHttpClient httpclient = null;
            CloseableHttpResponse httpresponse = null;
            try {
                httpclient = HttpClients.createDefault();
                HttpPost httppost = new HttpPost(url);
                if (param!=null) {
                    StringEntity se = new StringEntity(
                            JSON.toJSONString(param));
                    se.setContentType(CONTENT_TYPE_TEXT_JSON);
                    httppost.setEntity(se);
                }
                Header[] arr = new BasicHeader[2];
                arr[0] = new BasicHeader("Content-Type",
                        "application/json;charset=UTF-8");
                arr[1] = new BasicHeader("accept", "*/*");
                httppost.setHeaders(arr);
                httpresponse = httpclient.execute(httppost);
                response = EntityUtils.toString(httpresponse.getEntity());
            } finally {
                if (httpclient != null) {
                    httpclient.close();
                }
                if (httpresponse != null) {
                    httpresponse.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 测试 说明：这里用新浪股票接口做get测试,新浪股票接口不支持jsonp,至于post,因为本人用的公司的接口就不展示了,一样的,一个url,
     * 一个数据包
     */
    public static void main(String[] args) {
        String appId="wxe4ee869554ed9cf3";
        String secret="87bdc864c4efc148001e13e115d00c72";
        String jsCode="043jsSri0KuN4q1PHWpi0Ybtri0jsSry";
        String resultGet = sendGet("https://api.weixin.qq.com/sns/jscode2session", String.format("appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",appId,secret,jsCode));
        System.out.println(resultGet);
        Map map = (Map) JSON.parse(resultGet);
        System.out.println(map.get("errcode"));
        System.out.println(String.format("a%sc","xxx"));
    }

}
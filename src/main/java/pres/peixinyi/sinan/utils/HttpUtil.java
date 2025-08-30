package pres.peixinyi.sinan.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import org.springframework.stereotype.Component;
import pres.peixinyi.sinan.common.Result;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author : PeiXinyi
 * @Date : 2025/5/27 09:25
 * @Version : 0.0.0
 */
@Slf4j
@Component
public class HttpUtil {


    private static volatile OkHttpClient client;
    private String url;
    private Map<String, String> headers;
    private Map<String, String> params;
    private RequestBody requestBody;
    private static final MediaType JSONType = MediaType.parse("application/json; charset=utf-8");

    // 双重校验锁实现单例
    public static OkHttpClient getClient() {
        if (client == null) {
            synchronized (HttpUtil.class) {
                if (client == null) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .sslSocketFactory(createSSLSocketFactory(), createTrustAllManager())
                            .hostnameVerifier((hostname, session) -> true).build();
                }
            }
        }
        return client;
    }

    // 构建工具类实例
    public static HttpUtil builder() {
        return new HttpUtil();
    }

    public static <T> Result<T> parseJson(String result) {
        if (result != null && !result.isEmpty()) {
            return JSON.parseObject(result, new TypeReference<Result<T>>() {
            });
        }
        return Result.error("Response is empty or null");
    }

    public HttpUtil url(String url) {
        this.url = url;
        return this;
    }

    public HttpUtil url(String server, String url) {
        if (server == null || server.trim().isEmpty()) {
            throw new IllegalArgumentException("HttpUtil: server 不能为空");
        }
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("HttpUtil: url 不能为空");
        }
        this.url = server + url;
        return this;
    }

    public HttpUtil headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public HttpUtil header(String key, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(key, value);
        return this;
    }

    public HttpUtil params(Map<String, String> params) {
        this.params = params;
        return this;
    }

    public HttpUtil params(String key, String value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }
        this.params.put(key, value);
        return this;
    }

    public HttpUtil jsonBody(String json) {
        this.requestBody = RequestBody.create(json, JSONType);
        return this;
    }

    public HttpUtil jsonBody(Object jsonObject) {
        if (jsonObject instanceof String) {
            this.requestBody = RequestBody.create((String) jsonObject, JSONType);
        } else {
            this.requestBody = RequestBody.create(JSON.toJSONString(jsonObject), JSONType);
        }
        return this;
    }

    // 同步GET请求
    public String get() throws IOException {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("HttpUtil: url 不能为空");
        }
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new IllegalArgumentException("HttpUtil: url 非法，无法解析: " + url);
        }
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        if (params != null) {
            params.forEach(urlBuilder::addQueryParameter);
        }
        Request.Builder builder = new Request.Builder().url(urlBuilder.build());
        addHeaders(builder);
        String result = execute(builder.build());
        log.debug("HttpUtil GET request to {} with params {} returned: {}", url, params, result);
        return result;
    }


    public <T> T get(TypeReference<T> typeReference) {
        String result = null;
        try {
            log.debug("HttpUtil GET request to {} with params {}", url, params);
            result = this.get();
        } catch (IOException e) {
            throw new RuntimeException("Error during GET request: " + e.getMessage());
        }
        if (result == null || result.isEmpty()) {
            return null;
        }

        return JSON.parseObject(result, typeReference);
    }

    // 异步POST请求
    public void postAsync(Callback callback) {
        Request.Builder builder = new Request.Builder().url(url);
        if (requestBody == null) {
            requestBody = new FormBody.Builder(StandardCharsets.UTF_8).addEncoded("", paramsToFormData()).build();
        }
        builder.post(requestBody);
        addHeaders(builder);
        getClient().newCall(builder.build()).enqueue(callback);
    }

    private String execute(Request request) throws IOException {
        try (Response response = getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body().string();
        }
    }

    private void addHeaders(Request.Builder builder) {
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
    }

    private String paramsToFormData() {
        FormBody.Builder formBuilder = new FormBody.Builder(StandardCharsets.UTF_8);
        params.forEach(formBuilder::addEncoded);
        return bufferToString(formBuilder.build());
    }

    // HTTPS相关配置（测试环境使用）
    private static SSLSocketFactory createSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{createTrustAllManager()}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static X509TrustManager createTrustAllManager() {
        return new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    private static String bufferToString(RequestBody body) {
        try (Buffer buffer = new Buffer()) {
            body.writeTo(buffer);
            return buffer.readString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    public String post() throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        if (requestBody == null) {
            requestBody = RequestBody.create("", JSONType);
        }
        builder.post(requestBody);
        addHeaders(builder);
        String result = execute(builder.build());
        log.debug("HttpUtil POST request to {} returned: {}", url, result);
        return result;
    }


    public String put() throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        if (requestBody == null) {
            requestBody = RequestBody.create("", JSONType);
        }
        builder.put(requestBody);
        addHeaders(builder);
        String result = execute(builder.build());
        log.debug("HttpUtil PUT request to {} returned: {}", url, result);
        return result;
    }


    public String delete() throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        if (requestBody == null) {
            requestBody = RequestBody.create("", JSONType);
        }
        builder.delete(requestBody);
        addHeaders(builder);
        String result = execute(builder.build());
        log.debug("HttpUtil DELETE request to {} returned: {}", url, result);
        return result;
    }


    public String patch() throws IOException {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("HttpUtil: url 不能为空");
        }
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new IllegalArgumentException("HttpUtil: url 非法，无法解析: " + url);
        }
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        if (params != null) {
            params.forEach(urlBuilder::addQueryParameter);
        }
        Request.Builder builder = new Request.Builder().url(urlBuilder.build());
        if (requestBody == null) {
            requestBody = RequestBody.create("", JSONType);
        }
        builder.patch(requestBody);
        addHeaders(builder);
        String result = execute(builder.build());
        log.debug("HttpUtil PATCH request to {} returned: {}", url, result);
        return result;
    }

    // 支持 TypeReference 泛型的 getWithType
    public <T> T getWithType(TypeReference<T> typeReference) {
        String jsonStr;
        try {
            jsonStr = get();
        } catch (IOException e) {
            log.error("Error during GET request: {}", e.getMessage(), e);
            throw new RuntimeException("Error during GET request:" + e.getMessage());
        }
        if (jsonStr == null || jsonStr.isEmpty()) {
            return null;
        }
        return JSON.parseObject(jsonStr, typeReference);
    }

    // 支持传入类型参数的 postWithType
    public <T> T postWithType(TypeReference<T> typeReference) {
        String jsonStr = null;
        try {
            jsonStr = post();
            log.debug("HttpUtil POST request to {} returned: {}", url, jsonStr);
        } catch (IOException e) {
            log.error("Error during POST request: {}", e.getMessage(), e);
            throw new RuntimeException("Error during POST request: " + e.getMessage());
        }
        if (jsonStr == null || jsonStr.isEmpty()) {
            return null;
        }
        return JSON.parseObject(jsonStr, typeReference);
    }

    // 支持传入类型参数的 putWithType
    public <T> T putWithType(TypeReference<T> typeReference) {
        String jsonStr = null;
        try {
            jsonStr = put();
        } catch (IOException e) {
            log.error("Error during PUT request: {}", e.getMessage(), e);
            throw new RuntimeException("Error during PUT request: " + e.getMessage());
        }
        if (jsonStr == null || jsonStr.isEmpty()) {
            return null;
        }
        Result<?> result = JSON.parseObject(jsonStr, new TypeReference<Result<?>>() {
        });
        if (result.isSuccess()) {
            return JSON.parseObject(JSON.toJSONString(result.getData()), typeReference);
        }
        throw new RuntimeException("Error fetching data: " + result.getMessage());
    }

    // 支持传入类型参数的 deleteWithType
    public <T> T deleteWithType(TypeReference<T> typeReference) {
        String jsonStr = null;
        try {
            jsonStr = delete();
        } catch (IOException e) {
            log.error("Error during DELETE request: {}", e.getMessage(), e);
            throw new RuntimeException("Error during DELETE request: " + e.getMessage());
        }
        Result<?> result = JSON.parseObject(jsonStr, new TypeReference<Result<?>>() {
        });
        if (result.isSuccess()) {
            return JSON.parseObject(JSON.toJSONString(result.getData()), typeReference);
        }
        throw new RuntimeException("Error fetching data: " + result.getMessage());
    }

    // 支持传入类型参数的 patchWithType
    public <T> T patchWithType(TypeReference<T> typeReference) {
        String jsonStr = null;
        try {
            jsonStr = patch();
        } catch (IOException e) {
            log.error("Error during PATCH request: {}", e.getMessage(), e);
            throw new RuntimeException("Error during PATCH request: " + e.getMessage());
        }
        if (jsonStr == null || jsonStr.isEmpty()) {
            return null;
        }
        Result<?> result = JSON.parseObject(jsonStr, new TypeReference<Result<?>>() {
        });
        if (result.isSuccess()) {
            return JSON.parseObject(JSON.toJSONString(result.getData()), typeReference);
        }
        throw new RuntimeException("Error fetching data: " + result.getMessage());
    }

}

/**
 * // 同步GET请求
 * String result = HttpUtil.builder()
 * .url("https://api.example.com/data")
 * .headers(Map.of("Authorization", "Bearer token"))
 * .params(Map.of("page", "1", "size", "20"))
 * .get();
 * <p>
 * // 异步POST请求
 * HttpUtil.builder()
 * .url("https://api.example.com/create")
 * .jsonBody("{\"name\":\"test\"}")
 * .postAsync(new Callback() {
 *
 * @Override public void onResponse(Call call, Response response) {
 * // 处理响应
 * }
 * @Override public void onFailure(Call call, IOException e) {
 * // 处理异常
 * }
 * });
 */

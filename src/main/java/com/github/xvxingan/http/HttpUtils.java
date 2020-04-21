package com.github.xvxingan.http;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author xuxingan
 * 基于http连接池 提供http服务
 */

public class HttpUtils {
    private static HttpUtils instance = null;
    private static Properties properties = null;
    //设置最大连接数
    private final Integer maxTotal = Integer.valueOf(getConfigValue("maxTotal", "200"));
    //设置每个主机的并发数
    private final Integer defaultMaxPerRoute = Integer.valueOf(getConfigValue("defaultMaxPerRoute", "20"));
    //设置超时时间 指的是连接目标url的连接超时时间，即客服端发送请求到与目标url建立起连接的最大时间。如果在该时间范围内还没有建立起连接，则就抛出connectionTimeOut异常。
    private final Integer connectTimeout = Integer.valueOf(getConfigValue("connectTimeout", "1000"));
    //从连接池中获取到连接的最长时间  HttpClient中的要用连接时尝试从连接池中获取，若是在等待了一定的时间后还没有获取到可用连接（比如连接池中没有空闲连接了）则会抛出获取连接超时异常。
    private final Integer connectionRequestTimeout = Integer.valueOf(getConfigValue("connectionRequestTimeout", "500"));
    //数据传输的最长时间 连接上一个url后，获取response的返回等待时间 ，即在与目标url建立连接后，等待放回response的最大时间，在规定时间内没有返回响应的话就抛出SocketTimeout。
    private final Integer socketTimeout = Integer.valueOf(getConfigValue("socketTimeout", "10000"));
    private final Boolean staleConnectionCheckEnabled = Boolean.valueOf(getConfigValue("staleConnectionCheckEnabled", "true"));
    Logger logger = Logger.getLogger(HttpUtils.class);
    private String DEFAULT_CHARSET = "UTF-8";
    private PoolingHttpClientConnectionManager httpClientConnectionManager;
    private HttpClientBuilder httpClientBuilder;
    private CloseableHttpClient httpClient;
    private RequestConfig requestConfig;

    private HttpUtils() {
        this.httpClientConnectionManager = new PoolingHttpClientConnectionManager();
        this.httpClientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        this.httpClientConnectionManager.setMaxTotal(maxTotal);

        requestConfig = RequestConfig.custom().setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectionRequestTimeout).setSocketTimeout(socketTimeout).setStaleConnectionCheckEnabled(staleConnectionCheckEnabled).build();

        this.httpClientBuilder = HttpClientBuilder.create().setConnectionManager(httpClientConnectionManager).setDefaultRequestConfig(requestConfig);
        this.httpClient = httpClientBuilder.build();
        //关闭连接池的无效链接
        IdleConnectionEvictor idleConnectionEvictor = new IdleConnectionEvictor(this.httpClientConnectionManager);
    }

    private static String getConfigValue(String key, String defaultValue) {
        if (properties == null) {
            synchronized (HttpUtils.class) {
                if (properties == null) {
                    properties = new Properties();
                    InputStream inputStream = HttpUtils.class.getClassLoader().getResourceAsStream("config/httpclientPool.properties");
                    try {
                        properties.load(inputStream);
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        String val = properties.getProperty(key);
        return StringUtils.isEmpty(val) ? defaultValue : val;
    }

    public static HttpUtils getInstance() {
        if (instance == null) {
            synchronized (HttpUtils.class) {
                if (instance == null) {
                    instance = new HttpUtils();
                }

            }

        }
        return instance;
    }

    public String getResponseText(CloseableHttpResponse response, String charset) throws IOException {
        charset = StringUtils.isBlank(charset) ? this.attempt2FindCharset(response) : charset;
        // 获取服务端返回的数据,并返回
        String body = EntityUtils.toString(response.getEntity(), charset);
        logger.info("response:	" + body);
        return body;

    }

    /**
     * 解释response时 寻找头部中的charset
     * 缺省时取UTF-8
     *
     * @param response
     * @return
     * @author xuxingan
     */
    public String attempt2FindCharset(CloseableHttpResponse response) {
        Header contentType = response.getFirstHeader("Content-Type");
        if (contentType != null && contentType.getValue().toUpperCase().contains("CHARSET=")) {
            String[] charSeq = contentType.getValue().toUpperCase().split("CHARSET=");
            if (charSeq.length == 2) {
                return charSeq[1];
            }
        }
        return this.DEFAULT_CHARSET;
    }

    /**
     * 使用此方法发送get请求
     *
     * @param url
     * @param parameters
     * @return
     * @throws Exception
     * @author xuxingan
     */
    public String httpGet(String url, Map<String, Object> parameters) throws ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException, StatusCodeException, IOException {
        return httpGet(url, parameters, null, null);
    }

    /**
     * 使用此方法发送get请求
     *
     * @param url
     * @param parameters
     * @param headers
     * @param charset
     * @return
     * @throws Exception
     * @author xuxingan
     */
    public String httpGet(String url, Map<String, Object> parameters, Map<String, String> headers, String charset) throws ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException, StatusCodeException, IOException {
        return http(Method.GET, url, null, parameters, headers, charset, null);
    }

    /**
     * 调用http方法发送请求
     *
     * @param method      支持get post
     * @param url         支持淡出的url、支持包含query部分的url
     * @param parameters  get请求或者contentType为FORM的post请求的参数
     * @param headers     支持指定header信息。可选。
     *                    在此处指定的charset及contentType会被单独指定的值覆盖
     * @param charset     指定字符集 可选，不指定时按照以下顺序取默认值：1、headers['charset'] 2、默认UTF-8.
     *                    get请求不需要指定字符集
     * @param raw         指定post请求body部分的原生内容[contentType不为FORM时]
     *                    另注：contentType为FORM的POST请求 其body内容格式为 parameter1=value1&parameter2=value2&...... 即参数以key=value形式出现在body中 而不是在url中
     * @param contentType 指定contentType GET请求时缺省
     * @return
     * @author xuxingan
     */
    public String http(Method method, String url, ContentType contentType, Map<String, Object> parameters, Map<String, String> headers, String charset, String raw) throws StatusCodeException, ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException, IOException {
        if (method == null) {
            throw new IllegalArgumentException("非法参数:method");
        }
        if (method.equals(Method.GET)) {
            HttpGet httpGet = null;
            if (MapUtils.isNotEmpty(parameters)) {
                URIBuilder builder = null;
                try {
                    builder = new URIBuilder(url);
                    List<NameValuePair> parametersList = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                        parametersList.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
                    }
                    builder.addParameters(parametersList);
                    httpGet = new HttpGet(builder.build());
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("非法参数:url");
                }

            } else {
                httpGet = new HttpGet(url);
            }
            if (MapUtils.isNotEmpty(headers)) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpGet.addHeader(entry.getKey(), entry.getValue());
                }
            }
            httpGet.setConfig(this.requestConfig);
            return this.getResponseText(httpGet, charset);
        } else if (method.equals(Method.POST)) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(this.requestConfig);
            if (contentType == null) {
                throw new IllegalArgumentException("非法参数:contentType");
            }
            charset = this.attempt2DeterminateCharset(headers, charset);
            if (MapUtils.isNotEmpty(headers)) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("Content-Type")) {
                        continue;
                    }
                    httpPost.addHeader(entry.getKey(), entry.getValue());
                }
            }
            httpPost.addHeader("Content-Type", contentType.getContentType() + ";charset=" + charset);
            if (contentType.equals(ContentType.FORM)) {
                UrlEncodedFormEntity formEntity = null;
                List<NameValuePair> parametersList = new ArrayList<>();
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    parametersList.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
                }
                try {
                    formEntity = new UrlEncodedFormEntity(parametersList);
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException("非法参数:charset");
                }
                httpPost.setEntity(formEntity);
            } else {
                httpPost.setEntity(new StringEntity(raw, charset));
            }
            return this.getResponseText(httpPost, charset);
        }
        return null;
    }

    /**
     * 解析response中的body部分
     *
     * @param request
     * @param charset 指定字符集 如果未指定 则按照response的header中charset取值 若均无 默认取UTF-8
     * @return
     * @throws IOException
     * @author xuxingan
     */
    private String getResponseText(HttpUriRequest request, String charset) throws ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException, StatusCodeException, IOException {
        CloseableHttpResponse response = null;
        try {
            // 执行请求
            response = httpClient.execute(request);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                charset = StringUtils.isBlank(charset) ? this.attempt2FindCharset(response) : charset;
                // 获取服务端返回的数据,并返回
                return EntityUtils.toString(response.getEntity(), charset);
            } else {
                throw new StatusCodeException(response.getStatusLine().getStatusCode());
            }
        } catch (ConnectTimeoutException ex) {
            throw ex;
        } catch (HttpHostConnectException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    throw e;
                }
            }
        }

    }

    /**
     * 确定post请求的charset
     * 取值顺序：
     * 1、指定的charset参数
     * 2、指定header中的charset参数
     * 3、默认的charset UTF-8
     *
     * @param headers
     * @param charset
     * @return
     * @author xuxingan
     */
    private String attempt2DeterminateCharset(Map<String, String> headers, String charset) {
        if (StringUtils.isNotBlank(charset)) {
            return charset;
        }
        if (MapUtils.isNotEmpty(headers)) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (entry.getKey().equalsIgnoreCase("Content-Type") && String.valueOf(entry.getValue()).toUpperCase().contains("CHARSET=")) {
                    String[] charSeq = entry.getValue().toUpperCase().split("CHARSET=");
                    if (charSeq.length == 2) {
                        return charSeq[1];
                    }
                }
            }
        }
        return this.DEFAULT_CHARSET;
    }

    /**
     * 使用此方法发送post请求
     *
     * @param url
     * @param contentType
     * @param raw
     * @param charset
     * @return
     * @throws Exception
     * @author xuxingan
     */
    public String httpPost(String url, ContentType contentType, String raw, String charset) throws StatusCodeException, ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException, StatusCodeException, IOException {
        return httpPost(url, contentType, null, null, raw, charset);
    }

    /**
     * 推荐使用此方法发送post请求
     *
     * @param url
     * @param contentType
     * @param parameters
     * @param headers
     * @param raw
     * @param charset
     * @return
     * @throws Exception
     */
    public String httpPost(String url, ContentType contentType, Map<String, Object> parameters, Map<String, String> headers, String raw, String charset) throws ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException, StatusCodeException, IOException, StatusCodeException {
        return http(Method.POST, url, contentType, parameters, headers, charset, raw);
    }

    public CloseableHttpResponse invokeHttp(Method method, String url, ContentType contentType, List<NameValuePair> parameters, List<Header> headers, String charset, String raw) throws ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException, IOException {
        if (method == null) {
            throw new IllegalArgumentException("非法参数:method");
        }
        if (method.equals(Method.GET)) {
            HttpGet httpGet = null;
            if (CollectionUtils.isNotEmpty(parameters)) {
                URIBuilder builder = null;
                try {
                    builder = new URIBuilder(url);
                    builder.addParameters(parameters);
                    httpGet = new HttpGet(builder.build());
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("非法参数:url");
                }

            } else {
                httpGet = new HttpGet(url);
            }
            if (CollectionUtils.isNotEmpty(headers)) {
                for (Header entry : headers) {
                    httpGet.addHeader(entry);
                }
            }
            httpGet.setConfig(this.requestConfig);
            return this.getResponse(httpGet, charset);
        } else if (method.equals(Method.POST)) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(this.requestConfig);
            if (contentType == null) {
                throw new IllegalArgumentException("非法参数:contentType");
            }
            charset = this.attempt2DeterminateCharset(headers, charset);
            if (CollectionUtils.isNotEmpty(headers)) {
                for (Header entry : headers) {
                    if (entry.getName().equalsIgnoreCase("Content-Type")) {
                        continue;
                    }
                    httpPost.addHeader(entry);
                }
            }
            httpPost.addHeader("Content-Type", contentType.getContentType() + ";charset=" + charset);
            if (contentType.equals(ContentType.FORM)) {
                UrlEncodedFormEntity formEntity = null;

                try {
                    formEntity = new UrlEncodedFormEntity(parameters);
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException("非法参数:charset");
                }
                httpPost.setEntity(formEntity);
            } else {
                httpPost.setEntity(new StringEntity(raw, charset));
            }
            return this.getResponse(httpPost, charset);
        }
        return null;
    }

    private CloseableHttpResponse getResponse(HttpUriRequest request, String charset) throws ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException, IOException {
        CloseableHttpResponse response = null;
        try {
            // 执行请求
            response = httpClient.execute(request);
            return response;
        } catch (ConnectTimeoutException ex) {
            throw ex;
        } catch (HttpHostConnectException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }

    }

    private String attempt2DeterminateCharset(List<Header> headers, String charset) {
        if (StringUtils.isNotBlank(charset)) {
            return charset;
        }
        if (CollectionUtils.isNotEmpty(headers)) {
            for (Header entry : headers) {
                if (entry.getName().equalsIgnoreCase("Content-Type") && String.valueOf(entry.getValue()).toUpperCase().contains("CHARSET=")) {
                    String[] charSeq = entry.getValue().toUpperCase().split("CHARSET=");
                    if (charSeq.length == 2) {
                        return charSeq[1];
                    }
                }
            }
        }
        return this.DEFAULT_CHARSET;
    }

    /**
     * 调用http方法发送请求
     *
     * @param method      支持get post
     * @param url         支持淡出的url、支持包含query部分的url
     * @param parameters  get请求或者contentType为FORM的post请求的参数
     * @param headers     支持指定header信息。可选。
     *                    在此处指定的charset及contentType会被单独指定的值覆盖
     * @param charset     指定字符集 可选，不指定时按照以下顺序取默认值：1、headers['charset'] 2、默认UTF-8.
     *                    get请求不需要指定字符集
     * @param raw         指定post请求body部分的原生内容[contentType不为FORM时]
     *                    另注：contentType为FORM的POST请求 其body内容格式为 parameter1=value1&parameter2=value2&...... 即参数以key=value形式出现在body中 而不是在url中
     * @param contentType 指定contentType GET请求时缺省
     * @return
     * @author xuxingan
     */
    public CloseableHttpResponse invokeHttp(Method method, String url, ContentType contentType, Map<String, Object> parameters, Map<String, String> headers, String charset, String raw) throws ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException, IOException {
        if (method == null) {
            throw new IllegalArgumentException("非法参数:method");
        }
        if (method.equals(Method.GET)) {
            HttpGet httpGet = null;
            if (MapUtils.isNotEmpty(parameters)) {
                URIBuilder builder = null;
                try {
                    builder = new URIBuilder(url);
                    List<NameValuePair> parametersList = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                        parametersList.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
                    }
                    builder.addParameters(parametersList);
                    httpGet = new HttpGet(builder.build());
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("非法参数:url");
                }

            } else {
                httpGet = new HttpGet(url);
            }
            if (MapUtils.isNotEmpty(headers)) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpGet.addHeader(entry.getKey(), entry.getValue());
                }
            }
            httpGet.setConfig(this.requestConfig);
            return this.getResponse(httpGet, charset);
        } else if (method.equals(Method.POST)) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(this.requestConfig);
            if (contentType == null) {
                throw new IllegalArgumentException("非法参数:contentType");
            }
            charset = this.attempt2DeterminateCharset(headers, charset);
            if (MapUtils.isNotEmpty(headers)) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("Content-Type")) {
                        continue;
                    }
                    httpPost.addHeader(entry.getKey(), entry.getValue());
                }
            }
            httpPost.addHeader("Content-Type", contentType.getContentType() + ";charset=" + charset);
            if (contentType.equals(ContentType.FORM)) {
                UrlEncodedFormEntity formEntity = null;
                List<NameValuePair> parametersList = new ArrayList<>();
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    parametersList.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
                }
                try {
                    formEntity = new UrlEncodedFormEntity(parametersList);
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException("非法参数:charset");
                }
                httpPost.setEntity(formEntity);
            } else {
                httpPost.setEntity(new StringEntity(raw, charset));
            }
            return this.getResponse(httpPost, charset);
        }
        return null;
    }

    public CloseableHttpResponse postReturnResponse(String url, ContentType contentType, Map<String, Object> parameters, Map<String, String> headers, String charset, String raw) throws ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException,IOException {
        HttpPost httpPost =new HttpPost(url);
        httpPost.setConfig(this.requestConfig);
        if(contentType==null){
            throw new IllegalArgumentException("非法参数:contentType");
        }
        charset = this.attempt2DeterminateCharset(headers,charset);
        if(headers != null && headers.size()>0){
            for (Map.Entry<String,String> entry : headers.entrySet()){
                if(entry.getKey().equalsIgnoreCase("Content-Type")) {
                    continue;
                }
                httpPost.addHeader(entry.getKey(),entry.getValue());
            }
        }
        httpPost.addHeader("Content-Type",contentType.getContentType()+";charset="+charset);
        if(contentType.equals(ContentType.FORM)){
            UrlEncodedFormEntity formEntity =null;
            List<NameValuePair> parametersList=new ArrayList<>();
            for (Map.Entry<String,Object> entry:parameters.entrySet()){
                parametersList.add(new BasicNameValuePair(entry.getKey(),String.valueOf(entry.getValue())));
            }
            try {
                formEntity= new UrlEncodedFormEntity(parametersList);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("非法参数:charset");
            }
            httpPost.setEntity(formEntity);
        }else{
            httpPost.setEntity(new StringEntity(raw,charset));
        }
        CloseableHttpResponse response = httpClient.execute(httpPost);
        return response;
    }
    public CloseableHttpResponse getReturnResponse(String url, Map<String, Object> parameters, Map<String, String> headers, String charset) throws ConnectTimeoutException, SocketTimeoutException, HttpHostConnectException,IOException {
        HttpGet httpGet = null;
        if(parameters!=null&&parameters.size()>0){
            URIBuilder builder = null;
            try {
                builder = new URIBuilder(url);
                List<NameValuePair> parametersList=new ArrayList<>();
                for (Map.Entry<String,Object> entry:parameters.entrySet()){
                    parametersList.add(new BasicNameValuePair(entry.getKey(),String.valueOf(entry.getValue())));
                }
                builder.addParameters(parametersList);
                httpGet = new HttpGet(builder.build());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("非法参数:url");
            }

        }else{
            httpGet = new HttpGet(url);
        }
        if(headers != null && headers.size()>0){
            for (Map.Entry<String,String> entry : headers.entrySet()){
                httpGet.addHeader(entry.getKey(),entry.getValue());
            }
        }
        httpGet.setConfig(this.requestConfig);

        CloseableHttpResponse response = httpClient.execute(httpGet);
        return response;
    }

}

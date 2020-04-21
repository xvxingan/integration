package com.github.xvxingan.http;

/**
 * 错误码异常
 * http响应状态行中的状态码
 * 1xx：指示信息--表示请求已接收，继续处理。
 * 2xx：成功--表示请求已被成功接收、理解、接受。
 * 3xx：重定向--要完成请求必须进行更进一步的操作。
 * 4xx：客户端错误--请求有语法错误或请求无法实现。
 * 5xx：服务器端错误--服务器未能实现合法的请求。
 * 常见状态代码、状态描述的说明如下。
 *
 * 200 OK：客户端请求成功。
 * 400 Bad Request：客户端请求有语法错误，不能被服务器所理解。
 * 401 Unauthorized：请求未经授权，这个状态代码必须和WWW-Authenticate报头域一起使用。
 * 403 Forbidden：服务器收到请求，但是拒绝提供服务。
 * 404 Not Found：请求资源不存在，举个例子：输入了错误的URL。
 * 500 Internal Server Error：服务器发生不可预期的错误。
 * 503 Server Unavailable：服务器当前不能处理客户端的请求，一段时间后可能恢复正常，举个例子：HTTP/1.1 200 OK（CRLF）
 * @autho 徐兴安
 */
public class StatusCodeException extends Exception {
    private int statusCode;
    public StatusCodeException(int statusCode){
        this.statusCode = statusCode;
    }

    @Override
    public String getMessage() {
        return "http invoke exception,status code of response is ["+statusCode+"]";
    }

    @Override
    public String toString() {
        return "http invoke exception,status code of response is ["+statusCode+"]";
    }
    public int getStatusCode(){
        return this.statusCode;
    }
}

package com.github.xvxingan.http;

public enum Method{

		GET("GET"),POST("POST");

		private String method;
		Method(String method){
			this.method= method;
		}
		public String getMethod(){
			return method;
		}
}

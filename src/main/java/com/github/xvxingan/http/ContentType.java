package com.github.xvxingan.http;
/**
 * 例举 少量几种contentType
 * @author xuxingan
 */
public enum ContentType{
	FORM("application/x-www-form-urlencoded"),
	RAW_HTML("text/html"),
	RAW_TEXT("text/plain"),
	RAW_APPLICATION_JSON("application/json"),
	RAW_TEXT_JSON("text/json"),
	RAW_XML("application/xml");
		private String contentType;
		private ContentType(String contentType){
			this.contentType=contentType;
		}
		public String getContentType(){
			return this.contentType;
		}
	}
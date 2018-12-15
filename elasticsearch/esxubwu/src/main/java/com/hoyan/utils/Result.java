package com.hoyan.utils;

public class Result<T> {
	private int code;
	private String mes;
	private  T data;
	
	
	private Result(T data) {
		this.code=0;
		this.mes="success";
		this.data=data;
	}

	private Result(CodeMes cm) {
		if(cm == null){
			return;
		}
		this.code=cm.getCode();
		this.mes=cm.getMes();
	}

	public int getCode() {
		return code;
	}

	/*成功调用
	 * **/
	public static <T> Result<T> success(T data){
		return new Result<T>(data);
	}

	/*失败调用
	 * **/
   public static <T> Result<T> error(CodeMes cm){
	   return new Result<T>(cm);
	}
	
	public String getMes() {
		return mes;
	}
	public void setMes(String mes) {
		this.mes = mes;
	}
	public T getData() {
		return data;
	}
	public void setData(T data) {
		this.data = data;
	}
	public void setCode(int code) {
		this.code = code;
	}

	
	
	
}

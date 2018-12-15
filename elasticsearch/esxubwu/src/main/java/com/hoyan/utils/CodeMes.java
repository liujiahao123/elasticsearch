package com.hoyan.utils;

/**
 * @author 20160709
 *
 */
public class CodeMes {
    private int code;
    private String mes;
    
    
    public static CodeMes SUCCESSCodeMes = new CodeMes(0,"SUCCESS");
    public static CodeMes SERVICE_ERROR = new CodeMes(110,"服务器超时");
    
    
    
    
    
	public CodeMes(int code, String mes) {
		this.code=code;
		this.mes=mes;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public String getMes() {
		return mes;
	}
	public void setMes(String mes) {
		this.mes = mes;
	}
	
}

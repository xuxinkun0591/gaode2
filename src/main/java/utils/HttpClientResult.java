package utils;

public class HttpClientResult  {

    private int code;  //响应状态码
    private String content;  //响应数据
    
    public HttpClientResult(int code) {
		super();
		this.code = code;
	}

	public HttpClientResult(int code, String content) {
		super();
		this.code = code;
		this.content = content;
	}

	public int getCode() {
		return code;
	}

	public String getContent() {
		return content;
	}

	public String toString() {
    	String result = "code: "+ code + "; " + "content: " + content;
    	return result ;
    }
}
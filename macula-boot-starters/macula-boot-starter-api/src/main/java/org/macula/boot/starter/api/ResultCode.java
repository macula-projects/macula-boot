package org.macula.boot.starter.api;

/**
 * 返回码接口
 *
 * @author pangu
 */
public interface ResultCode {

	/**
	 * 返回码
	 *
	 * @return String
	 */
	String getCode();

	/**
	 * 返回消息
	 *
	 * @return String
	 */
	String getMsg();
}

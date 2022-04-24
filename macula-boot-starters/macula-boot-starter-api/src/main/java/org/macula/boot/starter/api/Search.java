package org.macula.boot.starter.api;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 搜索封装类
 *
 * @author pangu
 */
@Getter
@Setter
public class Search implements Serializable {

	/**
	 * 关键词
	 */
	private String keyword;

	/**
	 * 开始日期
	 */
	private String startDate;

	/**
	 * 结束日期
	 */
	private String endDate;

	/**
	 * 排序属性
	 */
	private String prop;

	/**
	 * 排序方式：asc,desc
	 */
	private String order;

	/**
	 * 当前页
	 */
	private Integer current = 1;

	/**
	 * 每页的数量
	 */
	private Integer size = 10;
}

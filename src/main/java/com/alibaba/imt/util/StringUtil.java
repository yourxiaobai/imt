package com.alibaba.imt.util;

/**
 * String工具类
 * @author hongwei.quhw
 *
 */
public class StringUtil {
	public static String trimToNull(String str) {
		if (str == null) {
			return null;
		}

		String result = str.trim();

		if (result == null || result.length() == 0) {
			return null;
		}

		return result;
	}
}

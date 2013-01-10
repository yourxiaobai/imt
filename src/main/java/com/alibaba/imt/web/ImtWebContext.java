package com.alibaba.imt.web;


import javax.servlet.ServletContext;

import org.apache.velocity.VelocityContext;

/**
 * 
 * @author hongwei.quhw
 *
 */
public class ImtWebContext extends VelocityContext{
	private final String url;
	private final String contextAttribute;
	private final ServletContext servletContext;
	private final String[] args;
	private final String[] additionalData;
	private final String key;
	
	public ImtWebContext(String url, String contextAttribute,
			ServletContext servletContext, String[] args,
			String[] additionalData, String key) {
		super();
		this.url = url;
		this.contextAttribute = contextAttribute;
		this.servletContext = servletContext;
		this.args = args;
		this.additionalData = additionalData;
		this.key = key;
	}

	public String getUrl() {
		return url;
	}

	public String getContextAttribute() {
		return contextAttribute;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public String[] getArgs() {
		return args;
	}

	public String[] getAdditionalData() {
		return additionalData;
	}

	public String getKey() {
		return key;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName());
		sb.append(" key=[").append(key).append("]");
		sb.append(", additionalData=[");
		if (null != additionalData) {
			for (String add : additionalData) {
				sb.append(add).append(",");
			}
		}
		sb.append("]");
		sb.append(", args=[");
		if (null != args) {
			for (String arg : args) {
				sb.append(arg).append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}
}

package com.alibaba.imt.web.servlet;

import static com.alibaba.imt.util.StringUtil.trimToNull;
import static com.alibaba.imt.web.ImtPageGen.process;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.alibaba.imt.web.ImtWebContext;

/**
 * IMT WEB 统一入口
 * @author hongwei.quhw
 *
 */
public class ImtFilter implements Filter{
	//配置了spring容器，且配置了特殊的容器名字时，需要把容器名字注入
	private String contextAttribute;
	private ServletContext servletContext;

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		resp.setContentType("text/html;charset=UTF-8");
		
		resp.getWriter().print(process(new ImtWebContext(
				((HttpServletRequest)req).getRequestURL().toString(),
				contextAttribute, 
				servletContext, 
				req.getParameterValues("arg"), 
				req.getParameterValues("additionalData"), 
				req.getParameter("key")
		)));
	}
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		servletContext = filterConfig.getServletContext();
	}
	
	@Override
	public void destroy() {
	}

	public final ServletContext getServletContext() {
		return servletContext;
	}
	
	public final String getContextAttribute() {
		return contextAttribute;
	}

	public final void setContextAttribute(String contextAttribute) {
		this.contextAttribute = trimToNull(contextAttribute);
	}

}

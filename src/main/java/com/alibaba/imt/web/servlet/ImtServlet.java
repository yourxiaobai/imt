package com.alibaba.imt.web.servlet;

import static com.alibaba.imt.util.StringUtil.trimToNull;
import static com.alibaba.imt.web.ImtPageGen.*;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.imt.web.ImtWebContext;

/**
 * IMT WEB 统一入口
 * @author hongwei.quhw
 *
 */
public class ImtServlet extends HttpServlet{
	//配置了spring容器，且配置了特殊的容器名字时，需要把容器名字注入
	private String contextAttribute;
	private ServletContext servletContext;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		resp.setContentType("text/html;charset=UTF-8");
		
		resp.getWriter().print(process(new ImtWebContext(
				req.getRequestURL().toString(),
				contextAttribute, 
				servletContext, 
				req.getParameterValues("arg"), 
				req.getParameterValues("additionalData"), 
				req.getParameter("key")
		)));
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		servletContext = config.getServletContext();
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

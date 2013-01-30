package com.alibaba.imt.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;

import com.alibaba.imt.web.ImtPageGen;
import com.alibaba.imt.web.ImtWebContext;
import com.alibaba.imt.web.ImtPageGen.ImtResourceLoader;

/*
 * 资源加载工具
 */
public class ResourceUtil {
	public static boolean isResource(ImtWebContext imtWebContext) {
		return isCssResource(imtWebContext) || isJsResource(imtWebContext) || isImgResource(imtWebContext);
	}
	
	public static boolean isCssResource(ImtWebContext imtWebContext) {
		return imtWebContext.getUrl().indexOf("/css") > 0;
	}
	
	public static boolean isJsResource(ImtWebContext imtWebContext) {
		return imtWebContext.getUrl().indexOf("/js") > 0;
	}
	
	public static boolean isImgResource(ImtWebContext imtWebContext) {
		return imtWebContext.getUrl().indexOf("/img") > 0;
	}
	
	public static void renderJsResource(ImtWebContext imtWebContext) throws IOException {
		int pos = imtWebContext.getUrl().indexOf("/js");
		String path = imtWebContext.getUrl().substring(pos);
		
		InputStream is = null;
		BufferedReader in = null;
	    try {
	    	is = ImtPageGen.class.getResourceAsStream(path);
	    	in = new BufferedReader(new InputStreamReader(is));
	    	
	    	String line = "";
			while ((line = in.readLine()) != null){
				imtWebContext.render(line);
			}
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			if (null != in) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public static void renderImgResource(ImtWebContext imtWebContext) throws IOException {
		int pos = imtWebContext.getUrl().indexOf("/img");
		String path = imtWebContext.getUrl().substring(pos);
		
		InputStream is = null;
	    try {
	    	is = ImtPageGen.class.getResourceAsStream(path);
	    	byte[] b = new byte[4000];
	    	is.read(b);
	    	
	    	imtWebContext.getResponse().getOutputStream().write(b);
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public static void renderCssResource(ImtWebContext context) throws IOException {
		int pos = context.getUrl().indexOf("/css");
		String path = context.getUrl().substring(pos);
		context.put("url", context.getUrl().substring(0, pos));
		context.put("encoding", context.getEncoding());
		
		context.render(merge(context, path));
	}
	
	public static String merge(ImtWebContext context, String path) {
		try {
			VelocityEngine ve = new VelocityEngine();
			ve.setProperty("resource.loader", "imt");
			ve.setProperty("imt.resource.loader.class", ImtResourceLoader.class.getName());
			ve.setProperty("input.encoding", "UTF-8");
			ve.setProperty("output.encoding", context.getEncoding());

			ve.init();
			
			Template template = ve.getTemplate(path, context.getEncoding());
			
			StringWriter writer = new StringWriter();
			template.merge(context, writer);
			
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException("渲染模版出错," , e);
		}
	}
}

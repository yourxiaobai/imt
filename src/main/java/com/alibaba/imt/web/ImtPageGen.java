package com.alibaba.imt.web;

import static com.alibaba.imt.util.ResourceUtil.isContent;
import static com.alibaba.imt.util.ResourceUtil.isCssResource;
import static com.alibaba.imt.util.ResourceUtil.isImgResource;
import static com.alibaba.imt.util.ResourceUtil.isInitPage;
import static com.alibaba.imt.util.ResourceUtil.isJsResource;
import static com.alibaba.imt.util.ResourceUtil.isMethodInvoke;
import static com.alibaba.imt.util.ResourceUtil.renderContent;
import static com.alibaba.imt.util.ResourceUtil.renderCssResource;
import static com.alibaba.imt.util.ResourceUtil.renderImgResource;
import static com.alibaba.imt.util.ResourceUtil.renderJsResource;
import static com.alibaba.imt.util.ResourceUtil.renderMethodInvoke;
import static com.alibaba.imt.util.ResourceUtil.renderPage;
import static com.alibaba.imt.util.StringUtil.trimToNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.context.WebApplicationContext;

import com.alibaba.imt.InterfaceManagementTool;
import com.alibaba.imt.adapter.privileges.ImtPrivilege;
import com.alibaba.imt.bean.ImtGroup;
import com.alibaba.imt.bean.ImtInfo;
import com.alibaba.imt.bean.InterfaceInfo;
import com.alibaba.imt.support.spring.BeanUtils;

/**
 * 页面生成工具
 * @author hongwei.quhw
 *
 */
public class ImtPageGen {
	
	public static void process(ImtWebContext imtWebContext) throws IOException {
		initInterfaceManagementTool(imtWebContext);
		
		if (isInitPage(imtWebContext)) {
			//初始化页面
			initData(imtWebContext);
			renderPage(imtWebContext);
		} else if (isContent(imtWebContext)) {
			renderContent(imtWebContext);	
		} else if (isMethodInvoke(imtWebContext)){
			//调方法
			renderMethodInvoke(imtWebContext);
		} else if (isJsResource(imtWebContext)) {
			//加载js文件
			renderJsResource(imtWebContext);
		} else if (isCssResource(imtWebContext)) {
			//加载css文件
			renderCssResource(imtWebContext);
		} else if (isImgResource(imtWebContext)) {
			//加载图片
			renderImgResource(imtWebContext);
		} else {
			throw new RuntimeException("参数错误:" + imtWebContext);
		}
	}
	
	private static void initData(ImtWebContext imtWebContext) {
		imtWebContext.put("url", imtWebContext.getUrl());
		imtWebContext.put("encoding", imtWebContext.getEncoding());
		
		//验证权限
		if(!authUser(imtWebContext)) {
			return;
		}
		
		InterfaceManagementTool tool = imtWebContext.getInterfaceManagementTool();
		tool.initGroups();
		
		imtWebContext.put("groups", tool.getImtGroups());
		imtWebContext.put("items", tool.getInterfaceInfoList());
		
	}

	private static boolean authUser(ImtWebContext imtWebContext) {
		
		ImtPrivilege imtPrivilege = imtWebContext.getInterfaceManagementTool().getImtPrivilege();
		
		boolean authed = true;
		
		if (null != imtPrivilege) {
			authed = imtPrivilege.authUser();
		} else { 
			//默认不校验
		}
		
		imtWebContext.put("authed", authed);
		
		return authed;
	}
	
	private static void initInterfaceManagementTool(ImtWebContext imtWebContext) {
		InterfaceManagementTool interfaceManagementTool = null;
		
		WebApplicationContext webApplicationContext =  BeanUtils.findWebApplicationContext(imtWebContext.getServletContext(), imtWebContext.getContextAttribute());
	
		if (null != webApplicationContext) {
			List<InterfaceManagementTool> tools = BeanUtils.getBeanByType(InterfaceManagementTool.class, webApplicationContext);
			if (null != tools && tools.size() > 1) {
				throw new RuntimeException("配置了多个InterfaceManagementTool， 请检查");
			}
			
			interfaceManagementTool= tools.get(0);
		}
		
		if (null == interfaceManagementTool) {
			throw new RuntimeException("InterfaceManagementTool,目前仅支持基于spring的配置，请耐心等候!");
		}
		
		imtWebContext.setInterfaceManagementTool(interfaceManagementTool);
	}
}

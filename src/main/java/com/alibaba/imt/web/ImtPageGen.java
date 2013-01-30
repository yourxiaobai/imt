package com.alibaba.imt.web;

import static com.alibaba.imt.util.StringUtil.trimToNull;
import static com.alibaba.imt.util.ResourceUtil.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.springframework.web.context.WebApplicationContext;

import com.alibaba.fastjson.JSON;
import com.alibaba.imt.InterfaceManagementTool;
import com.alibaba.imt.adapter.privileges.ImtPrivilege;
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
		
		String result = null;
		if (!isResource(imtWebContext) && null == trimToNull(imtWebContext.getKey())) {
			//初始化页面
			initData(imtWebContext);
			result = merge(imtWebContext, "/vm/page.vm");
			imtWebContext.setHtmlContentType();
			imtWebContext.render(result);
		} else if (null != imtWebContext.getAdditionalData()){
			//调方法
			Object ret = imtWebContext.getInterfaceManagementTool().invoke(imtWebContext.getKey(), imtWebContext.getAdditionalData(), imtWebContext.getArgs());
			result = JSON.toJSONString(ret);
			//imtWebContext.setJsonContentType();
			imtWebContext.setHtmlContentType();
			imtWebContext.render(result);
		} else if (isJsResource(imtWebContext)) {
			//加载js文件
			imtWebContext.setJavaScriptContentType();
			renderJsResource(imtWebContext);
		} else if (isCssResource(imtWebContext)) {
			//加载css文件
			imtWebContext.setCssContentType();
			renderCssResource(imtWebContext);
		} else if (isImgResource(imtWebContext)) {
			//加载图片
			renderImgResource(imtWebContext);
		}
		else {
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
		
		List<ImtGroup> groups = new ArrayList<ImtGroup>();
		InterfaceManagementTool tool = imtWebContext.getInterfaceManagementTool();
		List<InterfaceInfo> interfaceInfos = tool.getInterfaceInfoList();
		for (InterfaceInfo info : interfaceInfos) {
			String[] datas = info.getDatas();
			if (datas.length >= 3) {
				ImtGroup group = new ImtGroup(datas[2]);
				int index = groups.indexOf(group);
				if (index != -1) {
					group =  groups.get(index);
				} else {
					groups.add(group);
				}
				
				try {
					//next group 目前先支持二维
					ImtGroup nextGroup = group.getNextGroupByName(datas[3]);
					if (null == nextGroup) {
						nextGroup = new ImtGroup(datas[3]);
						group.addNext(nextGroup);
					} 
					
					nextGroup.addInterfaceInfo(info);
				} catch (IndexOutOfBoundsException e) {
					// 一维分组
					group.addInterfaceInfo(info);
				}
			}
			
		}
		
		imtWebContext.put("groups", groups);
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
	
	public static class ImtResourceLoader extends ResourceLoader {

		@Override
		public void init(ExtendedProperties configuration) {
		}

		@Override
		public InputStream getResourceStream(String source) throws ResourceNotFoundException {
			return getClass().getResourceAsStream(source);
		}

		@Override
		public boolean isSourceModified(Resource resource) {
			return false;
		}

		@Override
		public long getLastModified(Resource resource) {
			return 0;
		}
	}
	
	public static class ImtGroup {
		private final String uuid;
		private ImtGroup previous;
		private List<ImtGroup> nexts;
		private final String name;
		private List<InterfaceInfo> interfaceInfos;
		
		public ImtGroup(String name) {
			this.name = name;
			uuid = UUID.randomUUID().toString();
		}
		
		public String getUuid() {
			return uuid;
		}

		public String getName() {
			return name;
		}
		public ImtGroup getPrevious() {
			return previous;
		}
		public void setPrevious(ImtGroup previous) {
			this.previous = previous;
		}

		public List<ImtGroup> getNexts() {
			return nexts;
		}

		public void addNext(ImtGroup next) {
			if (null == nexts) {
				nexts = new ArrayList<ImtGroup>();
			}
			nexts.add(next);
		}

		public List<InterfaceInfo> getInterfaceInfos() {
			return interfaceInfos;
		}

		public void addInterfaceInfo(InterfaceInfo interfaceInfo) {
			if (null == interfaceInfos) {
				interfaceInfos = new ArrayList<InterfaceInfo>();
			}
			interfaceInfos.add(interfaceInfo);
		}

		public ImtGroup getNextGroupByName(String name) {
			if (null == nexts) {
				return null;
			}
			
			for (ImtGroup imtGroup : nexts) {
				if (name.equals(imtGroup.getName())) {
					return imtGroup;
				}
			}
			return null;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ImtGroup other = (ImtGroup) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
}

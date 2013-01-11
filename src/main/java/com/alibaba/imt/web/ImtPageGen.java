package com.alibaba.imt.web;

import static com.alibaba.imt.util.StringUtil.trimToNull;

import java.io.IOException;
import java.io.InputStream;
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
		if (null == trimToNull(imtWebContext.getKey())) {
			//验证权限
			authUser(imtWebContext);
			//初始化页面
			initData(imtWebContext);
			result = merge(imtWebContext);
			//imtWebContext.setHtmlContentType();
		} else if (null != imtWebContext.getAdditionalData()){
			//调方法
			Object ret = imtWebContext.getInterfaceManagementTool().invoke(imtWebContext.getKey(), imtWebContext.getAdditionalData(), imtWebContext.getArgs());
			result = JSON.toJSONString(ret);
			//imtWebContext.setJsonContentType();
		} else {
			throw new RuntimeException("参数错误:" + imtWebContext);
		}
		imtWebContext.setHtmlContentType();
		imtWebContext.render(result);
	}
	
	private static String merge(ImtWebContext context) {
		try {
			VelocityEngine ve = new VelocityEngine();
			ve.setProperty("resource.loader", "imt");
			ve.setProperty("imt.resource.loader.class", ImtResourceLoader.class.getName());
			ve.setProperty("input.encoding", "UTF-8");
			ve.setProperty("output.encoding", context.getEncoding());

			ve.init();
			
			Template template = ve.getTemplate("page.vm", context.getEncoding());
			
			StringWriter writer = new StringWriter();
			template.merge(context, writer);
			
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException("渲染模版出错," , e);
		}
	}
	
	private static void initData(ImtWebContext imtWebContext) {
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
		imtWebContext.put("url", imtWebContext.getUrl());
		imtWebContext.put("encoding", imtWebContext.getEncoding());
		imtWebContext.put("authed", imtWebContext.isAuthed());
	}

	private static void authUser(ImtWebContext imtWebContext) {
		WebApplicationContext webApplicationContext =  BeanUtils.findWebApplicationContext(imtWebContext.getServletContext(), imtWebContext.getContextAttribute());
		
		List<ImtPrivilege> imtPrivileges = null;
		if (null != webApplicationContext) {
			imtPrivileges = BeanUtils.getBeanByType(ImtPrivilege.class, webApplicationContext);
		}
		
		if (null == imtPrivileges) {
			throw new RuntimeException("ImtPrivilege,目前仅支持基于spring的配置，请耐心等候!");
		}
		
		for (ImtPrivilege privilege : imtPrivileges) {
			if (privilege.authUser()) {
				imtWebContext.setAuthed(true);
				break;
			}
		}
		
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

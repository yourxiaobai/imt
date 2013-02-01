package com.alibaba.imt.web;

import static com.alibaba.imt.util.ResourceUtil.isCssResource;
import static com.alibaba.imt.util.ResourceUtil.isImgResource;
import static com.alibaba.imt.util.ResourceUtil.isInitPage;
import static com.alibaba.imt.util.ResourceUtil.isJsResource;
import static com.alibaba.imt.util.ResourceUtil.isMethodInvoke;
import static com.alibaba.imt.util.ResourceUtil.renderCssResource;
import static com.alibaba.imt.util.ResourceUtil.renderImgResource;
import static com.alibaba.imt.util.ResourceUtil.renderJsResource;
import static com.alibaba.imt.util.ResourceUtil.renderMethodInvoke;
import static com.alibaba.imt.util.ResourceUtil.renderPage;
import static com.alibaba.imt.util.StringUtil.trimToNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.web.context.WebApplicationContext;

import com.alibaba.imt.InterfaceManagementTool;
import com.alibaba.imt.adapter.privileges.ImtPrivilege;
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
		
		List<ImtGroup> groups = new ArrayList<ImtGroup>();
		InterfaceManagementTool tool = imtWebContext.getInterfaceManagementTool();
		List<InterfaceInfo> interfaceInfos = tool.getInterfaceInfoList();
		for (InterfaceInfo info : interfaceInfos) {
			String[] datas = info.getDatas();
			if (null != info.getImtInfo() && null != trimToNull(info.getImtInfo().getMehtodDescrption())) {
				ImtInfo imtInfo = info.getImtInfo();
				String[] groupsArray = imtInfo.getGroup();
				ImtGroup imtGroup = null;
				if (null != groupsArray && groupsArray.length > 0) {
					imtGroup = new ImtGroup(groupsArray[0]);
					
					int index = groups.indexOf(imtGroup);
					if (index != -1) {
						imtGroup =  groups.get(index);
					} else {
						groups.add(imtGroup);
					}
					
					ImtGroup previous = imtGroup;
					for (int i = 1; i < groupsArray.length; i++) {
						ImtGroup nextGroup = previous.getNextGroupByName(groupsArray[i]);
						if (null == nextGroup) {
							nextGroup = new ImtGroup(groupsArray[i]);
							previous.addNext(nextGroup);
						} 
						previous = nextGroup;
					}
				} else {
					imtGroup = new ImtGroup(imtInfo.getMehtodDescrption());
					int index = groups.indexOf(imtGroup);
					if (index != -1) {
						imtGroup =  groups.get(index);
					} else {
						groups.add(imtGroup);
					}
				}
				
			} else if (null != datas && datas.length >= 3) {
				//老注解以数组形式
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
		
		@Override
		public String toString() {
			return name;
		}
	}
}

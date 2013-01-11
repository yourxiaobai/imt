package com.alibaba.imt.adapter.privileges;

/**
 * 权限回调，用于获取登陆用户
 * 应用方需实现此接口
 * 没有实现imt不做权限控制
 * @author hongwei.quhw
 *
 */
public interface ImtPrivilege {
	
	boolean authUser();
	
}

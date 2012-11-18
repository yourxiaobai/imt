package com.alibaba.imt;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.imt.adapter.BeanAdapter;
import com.alibaba.imt.bean.CheckResult;
import com.alibaba.imt.bean.InterfaceInfo;
import com.alibaba.imt.handler.Handler;
import com.alibaba.imt.handler.imp.InterfaceHandler;
import com.alibaba.imt.scanner.Scanner;
import com.alibaba.imt.scanner.imp.SimpleClassScanner;
import com.alibaba.imt.util.Util;

/**
 * InterfaceManageToolBean
 *
 */
public class InterfaceManagementTool{
    private Set<String> pkgs;
    private BeanAdapter beanAdapter;
    private Scanner scanner = new SimpleClassScanner();
    private Handler handler = new InterfaceHandler();
    private List<Map<String,Object>> dataList = null;
    private List<Map<String,Object>> xmlDataList = null;
    private List<InterfaceInfo> interfaceInfoList = null;
    private Map<String, InterfaceInfo> interfaceInfoMap = null;
    private boolean remoteManage = false;
    private boolean annotationScan = false;
   
    public void init(){
    	try{
	        if(dataList == null){
	        	dataList = new ArrayList<Map<String,Object>>();
	        	interfaceInfoList = new ArrayList<InterfaceInfo>();
	        	interfaceInfoMap = new HashMap<String, InterfaceInfo>();
	        }else{
	        	dataList.clear();
	        	interfaceInfoList.clear();
	        	interfaceInfoMap.clear();
	        }
	        Set<String> classNameSet = scanner.scan(pkgs);
	        for(String className : classNameSet){
	            handler.handleClass(className, dataList);
	        }
	        if(xmlDataList != null){
	            dataList.addAll(xmlDataList);
	        }
	        for(Map<String,Object> data : dataList){
	            if(beanAdapter != null){
	                Object[] additionalDatas = beanAdapter.getBeanDatas((Class<?>)data.get("clazz"));
	                data.put("additionalDatas", additionalDatas);
	            }
	            InterfaceInfo ii = Util.mapToBean(data, InterfaceInfo.class);
	            interfaceInfoList.add(ii);
	            interfaceInfoMap.put(ii.getKey(), ii);
	        }
    	}catch(Exception e){
    		//There is for container can start up normally
    		dataList = null;
    		e.printStackTrace();
    	}
        
    }
    
    public List<InterfaceInfo> getInterfaceInfoList(boolean refresh){
    	if(this.dataList == null || refresh){
    		init();
    		
    	}
    	return this.interfaceInfoList;
    }
    
    public List<InterfaceInfo> getInterfaceInfoList(){
    	return getInterfaceInfoList(false);
    }
    
    public Map<String, InterfaceInfo> getInterfaceInfoMap(boolean refresh){
    	if(this.dataList == null || refresh){
    		init();
    	}
    	return this.interfaceInfoMap;
    }
    
    public Map<String, InterfaceInfo> getInterfaceInfoMap(){
    	return getInterfaceInfoMap(false);
    }
    
    public Object invoke(String key, Object[] additionalDatas, Object[] args){
        InterfaceInfo ii = getInterfaceInfoMap().get(key);
        if(ii == null){
        	throw new RuntimeException("No class and method can be found: " + key);
        }
        Class<?>[] argumentClasses = ii.getArgumentClasses();
        
        args = Arrays.copyOfRange(args, 0, args.length, Object[].class);//make sure the args is an Object[]
        CheckResult<String> checkResult = checkAndConvertArgs(argumentClasses, args);
        if(!checkResult.isPassed()){
            return checkResult.getResult();
        }
        
        Class<?> clazz = ii.getClazz();
        
        Method method = null;
        Object result = null;
        Object object = null;
        try {
            method = clazz.getMethod(ii.getMethodName(), argumentClasses);
            if(Modifier.isStatic(method.getModifiers())){
                result = method.invoke(clazz, args);
            }else{
                if(beanAdapter != null){
                    object = beanAdapter.getObject(clazz, additionalDatas);
                    if(object == null){
                        throw new RuntimeException("No specified instance can be found via " + beanAdapter.getClass().getName() + ": class=" + clazz + ",additionalDatas=" + additionalDatas);
                    }
                    method = object.getClass().getMethod(ii.getMethodName(), argumentClasses);
                }else{
                    object = clazz.newInstance();
                }
                result = method.invoke(object, args);
            }
        
        } catch (Exception e) {
            String stackTrace = "";
            StringWriter writer = new StringWriter();
            try{
                e.printStackTrace(new PrintWriter(writer));
                stackTrace = writer.getBuffer().toString();
            }finally {
                if(writer != null)
                try {
                    writer.close();
                }catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
            //stackTrace = stackTrace.replaceAll("\r\n", "<br/>");
            StringBuilder sb = new StringBuilder("Method invoke has cause a exception :");
            sb.append(e.getMessage());
            sb.append("\r\n");
            sb.append(stackTrace);
            return sb.toString();
        }
        return result;
        
    }

    private CheckResult<String> checkAndConvertArgs(Class<?>[] argumentClasses, Object[] args) {
        CheckResult<String> cr = new CheckResult<String>();
        cr.setPassed(true);
        
        int argumentClassesLength = argumentClasses.length;
        int argsLength = args.length;
        if(argumentClassesLength != argsLength){
            cr.setPassed(false);
            cr.setResult("Argument count is wrong, expect " + argumentClassesLength + ", but " + argsLength);
            return cr;
        }
        for(int i = 0; i < argumentClassesLength; i++){
            Class<?> argumentClass = argumentClasses[i];
            Object arg = args[i];
            if(argumentClass.isInstance(arg)){
                continue;
            }
            try{
                String argStr = (String)arg;
                if(argumentClass.isPrimitive()){
                    if (argumentClass == Integer.TYPE) {
                        args[i] = Integer.valueOf(argStr);
                    }  else if (argumentClass == Boolean.TYPE) {
                        args[i] = Boolean.valueOf(argStr);
                    } else if (argumentClass == Byte.TYPE) {
                        args[i] = Byte.valueOf(argStr);
                    } else if (argumentClass == Character.TYPE) {
                        if(argStr.length() != 1){
                            throw new IllegalArgumentException(argStr + " is not a char.");
                        }
                        args[i] = Character.valueOf(argStr.charAt(0));
                    } else if (argumentClass == Short.TYPE) {
                        args[i] = Short.valueOf(argStr);
                    } else if (argumentClass == Double.TYPE) {
                        args[i] = Double.valueOf(argStr);
                    } else if (argumentClass == Float.TYPE) {
                        args[i] = Float.valueOf(argStr);
                    } else /* if (c == Long.TYPE) */{
                        args[i] = Long.valueOf(argStr);
                    }
                }else if(argumentClass.isArray()){
                    args[i] = convertArrayArgument(argStr, argumentClass.getComponentType());
                }else{
                    args[i] = JSON.parseObject(argStr, argumentClass);
                }
            }catch(RuntimeException re){
                cr.setPassed(false);
                cr.setResult("The type of argument at " + (i + 1) + " is wrong, expect " + argumentClass.getName() + ", but cannot convert the value of " + arg + " from the type " + arg.getClass().getName());
                return cr;
            }
        }
        return cr;
    }

    /**
     * @param argStr
     * @param componentType
     * @return
     */
    private <T>T[] convertArrayArgument(String argStr, Class<T> componentType){
        List<T> list = JSON.parseArray(argStr, componentType);
        T[] arr = list.toArray((T[])Array.newInstance(componentType, 0));
        return arr;
    }
    
    public Set<String> getPkgs() {
        return pkgs;
    }
    public void setPkgs(Set<String> pkgs) {
        this.pkgs = pkgs;
    }
    
    public BeanAdapter getBeanAdapter() {
        return beanAdapter;
    }

    public void setBeanAdapter(BeanAdapter beanAdapter) {
        this.beanAdapter = beanAdapter;
    }

    public List<Map<String, Object>> getXmlDataList() {
        return xmlDataList;
    }

    public void setXmlDataList(List<Map<String, Object>> xmlDataList) {
        this.xmlDataList = xmlDataList;
    }

    public boolean isRemoteManage() {
        return remoteManage;
    }

    public void setRemoteManage(boolean remoteManage) {
        this.remoteManage = remoteManage;
    }

    public boolean isAnnotationScan() {
        return annotationScan;
    }

    public void setAnnotationScan(boolean annotationScan) {
        this.annotationScan = annotationScan;
    }

    public static void main( String[] args ){

        Set<String> pkgs = new HashSet<String>();
        pkgs.add("com.alibaba.imt");
        InterfaceManagementTool imt = new InterfaceManagementTool();
        imt.setPkgs(pkgs);
        //imtb.setBeanAdapter(new RomaBeanAdapter());
        imt.init();
        List<InterfaceInfo> interfaceInfoList = imt.getInterfaceInfoList();
        for(InterfaceInfo interfaceInfo : interfaceInfoList){
            
            System.out.println("key:" + interfaceInfo.getKey());
            System.out.println("methodName:" + interfaceInfo.getMethodName());
            System.out.println("returnClass:" + interfaceInfo.getReturnClass());
            System.out.println("argumentClasses:" + interfaceInfo.getArgumentClasses());
            System.out.println("argumentClasses.num:" + interfaceInfo.getArgumentClasses().length);
            for(String group : interfaceInfo.getDatas()){
                System.out.println("group==" + group);
            }
        }
        Object obj = imt.invoke(Util.generateKey("com.alibaba.imt.Test", "testMethod2", "(ILjava/lang/String;)Ljava/lang/String;"), null, new Object[]{18, "你好"});
        System.out.println("this is result:" + obj);
        
    }
}

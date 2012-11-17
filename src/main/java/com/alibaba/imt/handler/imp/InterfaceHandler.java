/**
 * Project: imt
 * 
 * File Created at 2012-9-17
 * $Id: InterfaceHandler.java 23 2012-11-10 03:59:38Z yourxiaobai@gmail.com $
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.imt.handler.imp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.alibaba.imt.annotation.Interface;
import com.alibaba.imt.handler.Handler;
import com.alibaba.imt.util.Util;

/**
 * @author yansong.baiys
 *
 */
public class InterfaceHandler extends ClassVisitor implements Handler,Opcodes {


    private String internalClassName;
    private String methodName;
    private String methodDesc;
    private List<Map<String,Object>> dataList;
    private Map<String, Object>  dataMap;
	public InterfaceHandler(){
		super(ASM4);
	}

    @Override
    public void handleClass(String className, List<Map<String,Object>> dataList) {
        this.dataList = dataList;
    	ClassReader cr = null;
    	InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(className + ".class");
    	try {
			cr = new ClassReader(is);
		} catch (IOException e) {
		    throw new RuntimeException(e);
		}finally{
		    if(is != null){
		        try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
		    }
		}
        cr.accept(this, ClassReader.SKIP_DEBUG);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        
        this.internalClassName = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        InterfaceMethodVisitor imv = null;
        if((access & ACC_ABSTRACT) == 0 && (access & ACC_PUBLIC) != 0){
            imv = new InterfaceMethodVisitor();
            this.methodName = name;
            this.methodDesc = desc;
        }
        return imv;
    }

    private class InterfaceMethodVisitor extends MethodVisitor{
        
        public InterfaceMethodVisitor(){
            super(ASM4);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            InterfaceAnnotationVisitor iav = null;
            if(Type.getDescriptor(Interface.class).equals(desc)){
                
                dataMap = new HashMap<String, Object>();
                Class<?> clazz = null;
                String className = internalClassName.replace("/", ".");
                try {
                    clazz = Class.forName(className);
                } catch (ClassNotFoundException e1) {
                    new RuntimeException(e1);
                }
                
                Type returnType = Type.getReturnType(methodDesc);
                Class<?> returnClass = Util.getClass(returnType);
                
                Type[] argumentTypes = Type.getArgumentTypes(methodDesc);
                Class<?>[] argumentClasses = new Class[argumentTypes.length];
                for(int i = 0; i < argumentTypes.length; i++){
                    Type argumentType = argumentTypes[i];
                    Class<?> argumentClass = Util.getClass(argumentType);
                    argumentClasses[i] = argumentClass;
                }
                
                String key = Util.generateKey(className, methodName, methodDesc);
                dataMap.put("key", key);
                dataMap.put("internalClassName", internalClassName);
                dataMap.put("className", className);
                dataMap.put("clazz", clazz);
                dataMap.put("methodName", methodName);
                dataMap.put("methodDesc", methodDesc);
                dataMap.put("returnClass", returnClass);
                dataMap.put("argumentClasses", argumentClasses);
                dataList.add(dataMap);
                
                iav = new InterfaceAnnotationVisitor();
            }
            return iav;
        }
    }
    
    private class InterfaceAnnotationVisitor extends AnnotationVisitor{
        public InterfaceAnnotationVisitor(){
            super(ASM4);
        }

        @Override
        public void visit(String name, Object value) {
            
            dataMap.put(name, value);
        }
        
        @Override
        public void visitEnum(String name, String desc, String value) {
            dataMap.put(name, value);
        }
        
        @Override
        public AnnotationVisitor visitArray(final String name) {
            return new AnnotationVisitor(ASM4){
                private List<Object> valueList = new ArrayList<Object>();
                @Override
                public void visit(String name, Object value) {
                    valueList.add(value);
                }
                @Override
                public void visitEnd() {
                    String[] values = new String[valueList.size()];
                    values = valueList.toArray(values);
                    dataMap.put(name, values);
                }
            };
        }
        
    }
    

}

/**
 * Project: imt
 * 
 * File Created at 2012-9-17
 * $Id: SimpleClassScanner.java 17 2012-10-24 11:19:33Z yourxiaobai@gmail.com $
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
package com.alibaba.imt.scanner.imp;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.alibaba.imt.scanner.Scanner;

/**
 * @author yansong.baiys
 *
 */
public class SimpleClassScanner implements Scanner {

    @Override
    public Set<String> scan(Set<String> pkgs){
        if(pkgs == null){
            throw new IllegalArgumentException("The pkgs argument cannot be null!");
        }
        Set<String> allClassNameSet = new HashSet<String>();
        for(String pkgName : pkgs){
            Set<String> classNameSet = this.getClassInPackage(pkgName);
            allClassNameSet.addAll(classNameSet);
           
        }
        return allClassNameSet;
    }
    
    private Set<String> getClassInPackage(String pkgName) {
        Set<String> ret = new HashSet<String>();
        String innerPkgName = pkgName.replace('.', '/');
        String rPath = innerPkgName + "/";
        try {
            List<File>  classPaths = getClassPath();
            for (File classPath : classPaths) {
                if (!classPath.exists())
                    continue;
                if (classPath.isDirectory()) {
                    File dir = new File(classPath, rPath);
                    if (!dir.exists()){
                        continue;
                    }
                    recurCollect(dir, innerPkgName, ret);
                    
                } else {
                    FileInputStream fis = new FileInputStream(classPath);
                    JarInputStream jis = new JarInputStream(fis, false);
                    JarEntry e = null;
                    while ((e = jis.getNextJarEntry()) != null) {
                        String eName = e.getName();
                        if (eName.startsWith(rPath) && eName.endsWith(".class")) {
                            ret.add(eName.substring(0, eName.length() - 6));
                        }
                        jis.closeEntry();
                    }
                    jis.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ret;
    }

    private void recurCollect(File dir, String innerPkgName, Set<String> ret) {
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                String clsName = file.getName();
                if(clsName.endsWith(".class")){
                    clsName = innerPkgName + "/" + clsName.substring(0, clsName.length() - 6);
                    ret.add(clsName);
                }
            }else{
                recurCollect(file, innerPkgName + "/" + file.getName(), ret);
            }
        }
        
    }

    private List<File> getClassPath() {
        List<File> ret = new ArrayList<File>();
        
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        
        URL[] urls = ((URLClassLoader)contextClassLoader).getURLs();
        for(URL url : urls){
            if(url != null){
                ret.add(new File(url.getPath()));
            }
        }
        
        return ret;
    }
}

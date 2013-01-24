/**
 * Project: imt
 * 
 * File Created at 2012-9-17
 * $Id: Scanner.java 15 2012-10-17 09:16:05Z yourxiaobai@gmail.com $
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
package com.alibaba.imt.scanner;

import java.util.Set;

/**
 * @author yansong.baiys
 *
 */
public interface Scanner {

    Set<String> scan(Set<String> pkgs);

}

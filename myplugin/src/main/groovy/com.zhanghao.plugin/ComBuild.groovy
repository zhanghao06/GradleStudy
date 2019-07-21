/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.zhanghao.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class ComBuild implements Plugin<Project> {

    @Override
    void apply(Project project) {
        System.out.println("Hello Word")
    }
}
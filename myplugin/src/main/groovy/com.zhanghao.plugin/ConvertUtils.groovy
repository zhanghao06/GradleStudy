/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.zhanghao.plugin

import com.android.SdkConstants
import com.android.build.api.transform.TransformInput
import javassist.ClassPool
import javassist.CtClass
import javassist.NotFoundException
import org.apache.commons.io.FileUtils

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Matcher

class ConvertUtils {
    static List<CtClass> toCtClass(Collection<TransformInput> inputs, ClassPool classPool) {
        List<String> classNames = new ArrayList<>()
        List<CtClass> allClass = new ArrayList<>()
        inputs.each {
            it.directoryInputs.each {
                def dirPath = it.file.absolutePath
                classPool.insertClassPath(it.file.absolutePath)
                System.out.println("dirPath: "+it.file.absolutePath)
                FileUtils.listFiles(it.file,null,true).each {
                    System.out.println("listFiles: "+it.absolutePath)
                    if (it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                        String className = it.absolutePath.substring(dirPath.length()+1,it.absolutePath.length()-
                                SdkConstants.DOT_CLASS.length()).replaceAll(Matcher.quoteReplacement(File.separator),".")
                        System.out.println("className: "+className)
                        if (classNames.contains(className)) {
                            throw new RuntimeException("className: "+className + "Already add.")
                        }
                        classNames.add(className)
                    }

                }
            }
            it.jarInputs.each {
                System.out.println("jarInputs path"+it.file.absolutePath)
                classPool.insertClassPath(it.file.absolutePath)
                def jarFile = new JarFile(it.file)
                Enumeration<JarEntry> classes = jarFile.entries()
                while (classes.hasMoreElements()) {
                    JarEntry libClass = classes.nextElement()
                    String className = libClass.getName()
                    System.out.println("jarInputs className"+className)
                    if (className.endsWith(SdkConstants.DOT_CLASS)) {
                        className = className.substring(0, className.length() - SdkConstants.DOT_CLASS.length()).replaceAll('/', '.')
                        System.out.println("jarInputs className al "+className)
                        if (classNames.contains(className)) {
                            throw new RuntimeException("You have duplicate classes with the same name : " + className + " please remove duplicate classes ")
                        }
                        classNames.add(className)
                    }
                }
                jarFile.close()
            }
        }
        classNames.each {
            try {
                allClass.add(classPool.get(it))
            } catch (NotFoundException e) {
                println "class not found exception class name:  $it ,$e.getMessage()"
            }
        }
        return allClass
    }
}
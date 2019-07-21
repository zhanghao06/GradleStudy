/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.zhanghao.plugin

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class ComCodeTransform extends Transform {
    private Project project
    ClassPool classPool
    String applicationName

    ComCodeTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "ComCodeTransform"
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        getRealApplication()
        classPool = new ClassPool()
        project.android.bootClasspath.each {
            classPool.appendClassPath((String) it.absolutePath)
            System.out.println("bootClasspath : " + it.absolutePath)
        }
        //要收集的application，一般情况下只有一个
        List<CtClass> applications = new ArrayList<>()
        List<CtClass> likes = new ArrayList<>()
        def box = ConvertUtils.toCtClass(transformInvocation.getInputs(), classPool)
        for (CtClass ctClass : box) {
            if (isApplication(ctClass)) {
                applications.add(ctClass)
            }
            if (isLike(ctClass)) {
                likes.add(ctClass)
            }
        }
        for (CtClass ctClass : applications) {
            System.out.println("appliCation : " + ctClass.getName())
        }
        for (CtClass ctClass : likes) {
            System.out.println("likes : " + ctClass.getName())
        }
        transformInvocation.inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                //jar文件一般是第三方依赖库jar文件
                // 重命名输出文件（同目录copyFile会冲突）
                System.out.println("jarInput.name:  " + jarInput.name)
                def jarName = jarInput.name
                System.out.println("jarInput.file.getAbsolutePath() : " + jarInput.file.getAbsolutePath())
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //生成输出路径
                def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                System.out.println("生成输出路径dest " + dest)
                FileUtils.copyFile(jarInput.file, dest)
            }
            input.directoryInputs.each { DirectoryInput directoryInput ->
                System.out.println(">>>")
                System.out.println(">>>")
                System.out.println(">>>")
                System.out.println(">>>")
                System.out.println("check isRegisterCompoAuto:  " + directoryInput.file.getPath())
                String fileName = directoryInput.file.absolutePath
                System.out.println("absolutePath:  " + fileName)
                File dir = new File(fileName)
                dir.eachFileRecurse {
                    String filePath = it.absolutePath
                    System.out.println("eachDirRecurse : " + filePath)
                    String classNameTemp = filePath.replace(fileName, "")
                            .replace("\\", ".")
                            .replace("/", ".")
                    System.out.println("eachDirRecurse al: " + classNameTemp)
                    if (classNameTemp.endsWith(".class")) {
                        String className = classNameTemp.substring(1, classNameTemp.length() - 6)
                        System.out.println("className : " + className)
                        if (className.equals(applicationName)) {
                            System.out.println("size: " + applications.size())
                            CtClass a = applications.get(0)
                            System.out.println("isnull: " + a == null)
                            injectApplicationCode(a, likes, fileName)
                            a.detach()
                        }
                    }
                }
                def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)
                // 将input的目录复制到output指定目录

                FileUtils.copyDirectory(directoryInput.file, dest)
            }

        }

    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    void getRealApplication() {
        applicationName = project.extensions.combuild.applicationName
        if (applicationName == null || applicationName.isEmpty()) {
            throw new RuntimeException("you should set applicationName in combuild")
        }
    }

    boolean isApplication(CtClass ctClass) {
        def className = ctClass.getName()
        try {
            if (applicationName != null && applicationName == className) {
                return true
            }
        } catch (Exception e) {
            println "class not found exception class name:  " + ctClass.getName()
        }
        return false
    }

    boolean isLike(CtClass ctClass) {
        try {
            for (CtClass itClass : ctClass.getInterfaces()) {
                if ("com.example.componentlib.IlikeApplication".equals(itClass.name)) {
                    return true
                }
            }
        } catch (Exception e) {
            println "isLike got exception :" + ctClass.getName() + "   ;  " + e.toString()
        }
        return false
    }

    private void injectApplicationCode(CtClass ctClassApplication, List<CtClass> likes, String patch) {
        System.out.println("injectApplicationCode begin 1" + ctClassApplication == null)
        //ctClassApplication.defrost()
        //CtMethod attachBaseContextMethod = ctClassApplication.getDeclaredMethod("onCreate", null)
        //attachBaseContextMethod.insertAfter(getAutoLoadComCode(likes))
        CtMethod[] ctMethods = ctClassApplication.getDeclaredMethods()
        System.out.println("ctMethods : "+ctMethods.length)
        System.out.println("ctMethods : "+ctMethods[0])
        ctMethods[0].insertAfter(getAutoLoadComCode(likes))
        System.out.println("injectApplicationCode success " + patch)
        ctClassApplication.writeFile(patch)


        System.out.println("injectApplicationCode success ")
    }

    String getAutoLoadComCode(List<CtClass> likes) {
        StringBuilder stringBuilder = new StringBuilder()
        for (CtClass likeClass : likes) {
            stringBuilder.append("new " + likeClass.name + "().onCreate();")
        }
        return stringBuilder
    }
}
package com.cx.plugin

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

class FakePlugin implements Plugin<Project> {

    Project project

    @Override
    void apply(Project project) {
        this.project = project
        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException('只能与android application同时使用')
        }
        def aar = loadFakeDex()
        //创建一个扩展，用来配置aes秘钥
        project.extensions.create('fake', FakeExtension)
        project.afterEvaluate {
            project.android.applicationVariants.all {
                ApplicationVariantImpl variant ->
                    //创建任务，向manifest中插入meta-data,保存aes秘钥
                    def taskName = "${variant.flavorName.capitalize()}${variant.buildType.name.capitalize()}"
                    FakeManifestTask task = project.tasks.create("fakeManifest${taskName}", FakeManifestTask)
                    task.manifestFile = variant.outputs.first().processManifest.manifestOutputFile
                    //已经存在manifest文件并且在打包前
                    task.mustRunAfter variant.outputs.first().processManifest
                    variant.outputs.first().processResources.dependsOn task

                    def fakePath = "${project.buildDir}/outputs/fake"
                    //加密任务
                    FakeDexTask dexTask = project.tasks.create("fakeDex${taskName}",FakeDexTask)
                    dexTask.aarFile = aar
                    dexTask.apkFile = variant.outputs.first().outputFile
                    dexTask.outputs.file(fakePath)
                    dexTask.baseName = "${project.name}-${variant.baseName}"


                    FakePackageTask fakePackageTask = project.tasks.create("fakePackageDex${taskName}",FakePackageTask)
                    fakePackageTask.outputs.file("${fakePath}/outs")
                    fakePackageTask.inputs.file(fakePath)
                    fakePackageTask.baseName = "${project.name}-${variant.baseName}"
                    fakePackageTask.signConfig = variant.variantData.variantConfiguration.signingConfig

                    fakePackageTask.dependsOn dexTask
                    def assembleTask = project.tasks.getByName("assemble${taskName}")
                    def packageTask = project.tasks.getByName("package${taskName}")
                    assembleTask.dependsOn fakePackageTask
                    dexTask.dependsOn packageTask


            }

        }
    }

    def loadFakeDex() {
        //创建一个依赖分组
        def config = project.configurations.create('fakeClasspath')
        //创建需要拉取的工件信息
        def notaion = [group: 'com.cx.fakedex', name: 'fakedex', version: '1.0']
        project.logger.quiet("config.name-------->${config.name}")
        Dependency dp = project.dependencies.add(config.name, notaion)
        def file = config.fileCollection(dp).singleFile
        project.logger.quiet("config.fileCollection(dp).singleFile-------->${file.absolutePath}")
        project.logger.quiet("FAKE:获取${notaion} 依赖${file}")
        file
    }


}
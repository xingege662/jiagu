package com.cx.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.security.MessageDigest
import java.util.zip.Adler32

class FakeDexTask extends DefaultTask {

    File apkFile
    File aarFile
    String baseName

    FakeDexTask() {
        group = '加固dex'
        description = '加密dex'
        outputs.upToDateWhen {
            false
        }
        String key = project.fake.key
        if (null != key && !key.isEmpty())
            AES.init(key)
        else
            AES.init(AES.DEFAULT_PWD)

    }

    @TaskAction
    def run() {
        def outDir = outputs.files.singleFile
        def fakeDex = new File(outDir, 'fakeDex')
        def dx = project.extensions.fake.dexpath
        //解压aar到 build/outputs/fake
        Zip.unZip(aarFile, fakeDex)
        File classJar
        fakeDex.listFiles().each {
            if (it.name == 'classes.jar') {
                classJar = it
            } else {
                it.delete()
            }

        }
        //dx --dex --output=path/dex classesJar
        File aarDex = new File("${classJar.parent}/classes.dex")
        def result = "${dx} --dex --output=${aarDex} ${classJar}".execute()
        def out = new StringBuffer()
        def err = new StringBuffer()
        result.waitForProcessOutput(out, err)
        if (result.exitValue() != 0) {
            project.logger.quiet("FAKE:执行dx失败")
            throw new GradleException("执行dx失败")
        }

        //加密

        //解压apk
        def unZipFile = new File(outDir, baseName)
        Zip.unZip(apkFile, unZipFile)

        //查找apk中所有的dex文件
        def dexFiles = unZipFile.listFiles().findAll {
            it.name.endsWith(".dex")
        }
        dexFiles.each {
            //加密dex
            def fakeDexDatas = AES.encrypt(it.bytes)
            it.withOutputStream {
                it.write(fakeDexDatas)
            }
        }

        def maindex = dexFiles.find {
            it.name == "classes.dex"
        }

        def maindexBytes = maindex.bytes
        def aarDexBytes = aarDex.bytes
        def newDex = new byte[maindexBytes.length + aarDexBytes.length + 4]

        //拷贝数据
        //先拷贝aar的数据
        System.arraycopy(aarDexBytes, 0, newDex, 0, aarDexBytes.length)
        //拷贝maindex的数据
        System.arraycopy(maindexBytes, 0, newDex, aarDexBytes.length, maindexBytes.length)
        //拷贝maindex的长度
        System.arraycopy(Utils.int2Bytes(maindexBytes.length), 0, newDex, aarDexBytes.length + maindexBytes.length , 4)

        //修改长度信息 file_size
        def fileSize = Utils.int2Bytes(newDex.length)
        System.arraycopy(fileSize, 0, newDex, 32, 4)
        //替换签名
        def md = MessageDigest.getInstance('SHA-1')
        md.update(newDex, 32, newDex.length - 32)
        def sha1 = md.digest()
        System.arraycopy(sha1, 0, newDex, 12, 20)
        //计算checksum
        Adler32 adler32 = new Adler32()
        adler32.update(newDex, 12, newDex.length - 12)
        int value = adler32.getValue()
        def checkSum = Utils.int2Bytes(value)
        System.arraycopy(checkSum, 0, newDex, 8, 4)

        maindex.delete()
        maindex.withOutputStream {
            it.write(newDex)
        }
    }
}
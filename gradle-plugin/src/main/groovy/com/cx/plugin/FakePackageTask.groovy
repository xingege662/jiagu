package com.cx.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class FakePackageTask extends DefaultTask {

    def signConfig
    String baseName

    FakePackageTask() {
        group = '加固dex'
        description = '加密dex'
        outputs.upToDateWhen {
            false
        }
    }

    @TaskAction
    def run() {
        def dir = new File(inputs.files.singleFile,baseName)
        def outs = outputs.files.singleFile
        outs.mkdirs()
        def unsignedApk = new File(outs, "${baseName}-unsigned.apk")
        Zip.zip(dir, unsignedApk)
        if (!signConfig) {
            return
        }
        def signedApk = new File(outs, "${baseName}-signed.apk")

        def cmd = [
                "jarsigner", "-verbose", "-sigalg", "MD5withRSA",
                "-digestalg", "SHA1",
                "-keystore", signConfig.storeFile,
                "-storepass", signConfig.storePassword,
                "-keypass", signConfig.keyPassword,
                "-signedjar", signedApk.absolutePath,
                unsignedApk.absolutePath,
                signConfig.keyAlias
        ]
        def stdout = new StringBuffer()
        def stderr = new StringBuffer()
        project.logger.quiet("FAKE: 签名${signedApk}")
        def result = cmd.execute()
        result.waitForProcessOutput(stdout, stderr)
        if (result.exitValue() != 0) {
            def output = "FAKE: stdout: ${stdout}. stderr: ${stderr}"
            throw new GradleException(output)
        }
    }
}
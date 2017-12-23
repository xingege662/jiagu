package com.cx.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


class FakeManifestTask extends DefaultTask{
    File manifestFile

    def static final  FAKE_KEY = "fake_key"

    FakeManifestTask() {
        group = '加固dex'
        description = '将aes key插入manifest中'
        outputs.upToDateWhen {
            false
        }
    }
    @TaskAction
    def run(){
        String key = project.extensions.fake.key
        if (key == null || key.isEmpty()) {
            return null
        }

        project.logger.quiet("Fake:操作manifest 增加 meta-data ${key}")
        def xml = new XmlParser().parse(manifestFile)
        def ns = new groovy.xml.Namespace("http://schemas.android.com/apk/res/android","android")
        Node application = xml.application[0]
        def metaDataTags = application['meta-data']

        metaDataTags.findAll {
            groovy.util.Node node ->
                node.attributes()[ns.name] == FAKE_KEY
        }.each {
            groovy.util.Node node ->
                node.parent().remove(node)
        }

        application.appendNode('meta-data',[(ns.name):FAKE_KEY,(ns.value):key])
        def pw = new XmlNodePrinter(new PrintWriter(manifestFile, 'UTF-8'))
        pw.print(xml)


    }
}
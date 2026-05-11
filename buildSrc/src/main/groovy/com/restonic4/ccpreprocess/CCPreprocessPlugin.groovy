package com.restonic4.ccpreprocess

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.compile.JavaCompile

class CCPreprocessPlugin implements Plugin<Project> {

    private static final Set<String> KNOWN_LOADERS = ['fabric', 'forge', 'neoforge'] as Set

    @Override
    void apply(Project project) {
        def ext = project.extensions.create('ccpreprocess', CCPreprocessExtension)

        project.afterEvaluate {
            Map<String, String> ctx = buildContext(project, ext)
            project.logger.lifecycle("[CC] ${project.path} → context: $ctx")

            project.tasks.withType(JavaCompile).each { JavaCompile compileTask ->
                String preprocessTaskName = "preprocessJavaFor${compileTask.name.capitalize()}"

                if (project.tasks.findByName(preprocessTaskName) != null) return

                FileCollection originalSources = project.files(compileTask.source)

                def preprocessTask = project.tasks.register(preprocessTaskName, PreprocessJavaTask) { t ->
                    t.group       = 'cc-preprocess'
                    t.description = "Preprocess //CC directives for ${compileTask.name}"
                    t.sourceFiles.from(originalSources)
                    t.context.set(ctx)
                    t.outputDir.set(project.layout.buildDirectory.dir("cc-preprocessed/${compileTask.name}"))
                }

                compileTask.dependsOn(preprocessTask)
                compileTask.source = project.files(preprocessTask.map { it.outputDir })
            }
        }
    }

    private Map<String, String> buildContext(Project project, CCPreprocessExtension ext) {
        def prop = { String name -> project.findProperty(name)?.toString() }

        String minecraft = ext.minecraftVersion ?: prop('minecraft_version') ?: ''
        String loader    = ext.loader           ?: resolveLoader(project)
        String java      = ext.javaVersion      ?: prop('java_version') ?: System.getProperty('java.specification.version') ?: '17'

        Map<String, String> ctx = [
                minecraft  : minecraft,
                loader     : loader,
                java       : java,
                is_fabric  : (loader == 'fabric'  ).toString(),
                is_forge   : (loader == 'forge'   ).toString(),
                is_neoforge: (loader == 'neoforge').toString(),
                is_common  : (loader == 'common'  ).toString(),
        ]

        if (ext.extra) ctx.putAll(ext.extra)
        return ctx
    }

    private static String resolveLoader(Project project) {
        Project cursor = project
        while (cursor != null && cursor != cursor.rootProject) {
            if (cursor.name.toLowerCase() in KNOWN_LOADERS) return cursor.name.toLowerCase()
            cursor = cursor.parent
        }
        return 'common'
    }
}
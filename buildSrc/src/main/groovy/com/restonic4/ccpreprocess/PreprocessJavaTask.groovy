package com.restonic4.ccpreprocess

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*

/**
 * PreprocessJavaTask
 *
 * Reads all .java files from the registered source sets, runs them through
 * ConditionalCompilePreprocessor, and writes the results to outputDir.
 * The compileJava task is then pointed at outputDir instead of the original sources.
 */
@CacheableTask
abstract class PreprocessJavaTask extends DefaultTask {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract org.gradle.api.file.ConfigurableFileCollection getSourceFiles()

    @Input
    abstract MapProperty<String, String> getContext()

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @TaskAction
    void preprocess() {
        File outRoot = outputDir.get().asFile
        outRoot.deleteDir()
        outRoot.mkdirs()

        def preprocessor = new ConditionalCompilePreprocessor(context.get())
        int processed = 0
        int skipped = 0

        sourceFiles.asFileTree.visit { details ->
            if (details.directory) return

            File src = details.file
            File dest = new File(outRoot, details.relativePath.pathString)
            dest.parentFile.mkdirs()

            if (src.name.endsWith('.java')) {
                try {
                    String original = src.getText('UTF-8')
                    String result = preprocessor.process(original)
                    dest.setText(result, 'UTF-8')
                    processed++
                } catch (Exception e) {
                    throw new org.gradle.api.GradleException(
                            "CC Preprocessor failed in file: ${src.absolutePath}\n  → ${e.message}", e
                    )
                }
            } else {
                // Copy non-java files as-is
                dest.bytes = src.bytes
                skipped++
            }
        }

        logger.lifecycle("CC Preprocessor: processed $processed Java file(s), copied $skipped other file(s) → ${outRoot}")
    }
}
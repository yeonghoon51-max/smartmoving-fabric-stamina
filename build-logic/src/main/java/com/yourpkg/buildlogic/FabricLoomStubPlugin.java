package com.yourpkg.buildlogic;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class FabricLoomStubPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply("java");
        project.getConfigurations().maybeCreate("minecraft");
        project.getConfigurations().maybeCreate("mappings");
        project.getConfigurations().maybeCreate("modImplementation");
        project.getConfigurations().maybeCreate("modCompileOnly");
        project.getExtensions().create("loom", LoomExtension.class, project);
    }
}

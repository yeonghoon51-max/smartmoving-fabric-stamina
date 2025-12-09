package com.yourpkg.buildlogic;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class FabricLoomStubPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java");

        Configuration minecraft = project.getConfigurations().maybeCreate("minecraft");
        Configuration mappings = project.getConfigurations().maybeCreate("mappings");
        project.getConfigurations().maybeCreate("modImplementation")
            .extendsFrom(project.getConfigurations().getByName("implementation"));
        project.getConfigurations().maybeCreate("modCompileOnly")
            .extendsFrom(project.getConfigurations().getByName("compileOnly"));

        project.getExtensions().create("loom", LoomExtension.class, project, minecraft, mappings);
    }
}

package com.yourpkg.buildlogic;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

public class LoomExtension {
    private final Project project;
    private final Configuration minecraft;
    private final Configuration mappings;

    public LoomExtension(Project project, Configuration minecraft, Configuration mappings) {
        this.project = project;
        this.minecraft = minecraft;
        this.mappings = mappings;
    }

    public void splitEnvironmentSourceSets() {
        // no-op in stub
    }

    public Object yarnMappings(String notation) {
        Dependency dep = project.getDependencies().create("yarn:" + notation);
        mappings.getDependencies().add(dep);
        return dep;
    }

    public Configuration getMinecraft() { return minecraft; }
    public Configuration getMappings() { return mappings; }
}

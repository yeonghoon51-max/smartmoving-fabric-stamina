package com.yourpkg.buildlogic;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;

public class LoomExtension {
    private final Project project;

    public LoomExtension(Project project) {
        this.project = project;
    }

    public void splitEnvironmentSourceSets() {
        project.getExtensions().configure(SourceSetContainer.class, sourceSets -> {
            var main = sourceSets.getByName("main");
            main.getJava().srcDir("src/client/java");
            main.getResources().srcDir("src/client/resources");
        });
    }
}

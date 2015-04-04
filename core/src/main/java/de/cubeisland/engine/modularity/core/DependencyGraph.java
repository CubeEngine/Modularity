package de.cubeisland.engine.modularity.core;

import java.util.HashSet;
import java.util.Set;

public class DependencyGraph
{
    private Set<ModuleMetadata> modules;

    public DependencyGraph()
    {
        this.modules = new HashSet<ModuleMetadata>();
    }

    public void addModule(ModuleMetadata metadata)
    {

    }
}

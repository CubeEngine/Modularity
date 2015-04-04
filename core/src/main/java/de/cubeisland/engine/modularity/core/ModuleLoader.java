package de.cubeisland.engine.modularity.core;

import java.io.File;

public interface ModuleLoader
{
    ModuleMetadata loadModuleMetadata(File file);
    Module loadModule(ModuleMetadata metadata);
}

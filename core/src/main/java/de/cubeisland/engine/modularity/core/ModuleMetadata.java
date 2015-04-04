package de.cubeisland.engine.modularity.core;

import java.util.Set;

public interface ModuleMetadata
{
    String getIdentifier();
    String getName();
    String getDescription();
    Set<String> getAuthors();

    Set<String> introducedServices();
    Set<String> implementedServices();
    Set<String> requiredServices();
    Set<String> interestingServices();
}

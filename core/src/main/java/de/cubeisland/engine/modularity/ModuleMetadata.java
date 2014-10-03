package de.cubeisland.engine.modularity;

import java.util.Set;

public interface ModuleMetadata
{
    String getIdentifier();
    String getName();
    String getDescrition();
    Set<String> getAuthors();

    Set<String> introducedServices();
    Set<String> implementedServices();
    Set<String> requiredServices();
    Set<String> interestingServices();
}

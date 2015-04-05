package de.cubeisland.engine.modularity.asm;

import de.cubeisland.engine.modularity.asm.marker.Service;

@Service
public interface MuchService
{
    /**
     * Provides a stripped String
     * @return the String
     */
    String provideString();

    /**
     * Strips a String and stores it for later use
     */
    void stripString();
}

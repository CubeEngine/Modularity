package de.cubeisland.engine.modularity.asm;

import java.io.File;
import java.util.Set;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import org.junit.Test;

import static de.cubeisland.engine.modularity.asm.ASMModuleInfoParserTest.getPath;

public class AsmInformationLoaderTest
{

    @Test
    public void testStuff()
    {
        Set<DependencyInformation> infos = new AsmInformationLoader().loadInformation(new File(getPath()));
    }
}

package de.cubeisland.engine.modularity.asm;

public class VeryService implements MuchService
{
    private String stripper = "wow";

    public VeryService(SuchTestingModule module)
    {
    }

    @Override
    public String provideString()
    {
        return stripper;
    }

    @Override
    public void stripString()
    {
        stripper = stripper.trim();
    }
}

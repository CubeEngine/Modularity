/*
 * The MIT License
 * Copyright © 2014 Cube Island
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.cubeisland.engine.modularity.asm.info.module2;

import java.io.File;
import java.io.PrintStream;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.info.module1.BasicService;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Maybe;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Setup;

@ModuleInfo(name = "complex", description = "just testing")
public class ComplexModule extends Module
{
    @Inject private Maybe<BasicService> anOptionalService;
    @Inject public File file;
    @Inject private SelfProvidingService selfProvidedService;

    @Setup
    @Inject
    public void setup0(ComplexService requiredService, PrintStream providedService)
    {
        assert providedService != null;
        assert requiredService != null;
    }

    @Setup(1)
    @Inject
    public void setup1(PrintStream providedService)
    {
        assert providedService != null;
    }

    @Setup(2)
    @Inject
    public void setup2(Maybe<BasicService> optionalService, File file, Maybe<File> optionalFile)
    {
        assert optionalService != null;
    }
}

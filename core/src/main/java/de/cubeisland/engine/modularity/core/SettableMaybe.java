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
package de.cubeisland.engine.modularity.core;

public final class SettableMaybe<T> implements Maybe<T>
{
    private T value;
    private Callback<T> onAvailable;
    private Callback<T> onRemove;

    public SettableMaybe()
    {
        this(null);
    }

    public SettableMaybe(T value)
    {
        this.value = value;
    }

    @Override
    public T value()
    {
        return value;
    }

    @Override
    public boolean isAvailable()
    {
        return value != null;
    }

    void provide(T value)
    {
        this.value = value;
        if (onAvailable != null)
        {
            onAvailable.react(value);
        }
    }

    void remove()
    {
        if (onRemove != null)
        {
            onRemove.react(value);
        }
        value = null;
    }

    @Override
    public void onAvailable(Callback<T> callback)
    {
        this.onAvailable = callback;
    }

    @Override
    public void onRemove(Callback<T> callback)
    {
        // TODO
        this.onRemove = callback;
    }
}

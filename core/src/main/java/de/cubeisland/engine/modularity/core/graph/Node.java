/**
 * The MIT License
 * Copyright (c) 2014 Cube Island
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
package de.cubeisland.engine.modularity.core.graph;

import java.util.HashSet;
import java.util.Set;

/**
 * A dependency node containing a set of DependencyInformation
 */
public class Node
{
    private final Set<Node> successors = new HashSet<Node>();
    private final Set<Node> predecessors = new HashSet<Node>();
    private DependencyInformation information;

    public Node()
    {
    }

    public Node(DependencyInformation information)
    {
        this.information = information;
    }

    public void addSuccessor(Node node)
    {
        detectCircularDepdency(this, node);
        successors.add(node);
        node.predecessors.add(this);
    }

    private void detectCircularDepdency(Node node, Node check)
    {
        if (node.predecessors.contains(check))
        {
            throw new IllegalArgumentException("Circular Dependency! " + node.getInformation().getClassName() + " - " + check.getInformation().getClassName());
        }
        for (Node pre : node.predecessors)
        {
            detectCircularDepdency(pre, check);
        }
    }

    public Set<Node> getSuccessors()
    {
        return successors;
    }

    public Set<Node> getPredecessors()
    {
        return predecessors;
    }

    public DependencyInformation getInformation()
    {
        return information;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Node))
        {
            return false;
        }

        final Node node = (Node)o;

        if (information == node.information)
        {
            return true;
        }
        if (information != null && node.information != null)
        {
            return information.getClassName().equals(node.information.getClassName());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return information != null ? information.getClassName().hashCode() : 0;
    }
}

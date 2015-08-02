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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceDefinitionMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceProviderMetadata;

public class DependencyGraph
{
    private Map<Dependency, List<Node>> unresolved = new HashMap<Dependency, List<Node>>();
    private Map<Dependency, Node> nodes = new HashMap<Dependency, Node>();
    private Node root = new Node();

    public Node addNode(DependencyInformation info)
    {
        Node node = new Node(info);
        // Resolve dependencies of node
        boolean isDependent = false;
        for (Dependency id : info.requiredDependencies())
        {
            isDependent = true;
            resolveDependency(node, id);
        }
        for (Dependency id : info.optionalDependencies())
        {
            isDependent = true;
            resolveDependency(node, id);
        }
        if (info instanceof ModuleMetadata)
        {
            for (Dependency id : ((ModuleMetadata)info).loadAfter())
            {
                isDependent = true;
                resolveDependency(node, id);
            }
        }

        // Resolve dependencies to node
        Dependency found = findVersion(info.getIdentifier(), unresolved.keySet());
        while (found != null)
        {
            List<Node> dependents = unresolved.remove(found);
            if (dependents != null)
            {
                for (Node dependent : dependents)
                {
                    node.addSuccessor(dependent);
                }
            }
            found = findVersion(info.getIdentifier(), unresolved.keySet());
        }

        if (!isDependent)
        {
            root.addSuccessor(node);
        }

        nodes.put(info.getIdentifier(), node);
        return node;
    }

    public void provided(Dependency dep)
    {
        unresolved.remove(dep);
    }

    public static Dependency findVersion(Dependency id, Set<Dependency> in)
    {
        if (in.contains(id))
        {
            return id;
        }
        for (Dependency dependency : in)
        {
            if (dependency.name().equals(id.name()))
            {
                return dependency;
            }
        }
        return null;
    }

    private void resolveDependency(Node node, Dependency id)
    {
        Node dependency = nodes.get(findVersion(id, nodes.keySet()));
        if (dependency == null)
        {
            List<Node> list = unresolved.get(id);
            if (list == null)
            {
                list = new ArrayList<Node>();
                unresolved.put(id, list);
            }
            list.add(node);
        }
        else
        {
            dependency.addSuccessor(node);
        }
    }

    public Node getRoot()
    {
        return root;
    }

    public Map<Dependency, List<Node>> getUnresolved()
    {
        return unresolved;
    }

    public Node getNode(Dependency dep)
    {
        return nodes.get(findVersion(dep, nodes.keySet()));
    }
}

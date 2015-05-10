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

public class DependencyGraph
{
    private Map<String, List<Node>> unresolved = new HashMap<String, List<Node>>();
    private Map<String, Node> nodes = new HashMap<String, Node>();
    private Node root = new Node();

    public void addNode(DependencyInformation info)
    {
        Node node = new Node(info);
        // Resolve dependencies of node
        boolean isDependent = false;
        for (String id : info.requiredDependencies())
        {
            isDependent = true;
            resolveDependency(node, id);
        }
        for (String id : info.optionalDependencies())
        {
            isDependent = true;
            resolveDependency(node, id);
        }
        if (info instanceof ModuleMetadata)
        {
            for (String id : ((ModuleMetadata)info).loadAfter())
            {
                isDependent = true;
                resolveDependency(node, id);
            }
        }

        // Resolve dependencies to node

        String found = findVersion(info.getIdentifier(), unresolved.keySet());;
        while (found != null)
        {
            List<Node> dependents = unresolved.remove(found);
            if (dependents != null)
            {
                for (Node dependent : dependents)
                {
                    node.addChild(dependent);
                }
            }
            found = findVersion(info.getIdentifier(), unresolved.keySet());;
        }

        if (!isDependent)
        {
            root.addChild(node);
        }

        if (node.getInformation() instanceof ModuleMetadata)
        {
            nodes.put(node.getInformation().getClassName(), node);
        }
        else
        {
            nodes.put(node.getInformation().getIdentifier(), node);
        }
    }

    public static String findVersion(String id, Set<String> in)
    {
        if (in.contains(id))
        {
            return id;
        }
        if (!id.contains(":"))
        {
            for (String dep : in)
            {
                if (dep.contains(":"))
                {
                    if (dep.split(":")[0].equals(id))
                    {
                        return dep;
                    }
                }
            }
        }
        else
        {
            String substring = id.substring(0, id.indexOf(":"));
            if (in.contains(substring))
            {
                return substring;
            }
        }
        return null;
    }

    private void resolveDependency(Node node, String id)
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
            dependency.addChild(node);
        }
    }

    public Node getRoot()
    {
        return root;
    }

    public Map<String, List<Node>> getUnresolved()
    {
        return unresolved;
    }
}

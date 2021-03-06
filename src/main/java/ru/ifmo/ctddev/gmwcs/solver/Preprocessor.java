package ru.ifmo.ctddev.gmwcs.solver;

import ru.ifmo.ctddev.gmwcs.Signals;
import ru.ifmo.ctddev.gmwcs.graph.Edge;
import ru.ifmo.ctddev.gmwcs.graph.Graph;
import ru.ifmo.ctddev.gmwcs.graph.Node;
import ru.ifmo.ctddev.gmwcs.graph.Unit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Preprocessor {
    public static void preprocess(Graph graph, Signals signals) {
        Node primaryNode = null;
        for (Edge edge : new ArrayList<>(graph.edgeSet())) {
            if (!graph.containsEdge(edge)) {
                continue;
            }
            Node from = graph.getEdgeSource(edge);
            Node to = graph.getEdgeTarget(edge);
            if (edge.getWeight() >= 0 && from.getWeight() >= 0 && to.getWeight() >= 0) {
                merge(graph, signals, edge, from, to);
            }
        }
        for (Node v : new ArrayList<>(graph.vertexSet())) {
            if (v.getWeight() > 0) {
                primaryNode = v;
            }
            if (v.getWeight() <= 0 && graph.degreeOf(v) == 2) {
                Edge[] edges = graph.edgesOf(v).stream().toArray(Edge[]::new);
                if (edges[1].getWeight() > 0 || edges[0].getWeight() > 0) {
                    continue;
                }
                Node left = graph.getOppositeVertex(v, edges[0]);
                Node right = graph.getOppositeVertex(v, edges[1]);
                if (left == right) {
                    graph.removeVertex(v);
                } else {
                    graph.removeVertex(v);
                    absorb(signals, edges[0], v);
                    absorb(signals, edges[0], edges[1]);
                    graph.addEdge(left, right, edges[0]);
                }
            }
        }
        if (primaryNode != null) {
            Set<Node> toRemove = new HashSet<>();
            negR(graph, primaryNode, primaryNode, new HashSet<>(), toRemove);
            toRemove.forEach(graph::removeVertex);
        }
    }

    private static boolean negR(Graph g, Node v, Node r, Set<Node> vis, Set<Node> toRemove) {
        boolean safe = false;
        vis.add(v);
        for (Edge e : g.edgesOf(v)) {
            Node u = g.getOppositeVertex(v, e);
            if (vis.contains(u)) {
                if (u != r && !toRemove.contains(u)) {
                    safe = true;
                }
                continue;
            }
            boolean res = negR(g, u, v, vis, toRemove);
            if (u.getWeight() > 0) {
                res = true;
            } else {
                for (Edge edge : g.getAllEdges(v, u)) {
                    if (edge.getWeight() > 0) {
                        res = true;
                    }
                }
            }
            if (!res) {
                toRemove.add(u);
            }
            safe = res || safe;
        }
        return safe;
    }

    private static void merge(Graph graph, Signals ss, Unit... units) {
        Set<Node> nodes = new HashSet<>();
        Set<Edge> edges = new HashSet<>();
        for (Unit unit : units) {
            if (unit instanceof Node) {
                nodes.add((Node) unit);
            } else {
                edges.add((Edge) unit);
            }
        }
        for (Edge e : edges) {
            if (!nodes.contains(graph.getEdgeSource(e)) || !nodes.contains(graph.getEdgeTarget(e))) {
                throw new IllegalArgumentException();
            }
        }
        for (Edge e : edges) {
            contract(graph, ss, e);
        }
    }

    private static void contract(Graph graph, Signals ss, Edge e) {
        Node main = graph.getEdgeSource(e);
        Node aux = graph.getEdgeTarget(e);
        Set<Edge> auxEdges = new HashSet<>(graph.edgesOf(aux));
        auxEdges.remove(e);
        for (Edge a : auxEdges) {
            Node opposite = graph.getOppositeVertex(aux, a);
            Edge m = graph.getEdge(main, opposite);
            graph.removeEdge(a);
            if (m == null) {
                if (opposite == main) {
                    if (a.getWeight() >= 0) {
                        absorb(ss, main, a);
                    }
                    continue;
                }
                graph.addEdge(main, opposite, a);
            } else {
                if (a.getWeight() >= 0 && m.getWeight() >= 0) {
                    absorb(ss, m, a);
                } else {
                    graph.addEdge(main, opposite, a);
                }
            }
        }
        graph.removeVertex(aux);
        absorb(ss, main, aux);
        absorb(ss, main, e);
    }

    private static void absorb(Signals ss, Unit who, Unit whom) {
        who.absorb(whom);
        ss.join(whom, who);
    }
}

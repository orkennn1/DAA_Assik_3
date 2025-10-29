package com.assignment3;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Assignment3Prim {

    static class InputRoot { List<GraphInput> graphs; }
    static class GraphInput {
        int id;
        List<String> nodes;
        List<EdgeInput> edges;
    }
    static class EdgeInput {
        String from;
        String to;
        int weight;
    }

    static class OutputRoot {
        List<GraphResult> results = new ArrayList<>();
    }
    static class GraphResult {
        int graph_id;
        InputStats input_stats;
        AlgorithmResult prim;
        // kruskal оставим null/не используем в этой части
    }
    static class InputStats {
        int vertices;
        int edges;
        InputStats(int v, int e) { vertices = v; edges = e; }
    }
    static class AlgorithmResult {
        List<EdgeInput> mst_edges = new ArrayList<>();
        int total_cost;
        long operations_count;
        double execution_time_ms;
    }

    static class Edge {
        String u, v; int w;
        Edge(String u, String v, int w) { this.u = u; this.v = v; this.w = w; }
    }

    static AlgorithmResult runPrim(GraphInput g) {
        AlgorithmResult res = new AlgorithmResult();
        long ops = 0;
        long t0 = System.nanoTime();

        Map<String, List<Edge>> adj = new HashMap<>();
        for (String node : g.nodes) adj.put(node, new ArrayList<>());
        for (EdgeInput e : g.edges) {

            if (!adj.containsKey(e.from) || !adj.containsKey(e.to)) {

                adj.putIfAbsent(e.from, new ArrayList<>());
                adj.putIfAbsent(e.to, new ArrayList<>());
            }
            adj.get(e.from).add(new Edge(e.from, e.to, e.weight));
            adj.get(e.to).add(new Edge(e.to, e.from, e.weight));
            ops += 2;
        }

        if (g.nodes == null || g.nodes.isEmpty()) {
            long t1 = System.nanoTime();
            res.execution_time_ms = (t1 - t0) / 1_000_000.0;
            res.operations_count = ops;
            return res;
        }

        String start = g.nodes.get(0);
        Set<String> inMST = new HashSet<>();

        class PQNode { int w; String from, to; PQNode(int w, String from, String to) { this.w = w; this.from = from; this.to = to; } }
        PriorityQueue<PQNode> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.w));


        inMST.add(start); ops++;


        List<Edge> startEdges = adj.getOrDefault(start, Collections.emptyList());
        for (Edge e : startEdges) {
            pq.add(new PQNode(e.w, e.u, e.v)); ops++;
        }


        while (!pq.isEmpty() && inMST.size() < g.nodes.size()) {
            PQNode p = pq.poll(); ops++;
            if (inMST.contains(p.to)) { ops++; continue; }


            EdgeInput chosen = new EdgeInput();
            chosen.from = p.from;
            chosen.to = p.to;
            chosen.weight = p.w;
            res.mst_edges.add(chosen);
            res.total_cost += p.w; ops++;


            inMST.add(p.to); ops++;
            List<Edge> neighbours = adj.getOrDefault(p.to, Collections.emptyList());
            for (Edge e : neighbours) {
                if (!inMST.contains(e.v)) {
                    pq.add(new PQNode(e.w, e.u, e.v)); ops++;
                }
                ops++;
            }
        }

        long t1 = System.nanoTime();
        res.execution_time_ms = (t1 - t0) / 1_000_000.0;
        res.operations_count = ops;
        return res;
    }

    public static void main(String[] args) {
        String inputFile = "input_example.json";
        String outputFile = "output_prim.json";

        try {
            String json = new String(Files.readAllBytes(Paths.get(inputFile)));
            Gson gson = new Gson();
            InputRoot root = gson.fromJson(json, InputRoot.class);

            OutputRoot outRoot = new OutputRoot();

            if (root != null && root.graphs != null) {
                for (GraphInput g : root.graphs) {
                    GraphResult gr = new GraphResult();
                    gr.graph_id = g.id;
                    gr.input_stats = new InputStats(g.nodes == null ? 0 : g.nodes.size(),
                            g.edges == null ? 0 : g.edges.size());

                    AlgorithmResult primRes = runPrim(g);
                    gr.prim = primRes;
                    outRoot.results.add(gr);
                }
            }

            Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
            String outJson = gsonPretty.toJson(outRoot);
            Files.write(Paths.get(outputFile), outJson.getBytes());
            System.out.println("Prim results written to " + outputFile);

        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (JsonSyntaxException jse) {
            System.err.println("JSON parse error: " + jse.getMessage());
        } catch (Exception ex) {
            System.err.println("Unexpected error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

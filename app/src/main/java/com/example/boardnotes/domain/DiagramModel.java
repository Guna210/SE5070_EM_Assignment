package com.example.boardnotes.domain;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DiagramModel {
    public static final class Node {
        public final String id;
        public final String label;
        public final int row;
        public final int col;

        Node(String id, String label, int row, int col) {
            this.id = id;
            this.label = label;
            this.row = row;
            this.col = col;
        }
    }

    public static final class Edge {
        public final String from;
        public final String to;
        public final String label;

        Edge(String from, String to, String label) {
            this.from = from;
            this.to = to;
            this.label = label;
        }
    }

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();

    public void addNode(String id, String label, int row, int col) {
        if (id == null || id.isEmpty() || nodes.containsKey(id)) {
            return;
        }
        nodes.put(id, new Node(id, label == null ? "" : label, row, col));
    }

    public boolean addEdge(String from, String to, String label) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to)) {
            return false;
        }
        edges.add(new Edge(from, to, label == null ? "" : label));
        return true;
    }

    public List<Node> nodes() {
        return new ArrayList<>(nodes.values());
    }

    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    @Nullable
    public static DiagramModel fromJson(@Nullable String json) {
        if (json == null) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(json);
            DiagramModel model = new DiagramModel();

            JSONArray nodeArray = root.optJSONArray("nodes");
            if (nodeArray != null) {
                for (int i = 0; i < nodeArray.length(); i++) {
                    JSONObject node = nodeArray.optJSONObject(i);
                    if (node != null) {
                        model.addNode(node.optString("id"), node.optString("label"),
                                node.optInt("row", -1), node.optInt("col", -1));
                    }
                }
            }

            // Nodes first: an edge is only accepted if both of its ends already exist.
            JSONArray edgeArray = root.optJSONArray("edges");
            if (edgeArray != null) {
                for (int i = 0; i < edgeArray.length(); i++) {
                    JSONObject edge = edgeArray.optJSONObject(i);
                    if (edge != null) {
                        model.addEdge(edge.optString("from"), edge.optString("to"),
                                edge.optString("label"));
                    }
                }
            }
            return model;

        } catch (JSONException e) {
            return null;
        }
    }
}

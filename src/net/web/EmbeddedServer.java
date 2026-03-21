package net.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import utils.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;

public class EmbeddedServer {

    private static final int    PORT       = 80;
    private static final int    MAX_SCORES = 20;
    private static final String DATA_FILE  = "scores.json";

    private HttpServer server;

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", this::handle);
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            Logger.log(this, "Embedded server started on http://localhost:" + PORT);
        } catch (IOException e) {
            Logger.log(this, "Could not start embedded server: " + e.getMessage());
            Logger.log(this, "Try running as administrator or change PORT in EmbeddedServer.java");
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equalsIgnoreCase(method))       handleGet(exchange);
            else if ("POST".equalsIgnoreCase(method)) handlePost(exchange);
            else                                       sendText(exchange, 405, "Method Not Allowed");
        } catch (Exception e) {
            Logger.log(this, "Server error: " + e.getMessage());
            sendText(exchange, 500, "Internal Server Error");
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        List<ScoreEntry> entries = loadScores();
        entries.sort((a, b) -> Integer.compare(b.points, a.points));
        if (entries.size() > MAX_SCORES) entries = entries.subList(0, MAX_SCORES);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version='1.0' encoding='UTF-8'?>");
        xml.append("<leaderboard>");
        for (ScoreEntry e : entries) {
            xml.append("<score>");
            xml.append("<username>").append(escape(e.username)).append("</username>");
            xml.append("<points>").append(e.points).append("</points>");
            xml.append("<timestamp>").append(escape(e.timestamp)).append("</timestamp>");
            xml.append("</score>");
        }
        xml.append("</leaderboard>");

        byte[] bytes = xml.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/xml; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            String username  = extractTag(body, "username");
            String pointsStr = extractTag(body, "points");
            String timestamp = extractTag(body, "timestamp");

            if (username == null || pointsStr == null || timestamp == null)
                throw new IllegalArgumentException("Missing XML fields");

            int points = Integer.parseInt(pointsStr.trim());
            if (username.isEmpty() || username.length() > 16)
                throw new IllegalArgumentException("Invalid username length");

            ScoreEntry entry = new ScoreEntry(username, points, timestamp);
            List<ScoreEntry> entries = loadScores();
            entries.add(entry);
            saveScores(entries);
            sendText(exchange, 200, "Success");
        } catch (Exception e) {
            Logger.log(this, "POST error: " + e.getMessage());
            sendText(exchange, 200, "Error");
        }
    }

    private List<ScoreEntry> loadScores() {
        List<ScoreEntry> list = new ArrayList<>();
        File f = new File(DATA_FILE);
        if (!f.exists()) return list;
        try {
            String raw = Files.readString(f.toPath(), StandardCharsets.UTF_8).trim();
            raw = raw.replaceAll("^\\[|]$", "").trim();
            if (raw.isEmpty()) return list;
            for (String obj : splitJsonObjects(raw)) {
                String u = jsonField(obj, "username");
                String p = jsonField(obj, "points");
                String t = jsonField(obj, "timestamp");
                if (u != null && p != null && t != null)
                    list.add(new ScoreEntry(u, Integer.parseInt(p.trim()), t));
            }
        } catch (Exception e) {
            Logger.log(this, "Could not load scores: " + e.getMessage());
        }
        return list;
    }

    private void saveScores(List<ScoreEntry> entries) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < entries.size(); i++) {
            ScoreEntry e = entries.get(i);
            sb.append("  {")
              .append("\"username\":\"").append(e.username.replace("\"","\\\"")).append("\",")
              .append("\"points\":").append(e.points).append(",")
              .append("\"timestamp\":\"").append(e.timestamp).append("\"")
              .append("}");
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        try {
            Files.writeString(Path.of(DATA_FILE), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.log(this, "Could not save scores: " + e.getMessage());
        }
    }

    private void sendText(HttpExchange ex, int code, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private String extractTag(String xml, String tag) {
        int start = xml.indexOf("<" + tag + ">");
        int end   = xml.indexOf("</" + tag + ">");
        if (start == -1 || end == -1) return null;
        return xml.substring(start + tag.length() + 2, end);
    }

    private List<String> splitJsonObjects(String raw) {
        List<String> objs = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '{') { if (depth++ == 0) start = i; }
            else if (c == '}') { if (--depth == 0 && start != -1) objs.add(raw.substring(start, i + 1)); }
        }
        return objs;
    }

    private String jsonField(String obj, String key) {
        String strPat = "\"" + key + "\":\"";
        int si = obj.indexOf(strPat);
        if (si != -1) {
            si += strPat.length();
            int ei = obj.indexOf("\"", si);
            return ei != -1 ? obj.substring(si, ei) : null;
        }
        String numPat = "\"" + key + "\":";
        si = obj.indexOf(numPat);
        if (si != -1) {
            si += numPat.length();
            int ei = si;
            while (ei < obj.length() && (Character.isDigit(obj.charAt(ei)) || obj.charAt(ei) == '-')) ei++;
            return obj.substring(si, ei);
        }
        return null;
    }

    private String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static class ScoreEntry {
        final String username;
        final int    points;
        final String timestamp;
        ScoreEntry(String username, int points, String timestamp) {
            this.username  = username;
            this.points    = points;
            this.timestamp = timestamp;
        }
    }
}

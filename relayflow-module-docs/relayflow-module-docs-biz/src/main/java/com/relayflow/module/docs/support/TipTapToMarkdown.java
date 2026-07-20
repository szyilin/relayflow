package com.relayflow.module.docs.support;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;

/**
 * Server-side TipTap JSON → Markdown (aligned with web tipTapToMarkdown.ts).
 */
public final class TipTapToMarkdown {

    private TipTapToMarkdown() {
    }

    public static String convert(JsonNode doc, String title) {
        StringBuilder body = new StringBuilder();
        JsonNode content = doc.path("content");
        if (content.isArray()) {
            for (JsonNode node : content) {
                body.append(blockToMd(node, 0));
            }
        }
        String trimmed = body.toString().stripTrailing();
        if (title != null && !title.isBlank()) {
            return "# " + title.trim() + "\n\n" + trimmed + "\n";
        }
        return trimmed + "\n";
    }

    private static String blockToMd(JsonNode node, int depth) {
        if (node == null || node.isMissingNode()) {
            return "";
        }
        String type = node.path("type").asText("");
        String indent = "  ".repeat(Math.max(0, depth));
        return switch (type) {
            case "heading" -> {
                int level = Math.min(3, Math.max(1, node.path("attrs").path("level").asInt(1)));
                yield "#".repeat(level) + " " + inlineText(node.path("content")) + "\n\n";
            }
            case "paragraph" -> inlineText(node.path("content")) + "\n\n";
            case "bulletList" -> {
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : node.path("content")) {
                    sb.append(blockToMd(item, depth));
                }
                yield sb.toString();
            }
            case "orderedList" -> {
                StringBuilder sb = new StringBuilder();
                int index = 1;
                for (JsonNode item : node.path("content")) {
                    String text = inlineText(firstBlockContent(item));
                    sb.append(indent).append(index++).append(". ").append(text).append('\n');
                }
                yield sb.append('\n').toString();
            }
            case "listItem" -> {
                String text = inlineText(firstBlockContent(node));
                yield indent + "- " + text + '\n';
            }
            case "taskList" -> {
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : node.path("content")) {
                    sb.append(blockToMd(item, depth));
                }
                yield sb.append('\n').toString();
            }
            case "taskItem" -> {
                boolean checked = node.path("attrs").path("checked").asBoolean(false);
                String text = inlineText(firstBlockContent(node));
                yield indent + "- [" + (checked ? 'x' : ' ') + "] " + text + '\n';
            }
            case "blockquote" -> {
                StringBuilder sb = new StringBuilder();
                for (JsonNode child : node.path("content")) {
                    String block = blockToMd(child, depth).stripTrailing();
                    for (String line : block.split("\n")) {
                        sb.append("> ").append(line).append('\n');
                    }
                }
                yield sb.append('\n').toString();
            }
            case "codeBlock" -> "```\n" + inlineText(node.path("content")) + "\n```\n\n";
            case "horizontalRule" -> "---\n\n";
            default -> {
                StringBuilder sb = new StringBuilder();
                for (JsonNode child : node.path("content")) {
                    sb.append(blockToMd(child, depth));
                }
                yield sb.toString();
            }
        };
    }

    private static JsonNode firstBlockContent(JsonNode node) {
        JsonNode content = node.path("content");
        if (content.isArray() && !content.isEmpty()) {
            return content.get(0).path("content");
        }
        return content;
    }

    private static String inlineText(JsonNode nodes) {
        if (nodes == null || !nodes.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode node : nodes) {
            sb.append(inlineNode(node));
        }
        return sb.toString();
    }

    private static String inlineNode(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return "";
        }
        String type = node.path("type").asText("");
        if ("text".equals(type)) {
            return marksToMd(node.path("text").asText(""), node.path("marks"));
        }
        if ("hardBreak".equals(type)) {
            return "  \n";
        }
        return inlineText(node.path("content"));
    }

    private static String marksToMd(String text, JsonNode marks) {
        if (marks == null || !marks.isArray() || marks.isEmpty()) {
            return text;
        }
        String out = text;
        for (JsonNode mark : marks) {
            String markType = mark.path("type").asText("");
            out = switch (markType) {
                case "bold" -> "**" + out + "**";
                case "italic" -> "*" + out + "*";
                case "code" -> "`" + out + "`";
                case "link" -> {
                    String href = mark.path("attrs").path("href").asText("");
                    yield "[" + out + "](" + href + ")";
                }
                default -> out;
            };
        }
        return out;
    }
}

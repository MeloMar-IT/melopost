package com.melomarit.melopost.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allow-list HTML sanitizer for rich text (hole descriptions, remedial actions) that
 * is rendered unescaped. It is applied when a page is rendered, so stored data stays
 * unchanged.
 *
 * <p>The output is always rebuilt from scratch: only allow-listed elements are emitted,
 * without attributes (except a validated {@code href} and {@code target="_blank"} on
 * links, which also get {@code rel="noopener noreferrer"}). Everything else is dropped,
 * text is escaped, {@code script}/{@code style} contents are removed.
 */
public final class RichTextSanitizer {

    private static final Set<String> ALLOWED = Set.of(
            "p", "br", "div", "span", "strong", "b", "em", "i", "u", "s", "strike", "del",
            "ul", "ol", "li", "blockquote", "pre", "code", "hr",
            "h1", "h2", "h3", "h4", "h5", "h6", "a");
    private static final Set<String> VOID = Set.of("br", "hr");
    private static final Set<String> DROP_WITH_CONTENT = Set.of(
            "script", "style", "iframe", "object", "embed", "noscript", "template", "textarea", "title");

    // Attributes are matched as a plain character class (no alternation inside a repeated group):
    // Java's regex engine recurses per iteration of such a group and overflowed the stack on long attributes.
    // A '>' inside a quoted attribute value ends the tag early; the href is then only kept if it still parses.
    private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)([^<>]*)>");
    private static final Pattern ENTITY = Pattern.compile("&(?:#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z][a-zA-Z0-9]{1,31});");
    private static final Pattern HREF = Pattern.compile(
            "(?:^|[\\s/\"'])href\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))", Pattern.CASE_INSENSITIVE);
    private static final Pattern TARGET_BLANK = Pattern.compile(
            "(?:^|[\\s/\"'])target\\s*=\\s*(?:\"_blank\"|'_blank'|_blank(?=[\\s/>]|$))", Pattern.CASE_INSENSITIVE);

    private RichTextSanitizer() {
    }

    /** Returns allow-listed HTML; null or blank input gives "". */
    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder(html.length() + 16);
        Deque<String> open = new ArrayDeque<>();
        int pos = 0;
        int len = html.length();
        while (pos < len) {
            int lt = html.indexOf('<', pos);
            if (lt < 0) {
                appendText(out, html, pos, len);
                break;
            }
            appendText(out, html, pos, lt);
            if (html.startsWith("<!--", lt)) {
                int end = html.indexOf("-->", lt + 4);
                pos = end < 0 ? len : end + 3;
                continue;
            }
            Matcher m = TAG.matcher(html);
            m.region(lt, len);
            if (!m.lookingAt()) {
                if (lt + 1 < len && (html.charAt(lt + 1) == '!' || html.charAt(lt + 1) == '?')) {
                    int end = html.indexOf('>', lt);
                    pos = end < 0 ? len : end + 1;
                } else {
                    out.append("&lt;");
                    pos = lt + 1;
                }
                continue;
            }
            pos = m.end();
            boolean closing = !m.group(1).isEmpty();
            String name = m.group(2).toLowerCase(Locale.ROOT);
            String attrs = m.group(3);
            if (DROP_WITH_CONTENT.contains(name)) {
                if (!closing) {
                    pos = skipRawContent(html, pos, name);
                }
                continue;
            }
            if (!ALLOWED.contains(name)) {
                continue;
            }
            if (closing) {
                if (!VOID.contains(name) && open.contains(name)) {
                    while (!open.isEmpty()) {
                        String top = open.pop();
                        out.append("</").append(top).append('>');
                        if (top.equals(name)) {
                            break;
                        }
                    }
                }
                continue;
            }
            out.append('<').append(name);
            if (name.equals("a")) {
                appendLinkAttributes(out, attrs);
            }
            out.append('>');
            if (!VOID.contains(name)) {
                open.push(name);
            }
        }
        while (!open.isEmpty()) {
            out.append("</").append(open.pop()).append('>');
        }
        return out.toString();
    }

    private static int skipRawContent(String html, int from, String name) {
        Matcher close = Pattern.compile("</" + name + "[\\s/>]", Pattern.CASE_INSENSITIVE).matcher(html);
        if (!close.find(from)) {
            return html.length();
        }
        int end = html.indexOf('>', close.start());
        return end < 0 ? html.length() : end + 1;
    }

    private static void appendLinkAttributes(StringBuilder out, String attrs) {
        Matcher h = HREF.matcher(attrs);
        if (h.find()) {
            String raw = h.group(1) != null ? h.group(1) : h.group(2) != null ? h.group(2) : h.group(3);
            String href = decodeBasicEntities(raw).strip();
            if (isSafeUrl(href)) {
                out.append(" href=\"").append(escapeAttribute(href)).append('"');
                if (TARGET_BLANK.matcher(attrs).find()) {
                    out.append(" target=\"_blank\"");
                }
                out.append(" rel=\"noopener noreferrer\"");
            }
        }
    }

    private static boolean isSafeUrl(String url) {
        if (url.isEmpty()) {
            return false;
        }
        for (int i = 0; i < url.length(); i++) {
            if (Character.isISOControl(url.charAt(i))) {
                return false;
            }
        }
        int colon = url.indexOf(':');
        int firstDelimiter = -1;
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                firstDelimiter = i;
                break;
            }
        }
        if (colon < 0 || (firstDelimiter >= 0 && firstDelimiter < colon)) {
            return true; // relative URL
        }
        String scheme = url.substring(0, colon).toLowerCase(Locale.ROOT);
        return scheme.equals("http") || scheme.equals("https") || scheme.equals("mailto");
    }

    /** Decodes the entities an attacker could use to hide a scheme (e.g. {@code &#106;avascript:}). */
    private static String decodeBasicEntities(String s) {
        Matcher m = Pattern.compile("&(?:#([0-9]{1,7})|#[xX]([0-9a-fA-F]{1,6})|(colon|tab|newline|amp|lt|gt|quot|apos));?",
                Pattern.CASE_INSENSITIVE).matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String rep;
            if (m.group(1) != null) {
                rep = codePoint(Integer.parseInt(m.group(1)));
            } else if (m.group(2) != null) {
                rep = codePoint(Integer.parseInt(m.group(2), 16));
            } else {
                rep = switch (m.group(3).toLowerCase(Locale.ROOT)) {
                    case "colon" -> ":";
                    case "tab" -> "\t";
                    case "newline" -> "\n";
                    case "amp" -> "&";
                    case "lt" -> "<";
                    case "gt" -> ">";
                    case "quot" -> "\"";
                    default -> "'";
                };
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String codePoint(int cp) {
        return Character.isValidCodePoint(cp) ? new String(Character.toChars(cp)) : "\uFFFD";
    }

    /** Escapes text; existing well-formed entities are kept. */
    private static void appendText(StringBuilder out, String s, int from, int to) {
        for (int i = from; i < to; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '&' -> {
                    Matcher m = ENTITY.matcher(s);
                    m.region(i, to);
                    if (m.lookingAt()) {
                        out.append(s, i, m.end());
                        i = m.end() - 1;
                    } else {
                        out.append("&amp;");
                    }
                }
                default -> out.append(c);
            }
        }
    }

    private static String escapeAttribute(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}

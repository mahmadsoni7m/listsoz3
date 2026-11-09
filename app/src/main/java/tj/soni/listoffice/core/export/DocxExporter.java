package tj.soni.listoffice.core.export;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * DOCX-и воқеӣ (OOXML) бе китобхонаи берунӣ.
 * '\f' (U+000C) = канори саҳифа. Сатрҳои фақат бо ҳарфи калон = сарлавҳа (марказ, ғафс).
 * Дар поён рақами саҳифа гузошта мешавад.
 */
public final class DocxExporter {
    private static final String NS_W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    private DocxExporter() {}

    public static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '&') sb.append("&amp;");
            else if (ch == '<') sb.append("&lt;");
            else if (ch == '>') sb.append("&gt;");
            else if (ch == '"') sb.append("&quot;");
            else if (ch < 0x20 && ch != '\t') { /* хориҷ */ }
            else sb.append(ch);
        }
        return sb.toString();
    }

    static boolean isHeading(String line) {
        String t = line.trim();
        if (t.isEmpty() || t.length() > 60) return false;
        boolean hasLetter = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
                if (Character.isLowerCase(c)) return false;
            }
        }
        return hasLetter;
    }

    public static String documentXml(String text, int fontSizePt) {
        int half = fontSizePt * 2;
        String fonts = "<w:rFonts w:ascii=\"Times New Roman\" w:hAnsi=\"Times New Roman\" w:cs=\"Times New Roman\"/>";
        String size = "<w:sz w:val=\"" + half + "\"/><w:szCs w:val=\"" + half + "\"/>";
        StringBuilder body = new StringBuilder();
        String[] pages = text.replace("\r", "").split("\f", -1);
        for (int p = 0; p < pages.length; p++) {
            if (p > 0) body.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>");
            String[] lines = pages[p].split("\n", -1);
            for (String line : lines) {
                boolean head = isHeading(line);
                body.append("<w:p><w:pPr>");
                body.append("<w:spacing w:after=\"0\" w:line=\"360\" w:lineRule=\"auto\"/>");
                if (head) body.append("<w:jc w:val=\"center\"/>");
                else if (!line.trim().isEmpty()) body.append("<w:ind w:firstLine=\"567\"/><w:jc w:val=\"both\"/>");
                body.append("</w:pPr>");
                if (!line.isEmpty()) {
                    body.append("<w:r><w:rPr>").append(fonts);
                    if (head) body.append("<w:b/><w:bCs/>");
                    body.append(size).append("</w:rPr><w:t xml:space=\"preserve\">")
                        .append(escape(line)).append("</w:t></w:r>");
                }
                body.append("</w:p>");
            }
        }
        return XML + "<w:document xmlns:w=\"" + NS_W + "\" xmlns:r=\"" + NS_R + "\"><w:body>"
            + body
            + "<w:sectPr><w:footerReference w:type=\"default\" r:id=\"rId1\"/>"
            + "<w:pgSz w:w=\"11906\" w:h=\"16838\"/>"
            + "<w:pgMar w:top=\"1134\" w:right=\"850\" w:bottom=\"1134\" w:left=\"1701\" w:header=\"708\" w:footer=\"567\" w:gutter=\"0\"/>"
            + "</w:sectPr></w:body></w:document>";
    }

    private static String footerXml() {
        return XML + "<w:ftr xmlns:w=\"" + NS_W + "\" xmlns:r=\"" + NS_R + "\"><w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr>"
            + "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r>"
            + "<w:r><w:instrText xml:space=\"preserve\"> PAGE </w:instrText></w:r>"
            + "<w:r><w:fldChar w:fldCharType=\"separate\"/></w:r>"
            + "<w:r><w:t>1</w:t></w:r>"
            + "<w:r><w:fldChar w:fldCharType=\"end\"/></w:r></w:p></w:ftr>";
    }

    public static void write(OutputStream out, String text, int fontSizePt) throws IOException {
        ZipOutputStream z = new ZipOutputStream(out);
        put(z, "[Content_Types].xml", XML
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
            + "<Override PartName=\"/word/footer1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml\"/>"
            + "</Types>");
        put(z, "_rels/.rels", XML
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"" + NS_R + "/officeDocument\" Target=\"word/document.xml\"/>"
            + "</Relationships>");
        put(z, "word/_rels/document.xml.rels", XML
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"" + NS_R + "/footer\" Target=\"footer1.xml\"/>"
            + "</Relationships>");
        put(z, "word/document.xml", documentXml(text, fontSizePt));
        put(z, "word/footer1.xml", footerXml());
        z.finish();
        z.flush();
    }

    private static void put(ZipOutputStream z, String name, String content) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(content.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }
}

package tj.soni.listoffice.core.export;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * PPTX-и воқеӣ (PresentationML, 16:9, 960x540 pt) бе китобхонаи берунӣ.
 * Тарҳбандиҳо: TITLE, TEXT, IMG_RIGHT, IMG_LEFT, FULL. Сурат (JPEG) дар слайд ҷойгир мешавад.
 * Рангҳо — ҳекс бе '#', масалан "0F3D26".
 */
public final class PptxExporter {
    private static final String A = "http://schemas.openxmlformats.org/drawingml/2006/main";
    private static final String R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String P = "http://schemas.openxmlformats.org/presentationml/2006/main";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    private static final String NS = "xmlns:a=\"" + A + "\" xmlns:r=\"" + R + "\" xmlns:p=\"" + P + "\"";
    private static final String GRP = "<p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>"
        + "<p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>";
    private static final long PT = 12700L;

    /** Маълумоти як слайд. [image] = байтҳои JPEG ё null. */
    public static final class SlideData {
        public final String title;
        public final String body;
        public final String layout;
        public final byte[] image;
        public final int imageW;
        public final int imageH;

        public SlideData(String title, String body, String layout, byte[] image, int imageW, int imageH) {
            this.title = title == null ? "" : title;
            this.body = body == null ? "" : body;
            this.layout = layout == null ? "TEXT" : layout;
            this.image = image;
            this.imageW = imageW;
            this.imageH = imageH;
        }

        boolean usesImage() {
            return image != null && image.length > 0
                && ("TITLE".equals(layout) || "FULL".equals(layout) || "IMG_RIGHT".equals(layout) || "IMG_LEFT".equals(layout));
        }
    }

    private PptxExporter() {}

    private static String esc(String s) {
        return DocxExporter.escape(s);
    }

    private static long e(double pt) {
        return Math.round(pt * PT);
    }

    private static String run(String text, int sz, boolean bold, String color) {
        return "<a:r><a:rPr lang=\"ru-RU\" sz=\"" + sz + "\"" + (bold ? " b=\"1\"" : "") + " dirty=\"0\">"
            + "<a:solidFill><a:srgbClr val=\"" + color + "\"/></a:solidFill>"
            + "<a:latin typeface=\"Calibri\"/><a:cs typeface=\"Calibri\"/></a:rPr><a:t>" + esc(text) + "</a:t></a:r>";
    }

    private static String textShape(int id, String name, long x, long y, long cx, long cy, String anchor, String paragraphs) {
        return "<p:sp><p:nvSpPr><p:cNvPr id=\"" + id + "\" name=\"" + name + "\"/><p:cNvSpPr txBox=\"1\"/><p:nvPr/></p:nvSpPr>"
            + "<p:spPr><a:xfrm><a:off x=\"" + x + "\" y=\"" + y + "\"/><a:ext cx=\"" + cx + "\" cy=\"" + cy + "\"/></a:xfrm>"
            + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom><a:noFill/></p:spPr>"
            + "<p:txBody><a:bodyPr wrap=\"square\" lIns=\"0\" tIns=\"0\" rIns=\"0\" bIns=\"0\" anchor=\"" + anchor + "\"><a:normAutofit/></a:bodyPr><a:lstStyle/>"
            + paragraphs + "</p:txBody></p:sp>";
    }

    private static String rect(int id, String name, long x, long y, long cx, long cy, String color, int alphaPermille100) {
        String fill = alphaPermille100 >= 100000
            ? "<a:srgbClr val=\"" + color + "\"/>"
            : "<a:srgbClr val=\"" + color + "\"><a:alpha val=\"" + alphaPermille100 + "\"/></a:srgbClr>";
        return "<p:sp><p:nvSpPr><p:cNvPr id=\"" + id + "\" name=\"" + name + "\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>"
            + "<p:spPr><a:xfrm><a:off x=\"" + x + "\" y=\"" + y + "\"/><a:ext cx=\"" + cx + "\" cy=\"" + cy + "\"/></a:xfrm>"
            + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom><a:solidFill>" + fill + "</a:solidFill>"
            + "<a:ln><a:noFill/></a:ln></p:spPr></p:sp>";
    }

    private static String pic(int id, long x, long y, long cx, long cy, int w, int h, boolean round) {
        int l = 0, r = 0, t = 0, b = 0;
        if (w > 0 && h > 0 && cx > 0 && cy > 0) {
            double da = (double) cx / cy;
            double ia = (double) w / h;
            if (ia > da) {
                int c = (int) Math.round((1 - da / ia) / 2 * 100000);
                l = c; r = c;
            } else {
                int c = (int) Math.round((1 - ia / da) / 2 * 100000);
                t = c; b = c;
            }
        }
        return "<p:pic><p:nvPicPr><p:cNvPr id=\"" + id + "\" name=\"Picture " + id + "\" descr=\"\"/>"
            + "<p:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></p:cNvPicPr><p:nvPr/></p:nvPicPr>"
            + "<p:blipFill><a:blip r:embed=\"rId2\"/><a:srcRect l=\"" + l + "\" t=\"" + t + "\" r=\"" + r + "\" b=\"" + b + "\"/>"
            + "<a:stretch><a:fillRect/></a:stretch></p:blipFill>"
            + "<p:spPr><a:xfrm><a:off x=\"" + x + "\" y=\"" + y + "\"/><a:ext cx=\"" + cx + "\" cy=\"" + cy + "\"/></a:xfrm>"
            + "<a:prstGeom prst=\"" + (round ? "roundRect" : "rect") + "\"><a:avLst>"
            + (round ? "<a:gd name=\"adj\" fmla=\"val 5000\"/>" : "") + "</a:avLst></a:prstGeom></p:spPr></p:pic>";
    }

    private static String[] lines(String body) {
        ArrayList<String> out = new ArrayList<String>();
        for (String l : body.replace("\r", "").split("\n")) {
            String t = l.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out.toArray(new String[0]);
    }

    /** Андозаи калонтарини ҳарф (pt), ки банд дар қуттии [widthPt] x [heightPt] ҷо мешавад. */
    static int bulletSize(String[] ls, double widthPt, double heightPt) {
        int[] sizes = {28, 26, 24, 22, 20, 18, 16, 14, 12};
        for (int s : sizes) {
            double cpl = Math.max(8, (widthPt - 27) / (s * 0.5));
            double total = 0;
            for (String l : ls) {
                int n = (int) Math.ceil(l.length() / cpl);
                if (n < 1) n = 1;
                total += n * s * 1.2 + s * 0.45;
            }
            if (total <= heightPt) return s;
        }
        return 12;
    }

    static int titleSize(String t, double widthPt, double heightPt, int[] sizes) {
        for (int s : sizes) {
            double cpl = Math.max(6, widthPt / (s * 0.55));
            int n = (int) Math.ceil(t.length() / cpl);
            if (n < 1) n = 1;
            if (n * s * 1.2 <= heightPt) return s;
        }
        return sizes[sizes.length - 1];
    }

    private static String bullets(String[] ls, int sz, String accent, String textColor) {
        StringBuilder b = new StringBuilder();
        for (String l : ls) {
            b.append("<a:p><a:pPr marL=\"342900\" indent=\"-342900\"><a:spcBef><a:spcPts val=\"800\"/></a:spcBef>"
                + "<a:buClr><a:srgbClr val=\"" + accent + "\"/></a:buClr><a:buFont typeface=\"Arial\"/><a:buChar char=\"&#8226;\"/></a:pPr>")
                .append(run(l, sz * 100, false, textColor)).append("</a:p>");
        }
        if (ls.length == 0) b.append("<a:p><a:endParaRPr lang=\"ru-RU\"/></a:p>");
        return b.toString();
    }

    static String slideXml(int index, SlideData s, String bg, String titleColor, String textColor, String accent, boolean numbers) {
        StringBuilder sh = new StringBuilder();
        String[] ls = lines(s.body);
        boolean img = s.usesImage();
        String layout = s.layout;
        int id = 2;

        if ("TITLE".equals(layout) || ("FULL".equals(layout) && !img)) {
            String tc = img ? "FFFFFF" : titleColor;
            String xc = img ? "E6F0EA" : textColor;
            if (img) {
                sh.append(pic(id++, 0, 0, e(960), e(540), s.imageW, s.imageH, false));
                sh.append(rect(id++, "Scrim", 0, 0, e(960), e(540), "000000", 55000));
            }
            int tsz = titleSize(s.title, 816, 136.8, new int[]{44, 40, 36, 32, 28, 24});
            sh.append(textShape(id++, "Title", e(72), e(144), e(816), e(136.8), "b",
                "<a:p><a:pPr algn=\"ctr\"/>" + run(s.title, tsz * 100, true, tc) + "</a:p>"));
            sh.append(rect(id++, "Accent", e(426), e(295.2), e(108), e(4.32), accent, 100000));
            StringBuilder sub = new StringBuilder();
            for (String l : ls) sub.append("<a:p><a:pPr algn=\"ctr\"/>").append(run(l, 2400, false, xc)).append("</a:p>");
            if (ls.length == 0) sub.append("<a:p><a:endParaRPr lang=\"ru-RU\"/></a:p>");
            sh.append(textShape(id++, "Subtitle", e(72), e(309.6), e(816), e(144), "t", sub.toString()));
        } else if ("FULL".equals(layout)) {
            sh.append(pic(id++, 0, 0, e(960), e(540), s.imageW, s.imageH, false));
            sh.append(rect(id++, "Scrim", 0, 0, e(960), e(540), "000000", 60000));
            int tsz = titleSize(s.title, 859.2, 110, new int[]{40, 36, 32, 28, 24});
            sh.append(textShape(id++, "Title", e(50.4), e(230), e(859.2), e(110), "b",
                "<a:p>" + run(s.title, tsz * 100, true, "FFFFFF") + "</a:p>"));
            sh.append(rect(id++, "Accent", e(50.4), e(348), e(108), e(4.32), accent, 100000));
            int bsz = Math.min(24, bulletSize(ls, 859.2, 140));
            sh.append(textShape(id++, "Body", e(50.4), e(366), e(859.2), e(140), "t", bullets(ls, bsz, accent, "E6F0EA")));
        } else {
            boolean right = "IMG_RIGHT".equals(layout) && img;
            boolean left = "IMG_LEFT".equals(layout) && img;
            int tsz = titleSize(s.title, 859.2, 79.2, new int[]{36, 32, 28, 24, 20});
            sh.append(textShape(id++, "Title", e(50.4), e(28.8), e(859.2), e(79.2), "b",
                "<a:p>" + run(s.title, tsz * 100, true, titleColor) + "</a:p>"));
            sh.append(rect(id++, "Accent", e(50.4), e(115.2), e(108), e(4.32), accent, 100000));
            double bx = 50.4, bw = 859.2;
            if (right) {
                bw = 453.6;
                sh.append(pic(id++, e(525.6), e(136.8), e(384), e(352.8), s.imageW, s.imageH, true));
            } else if (left) {
                bx = 468;
                bw = 441.6;
                sh.append(pic(id++, e(50.4), e(136.8), e(384), e(352.8), s.imageW, s.imageH, true));
            }
            int bsz = bulletSize(ls, bw, 352.8);
            sh.append(textShape(id++, "Body", e(bx), e(136.8), e(bw), e(352.8), "t", bullets(ls, bsz, accent, textColor)));
        }

        if (numbers && index > 0) {
            String nc = ("FULL".equals(layout) && img) ? "FFFFFF" : textColor;
            sh.append(textShape(id++, "Number", e(870), e(510), e(60), e(20), "ctr",
                "<a:p><a:pPr algn=\"r\"/>" + run(String.valueOf(index + 1), 1200, false, nc) + "</a:p>"));
        }
        return XML + "<p:sld " + NS + "><p:cSld><p:bg><p:bgPr><a:solidFill><a:srgbClr val=\"" + bg + "\"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>"
            + "<p:spTree>" + GRP + sh + "</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>";
    }

    private static String themeXml() {
        String[] clr = {"dk2", "44546A", "lt2", "E7E6E6", "accent1", "4472C4", "accent2", "ED7D31", "accent3", "A5A5A5",
            "accent4", "FFC000", "accent5", "5B9BD5", "accent6", "70AD47", "hlink", "0563C1", "folHlink", "954F72"};
        StringBuilder sb = new StringBuilder();
        sb.append(XML).append("<a:theme xmlns:a=\"").append(A).append("\" name=\"ListOffice\"><a:themeElements><a:clrScheme name=\"ListOffice\">")
            .append("<a:dk1><a:srgbClr val=\"000000\"/></a:dk1><a:lt1><a:srgbClr val=\"FFFFFF\"/></a:lt1>");
        for (int i = 0; i < clr.length; i += 2) {
            sb.append("<a:").append(clr[i]).append("><a:srgbClr val=\"").append(clr[i + 1]).append("\"/></a:").append(clr[i]).append(">");
        }
        sb.append("</a:clrScheme><a:fontScheme name=\"ListOffice\"><a:majorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:majorFont>")
            .append("<a:minorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:minorFont></a:fontScheme>")
            .append("<a:fmtScheme name=\"ListOffice\"><a:fillStyleLst>");
        for (int i = 0; i < 3; i++) sb.append("<a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>");
        sb.append("</a:fillStyleLst><a:lnStyleLst>");
        int[] w = {6350, 12700, 19050};
        for (int i = 0; i < 3; i++) sb.append("<a:ln w=\"").append(w[i]).append("\"><a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill></a:ln>");
        sb.append("</a:lnStyleLst><a:effectStyleLst>");
        for (int i = 0; i < 3; i++) sb.append("<a:effectStyle><a:effectLst/></a:effectStyle>");
        sb.append("</a:effectStyleLst><a:bgFillStyleLst>");
        for (int i = 0; i < 3; i++) sb.append("<a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>");
        sb.append("</a:bgFillStyleLst></a:fmtScheme></a:themeElements></a:theme>");
        return sb.toString();
    }

    private static String rels(String... triples) {
        StringBuilder sb = new StringBuilder(XML);
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i + 2 < triples.length; i += 3) {
            sb.append("<Relationship Id=\"").append(triples[i]).append("\" Type=\"").append(R).append("/").append(triples[i + 1])
                .append("\" Target=\"").append(triples[i + 2]).append("\"/>");
        }
        return sb.append("</Relationships>").toString();
    }

    public static void write(OutputStream out, List<SlideData> slides,
                             String bg, String titleColor, String textColor, String accent,
                             boolean numbers) throws IOException {
        int n = slides.size();
        ZipOutputStream z = new ZipOutputStream(out);

        StringBuilder ct = new StringBuilder(XML);
        ct.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
            .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
            .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
            .append("<Default Extension=\"jpg\" ContentType=\"image/jpeg\"/>")
            .append("<Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/>")
            .append("<Override PartName=\"/ppt/slideMasters/slideMaster1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml\"/>")
            .append("<Override PartName=\"/ppt/slideLayouts/slideLayout1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml\"/>")
            .append("<Override PartName=\"/ppt/theme/theme1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.theme+xml\"/>");
        for (int i = 1; i <= n; i++) {
            ct.append("<Override PartName=\"/ppt/slides/slide").append(i)
                .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>");
        }
        ct.append("</Types>");
        put(z, "[Content_Types].xml", ct.toString());
        put(z, "_rels/.rels", rels("rId1", "officeDocument", "ppt/presentation.xml"));

        StringBuilder ids = new StringBuilder();
        String[] prels = new String[(n + 2) * 3];
        prels[0] = "rId1"; prels[1] = "slideMaster"; prels[2] = "slideMasters/slideMaster1.xml";
        for (int i = 1; i <= n; i++) {
            ids.append("<p:sldId id=\"").append(255 + i).append("\" r:id=\"rId").append(i + 1).append("\"/>");
            prels[i * 3] = "rId" + (i + 1); prels[i * 3 + 1] = "slide"; prels[i * 3 + 2] = "slides/slide" + i + ".xml";
        }
        prels[(n + 1) * 3] = "rId" + (n + 2); prels[(n + 1) * 3 + 1] = "theme"; prels[(n + 1) * 3 + 2] = "theme/theme1.xml";
        put(z, "ppt/presentation.xml", XML + "<p:presentation " + NS + " saveSubsetFonts=\"1\">"
            + "<p:sldMasterIdLst><p:sldMasterId id=\"2147483648\" r:id=\"rId1\"/></p:sldMasterIdLst>"
            + "<p:sldIdLst>" + ids + "</p:sldIdLst>"
            + "<p:sldSz cx=\"12192000\" cy=\"6858000\"/><p:notesSz cx=\"6858000\" cy=\"9144000\"/></p:presentation>");
        put(z, "ppt/_rels/presentation.xml.rels", rels(prels));

        put(z, "ppt/slideMasters/slideMaster1.xml", XML + "<p:sldMaster " + NS + "><p:cSld><p:bg><p:bgRef idx=\"1001\"><a:schemeClr val=\"bg1\"/></p:bgRef></p:bg>"
            + "<p:spTree>" + GRP + "</p:spTree></p:cSld>"
            + "<p:clrMap bg1=\"lt1\" tx1=\"dk1\" bg2=\"lt2\" tx2=\"dk2\" accent1=\"accent1\" accent2=\"accent2\" accent3=\"accent3\" accent4=\"accent4\" accent5=\"accent5\" accent6=\"accent6\" hlink=\"hlink\" folHlink=\"folHlink\"/>"
            + "<p:sldLayoutIdLst><p:sldLayoutId id=\"2147483649\" r:id=\"rId1\"/></p:sldLayoutIdLst>"
            + "<p:txStyles><p:titleStyle><a:lvl1pPr><a:defRPr sz=\"4400\"/></a:lvl1pPr></p:titleStyle>"
            + "<p:bodyStyle><a:lvl1pPr><a:defRPr sz=\"2400\"/></a:lvl1pPr></p:bodyStyle>"
            + "<p:otherStyle><a:lvl1pPr><a:defRPr sz=\"1800\"/></a:lvl1pPr></p:otherStyle></p:txStyles></p:sldMaster>");
        put(z, "ppt/slideMasters/_rels/slideMaster1.xml.rels",
            rels("rId1", "slideLayout", "../slideLayouts/slideLayout1.xml", "rId2", "theme", "../theme/theme1.xml"));
        put(z, "ppt/slideLayouts/slideLayout1.xml", XML + "<p:sldLayout " + NS + " type=\"blank\" preserve=\"1\"><p:cSld name=\"Blank\">"
            + "<p:spTree>" + GRP + "</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>");
        put(z, "ppt/slideLayouts/_rels/slideLayout1.xml.rels", rels("rId1", "slideMaster", "../slideMasters/slideMaster1.xml"));
        put(z, "ppt/theme/theme1.xml", themeXml());

        for (int i = 0; i < n; i++) {
            SlideData s = slides.get(i);
            put(z, "ppt/slides/slide" + (i + 1) + ".xml", slideXml(i, s, bg, titleColor, textColor, accent, numbers));
            if (s.usesImage()) {
                put(z, "ppt/slides/_rels/slide" + (i + 1) + ".xml.rels",
                    rels("rId1", "slideLayout", "../slideLayouts/slideLayout1.xml",
                        "rId2", "image", "../media/image" + (i + 1) + ".jpg"));
                z.putNextEntry(new ZipEntry("ppt/media/image" + (i + 1) + ".jpg"));
                z.write(s.image);
                z.closeEntry();
            } else {
                put(z, "ppt/slides/_rels/slide" + (i + 1) + ".xml.rels", rels("rId1", "slideLayout", "../slideLayouts/slideLayout1.xml"));
            }
        }
        z.finish();
        z.flush();
    }

    private static void put(ZipOutputStream z, String name, String content) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(content.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }
}

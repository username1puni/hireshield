package com.hireshield.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hireshield.exception.BadRequestException;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * Gets plain text out of screenshots (OCR), PDFs and job pages.
 */
@Slf4j
@Service
public class TextExtractionService {

    public static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");
    public static final String PDF_TYPE = "application/pdf";
    private static final int MAX_PDF_PAGES = 5;
    private static final int MAX_TEXT = 20_000;

    private final String tessdataPath;
    private final String language;

    public TextExtractionService(@Value("${app.ocr.tessdata-path}") String tessdataPath,
                                 @Value("${app.ocr.language}") String language) {
        this.tessdataPath = tessdataPath;
        this.language = language;
    }

    public String fromImage(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new BadRequestException("Could not read the image. Please upload a PNG or JPG screenshot.");
            }
            return limit(ocr(image));
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded image");
        }
    }

    String ocr(BufferedImage image) {
        // Tesseract instances are not thread safe, so create one per call. It is cheap.
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(language);
        tesseract.setVariable("user_defined_dpi", "150");
        try {
            return tesseract.doOCR(image);
        } catch (TesseractException | Error e) {
            // UnsatisfiedLinkError lands here when the native library is missing.
            log.error("OCR failed", e);
            throw new BadRequestException("Could not read text from the image");
        }
    }

    public String fromPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(Math.min(MAX_PDF_PAGES, document.getNumberOfPages()));
            String text = stripper.getText(document);
            if (text.isBlank()) {
                throw new BadRequestException("This PDF has no text layer. Take a screenshot of it and upload the image instead.");
            }
            return limit(text);
        } catch (IOException e) {
            throw new BadRequestException("Could not read the PDF");
        }
    }

    public String fromUrl(String url) {
        URI uri = validatePublicUrl(url);
        try {
            Document page = Jsoup.connect(uri.toString())
                    .userAgent("Mozilla/5.0 (compatible; HireShield/1.0)")
                    .timeout(8000)
                    .maxBodySize(2_000_000)
                    .followRedirects(true)
                    .get();
            page.select("script, style, nav, footer, header, noscript").remove();
            String text = page.title() + "\n" + page.body().wholeText();
            return limit(text.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim());
        } catch (IOException e) {
            throw new BadRequestException("Could not open that page: " + e.getMessage());
        }
    }

    /** Blocks requests to localhost and private networks (basic SSRF protection). */
    static URI validatePublicUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("That does not look like a valid link");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https") || uri.getHost() == null) {
            throw new BadRequestException("Only http and https links are supported");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                        || address.isAnyLocalAddress()) {
                    throw new BadRequestException("That link points to a private address");
                }
            }
        } catch (UnknownHostException e) {
            throw new BadRequestException("Could not find that website");
        }
        return uri;
    }

    private static String limit(String text) {
        String cleaned = text.replace("\u0000", "").trim();
        return cleaned.length() > MAX_TEXT ? cleaned.substring(0, MAX_TEXT) : cleaned;
    }
}

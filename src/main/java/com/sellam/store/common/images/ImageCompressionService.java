package com.sellam.store.common.images;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Réduit le poids des images envoyées (logo, photo produit, profil, pièce jointe)
 * sans changement visible à l'écran.
 *
 * Une image déjà assez petite et déjà dans les dimensions d'affichage est
 * renvoyée telle quelle : on ne la ré-encode pas, pour ne pas l'abîmer.
 * Sinon on la ramène au plus grand côté utile, en conservant les
 * transparences (PNG) et l'orientation des photos de téléphone.
 * Le JPEG est écrit en qualité haute (0,92), ce qui reste indistinguishable
 * d'un original une fois affiché dans l'application.
 */
@Service
@Slf4j
public class ImageCompressionService
{
    public enum Use
    {
        LOGO(512, 200 * 1024),
        PROFILE(512, 200 * 1024),
        PRODUCT(1600, 700 * 1024),
        ATTACHMENT(1600, 700 * 1024);

        private final int maxEdge;
        private final int skipBelowBytes;

        Use(int maxEdge, int skipBelowBytes)
        {
            this.maxEdge = maxEdge;
            this.skipBelowBytes = skipBelowBytes;
        }
    }

    /** Qualité JPEG haute : le détail reste celui d'une photo nette à l'écran. */
    private static final float JPEG_QUALITY = 0.92f;

    public PreparedImage prepare(byte[] original, String contentType, Use use)
    {
        if (original == null || original.length == 0)
        {
            return PreparedImage.unchanged(original, contentType);
        }
        try
        {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(original));
            if (decoded == null)
            {
                return PreparedImage.unchanged(original, contentType);
            }
            BufferedImage oriented = applyExifOrientation(original, decoded);
            boolean resized = Math.max(oriented.getWidth(), oriented.getHeight()) > use.maxEdge;
            if (!resized && original.length <= use.skipBelowBytes)
            {
                return PreparedImage.unchanged(original, contentType);
            }

            BufferedImage fitted = resize(oriented, use.maxEdge);
            boolean keepPng = hasAlpha(fitted) || "image/png".equalsIgnoreCase(contentType);
            PreparedImage encoded = keepPng ? writePng(fitted) : writeJpeg(fitted);
            if (!resized && encoded.bytes().length >= original.length)
            {
                return PreparedImage.unchanged(original, contentType);
            }
            return encoded;
        }
        catch (Exception e)
        {
            log.warn("Compression d'image ignorée, fichier d'origine conservé : {}", e.getMessage());
            return PreparedImage.unchanged(original, contentType);
        }
    }

    private BufferedImage applyExifOrientation(byte[] original, BufferedImage image)
    {
        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(original));
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory == null || !directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION))
            {
                return image;
            }
            int orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            return switch (orientation)
            {
                case 2 -> flip(image, true, false);
                case 3 -> rotate(image, Math.PI);
                case 4 -> flip(image, false, true);
                case 6 -> rotate(image, Math.PI / 2);
                case 8 -> rotate(image, -Math.PI / 2);
                default -> image;
            };
        }
        catch (Exception e)
        {
            return image;
        }
    }

    private BufferedImage rotate(BufferedImage src, double radians)
    {
        int w = src.getWidth();
        int h = src.getHeight();
        boolean quarter = Math.abs(radians - Math.PI / 2) < 0.01 || Math.abs(radians + Math.PI / 2) < 0.01;
        int nw = quarter ? h : w;
        int nh = quarter ? w : h;
        AffineTransform transform = new AffineTransform();
        if (radians > 0 && quarter)
        {
            transform.translate(h, 0);
        }
        else if (radians < 0 && quarter)
        {
            transform.translate(0, w);
        }
        else
        {
            transform.translate(w, h);
        }
        transform.rotate(radians);
        return draw(src, nw, nh, transform);
    }

    private BufferedImage flip(BufferedImage src, boolean horizontal, boolean vertical)
    {
        AffineTransform transform = new AffineTransform();
        transform.scale(horizontal ? -1 : 1, vertical ? -1 : 1);
        transform.translate(horizontal ? -src.getWidth() : 0, vertical ? -src.getHeight() : 0);
        return draw(src, src.getWidth(), src.getHeight(), transform);
    }

    private BufferedImage resize(BufferedImage src, int maxEdge)
    {
        BufferedImage current = src;
        while (Math.max(current.getWidth(), current.getHeight()) / 2 >= maxEdge)
        {
            current = scale(current, Math.max(1, current.getWidth() / 2), Math.max(1, current.getHeight() / 2));
        }
        int longEdge = Math.max(current.getWidth(), current.getHeight());
        if (longEdge <= maxEdge)
        {
            return current;
        }
        double factor = (double) maxEdge / longEdge;
        int width = Math.max(1, (int) Math.round(current.getWidth() * factor));
        int height = Math.max(1, (int) Math.round(current.getHeight() * factor));
        return scale(current, width, height);
    }

    private BufferedImage scale(BufferedImage src, int width, int height)
    {
        int type = hasAlpha(src) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage dst = new BufferedImage(width, height, type);
        Graphics2D graphics = dst.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(src, 0, 0, width, height, null);
        graphics.dispose();
        return dst;
    }

    private BufferedImage draw(BufferedImage src, int width, int height, AffineTransform transform)
    {
        int type = hasAlpha(src) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage dst = new BufferedImage(width, height, type);
        Graphics2D graphics = dst.createGraphics();
        graphics.setTransform(transform);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(src, 0, 0, null);
        graphics.dispose();
        return dst;
    }

    private boolean hasAlpha(BufferedImage image)
    {
        return image.getColorModel() != null && image.getColorModel().hasAlpha();
    }

    private PreparedImage writeJpeg(BufferedImage image) throws IOException
    {
        BufferedImage rgb = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB)
        {
            rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgb.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        if (param.canWriteProgressive())
        {
            param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
        }
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output))
        {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(rgb, null, null), param);
        }
        finally
        {
            writer.dispose();
        }
        return new PreparedImage(output.toByteArray(), "image/jpeg", ".jpg");
    }

    private PreparedImage writePng(BufferedImage image) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output))
        {
            throw new IOException("Écriture PNG impossible");
        }
        return new PreparedImage(output.toByteArray(), "image/png", ".png");
    }

    public record PreparedImage(byte[] bytes, String contentType, String extension)
    {
        static PreparedImage unchanged(byte[] bytes, String contentType)
        {
            return new PreparedImage(bytes, contentType, extensionFor(contentType));
        }

        private static String extensionFor(String contentType)
        {
            if (contentType == null)
            {
                return ".jpg";
            }
            return switch (contentType.toLowerCase())
            {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
        }
    }
}

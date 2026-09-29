package com.sellam.store.common.images;

import org.junit.jupiter.api.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageCompressionServiceTest
{
    private final ImageCompressionService service = new ImageCompressionService();

    @Test
    void rameneUneGrandePhotoSousLeCoteUtile() throws Exception
    {
        byte[] source = jpeg(1800, 1200);
        ImageCompressionService.PreparedImage prepared = service.prepare(source, "image/jpeg", ImageCompressionService.Use.PRODUCT);

        assertEquals("image/jpeg", prepared.contentType());
        assertEquals(".jpg", prepared.extension());
        assertTrue(prepared.bytes().length < source.length);
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(prepared.bytes()));
        assertEquals(1600, Math.max(result.getWidth(), result.getHeight()));
        assertEquals(1600, result.getWidth());
        assertEquals(1067, result.getHeight());
    }

    @Test
    void laisseIntacteUneImageDejaAdaptee() throws Exception
    {
        byte[] source = jpeg(80, 60);
        ImageCompressionService.PreparedImage prepared = service.prepare(source, "image/jpeg", ImageCompressionService.Use.PRODUCT);

        assertEquals(source.length, prepared.bytes().length);
        assertEquals("image/jpeg", prepared.contentType());
    }

    @Test
    void conserveLaTransparenceDunLogoPng() throws Exception
    {
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x00000000);
        image.setRGB(1, 1, Color.RED.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        ImageCompressionService.PreparedImage prepared = service.prepare(output.toByteArray(), "image/png", ImageCompressionService.Use.LOGO);

        assertEquals("image/png", prepared.contentType());
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(prepared.bytes()));
        assertEquals(0, (result.getRGB(0, 0) >>> 24) & 0xff);
        assertEquals(255, (result.getRGB(1, 1) >>> 24) & 0xff);
    }

    @Test
    void conserveUnFichierIllisible()
    {
        byte[] garbage = new byte[] {1, 2, 3, 4};
        ImageCompressionService.PreparedImage prepared = service.prepare(garbage, "image/webp", ImageCompressionService.Use.PRODUCT);

        assertEquals(4, prepared.bytes().length);
        assertEquals("image/webp", prepared.contentType());
        assertEquals(".webp", prepared.extension());
    }

    private byte[] jpeg(int width, int height) throws Exception
    {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        for (int x = 0; x < width; x += 8)
        {
            graphics.setColor(new Color(x * 255 / width, 80, 160));
            graphics.fillRect(x, 0, 8, height);
        }
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(1f);
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output))
        {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        }
        finally
        {
            writer.dispose();
        }
        return output.toByteArray();
    }
}

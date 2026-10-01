package com.myfitness.ai.infrastructure.client;

import com.myfitness.ai.application.dto.request.FoodPhotoCommand;
import com.myfitness.ai.application.exception.InvalidFoodPhotoException;
import com.myfitness.ai.application.exception.InvalidFoodPhotoException.Reason;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class FoodPhotoImagePreparer {
    public byte[] prepare(FoodPhotoCommand command) {
        validateUpload(command);
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(command.bytes()))) {
            BufferedImage source = readImage(input, command.contentType());
            return encodeJpeg(normalize(source));
        } catch (IOException exception) {
            throw new InvalidFoodPhotoException(Reason.INVALID);
        }
    }

    private void validateUpload(FoodPhotoCommand command) {
        if (command == null || command.bytes() == null || command.bytes().length == 0) {
            throw new InvalidFoodPhotoException(Reason.INVALID);
        }
        if (command.bytes().length > 5 * 1024 * 1024) {
            throw new InvalidFoodPhotoException(Reason.TOO_LARGE);
        }
        if (!"image/jpeg".equals(command.contentType()) && !"image/png".equals(command.contentType())) {
            throw new InvalidFoodPhotoException(Reason.UNSUPPORTED);
        }
    }

    private BufferedImage readImage(MemoryCacheImageInputStream input, String contentType) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) {
            throw new InvalidFoodPhotoException(Reason.INVALID);
        }
        ImageReader reader = readers.next();
        try {
            reader.setInput(input, true, true);
            validateFormat(reader.getFormatName(), contentType);
            validateDimensions(reader.getWidth(0), reader.getHeight(0));
            return reader.read(0);
        } finally {
            reader.dispose();
        }
    }

    private void validateFormat(String format, String contentType) {
        if (!(format.equalsIgnoreCase("JPEG") || format.equalsIgnoreCase("PNG"))
                || (format.equalsIgnoreCase("PNG") != "image/png".equals(contentType))) {
            throw new InvalidFoodPhotoException(Reason.UNSUPPORTED);
        }
    }

    private void validateDimensions(int width, int height) {
        if (width < 1 || height < 1 || (long) width * height > 16_000_000) {
            throw new InvalidFoodPhotoException(Reason.INVALID);
        }
    }

    private BufferedImage normalize(BufferedImage source) {
        double scale = Math.min(1, 1600.0 / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) (source.getWidth() * scale));
        int height = Math.max(1, (int) (source.getHeight() * scale));
        BufferedImage normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return normalized;
    }

    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", output);
        return output.toByteArray();
    }
}

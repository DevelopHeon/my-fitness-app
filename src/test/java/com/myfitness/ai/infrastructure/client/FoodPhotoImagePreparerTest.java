package com.myfitness.ai.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.ai.application.dto.request.FoodPhotoCommand;
import com.myfitness.ai.application.exception.InvalidFoodPhotoException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.ref.Reference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FoodPhotoImagePreparerTest {
    @TempDir
    Path directory;

    @Test
    void preparesTwoMaximumPhotosWith256MiBHeap() throws Exception {
        Path image = directory.resolve("maximum.png");
        ImageIO.write(new BufferedImage(8000, 4000, BufferedImage.TYPE_INT_ARGB), "png", image.toFile());
        assertThat(Files.size(image)).isLessThan(5 * 1024 * 1024);
        String classpath = Path.of(FoodPhotoImagePreparerTest.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()) + File.pathSeparator
                + Path.of(FoodPhotoImagePreparer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path output = directory.resolve("probe.log");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xms64m", "-Xmx256m", "-XX:+UseG1GC", "-cp", classpath,
                FoodPhotoImagePreparerTest.class.getName(), image.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        assertThat(process.exitValue()).as(Files.readString(output)).isZero();
    }

    @Test
    void rejectsOriginalDimensionsAboveLimitBeforeSubsampling() throws Exception {
        Path image = directory.resolve("too-many-pixels.png");
        ImageIO.write(new BufferedImage(8001, 4000, BufferedImage.TYPE_INT_ARGB), "png", image.toFile());
        FoodPhotoCommand command = new FoodPhotoCommand(Files.readAllBytes(image), "image/png");
        assertThatThrownBy(() -> new FoodPhotoImagePreparer().prepare(command))
                .isInstanceOf(InvalidFoodPhotoException.class);
    }

    // Separate JVM makes the heap contract independent of Gradle's test-worker heap.
    public static void main(String[] args) throws Exception {
        byte[] resident = new byte[64 * 1024 * 1024];
        FoodPhotoCommand command = new FoodPhotoCommand(Files.readAllBytes(Path.of(args[0])), "image/png");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<byte[]> first = executor.submit(() -> prepareAfterStart(command, start));
            Future<byte[]> second = executor.submit(() -> prepareAfterStart(command, start));
            start.countDown();
            for (Future<byte[]> result : List.of(first, second)) {
                BufferedImage normalized = ImageIO.read(new ByteArrayInputStream(result.get()));
                if (normalized.getWidth() != 1600 || normalized.getHeight() != 800) {
                    throw new AssertionError("Unexpected normalized dimensions");
                }
            }
            System.out.println("Two maximum photos prepared; reserved bytes: " + resident.length);
            Reference.reachabilityFence(resident);
        }
    }

    private static byte[] prepareAfterStart(FoodPhotoCommand command, CountDownLatch start) throws Exception {
        start.await();
        return new FoodPhotoImagePreparer().prepare(command);
    }
}

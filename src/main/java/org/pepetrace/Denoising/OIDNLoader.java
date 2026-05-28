package org.pepetrace.Denoising;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OIDNLoader {

    private static final String RESOURCE_BASE = "lib/oidn-2.4.1.x86_64.linux";

    private static boolean loaded;

    public static synchronized void load() {
        if (loaded) return;

        Path libDir = findOrExtract();
        System.setProperty(
            "jna.library.path",
            libDir.toAbsolutePath().toString()
        );

        for (String name : LOAD_ORDER) {
            Path p = libDir.resolve(name);
            if (!Files.exists(p)) continue;
            boolean optional = OPTIONAL_LIBS.contains(name);
            try {
                System.load(p.toAbsolutePath().toString());
            } catch (UnsatisfiedLinkError e) {
                if (optional) {
                    System.err.println("Skipping optional library (not available on this system): " + name);
                } else {
                    throw e;
                }
            }
        }

        loaded = true;
    }

    private static Path findOrExtract() {
        URL url = OIDNLoader.class
            .getClassLoader()
            .getResource(RESOURCE_BASE + "/libOpenImageDenoise.so.2.4.1");
        if (url == null) {
            throw new RuntimeException(
                "OIDN libraries not found on classpath at " + RESOURCE_BASE
            );
        }

        if ("file".equals(url.getProtocol())) {
            try {
                return Paths.get(url.toURI()).getParent();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        return extractToTemp();
    }

    private static Path extractToTemp() {
        try {
            Path tempDir = Files.createTempDirectory("pepetrace-oidn-");
            tempDir.toFile().deleteOnExit();

            for (Map.Entry<String, String> entry : FILES_AND_LINKS.entrySet()) {
                String name = entry.getKey();
                Path dest = tempDir.resolve(name);
                String target = entry.getValue();

                if (target != null) {
                    try {
                        Files.createSymbolicLink(dest, Path.of(target));
                    } catch (IOException e) {
                        Files.copy(
                            OIDNLoader.class
                                .getClassLoader()
                                .getResourceAsStream(
                                    RESOURCE_BASE + "/" + target
                                ),
                            dest,
                            StandardCopyOption.REPLACE_EXISTING
                        );
                    }
                } else {
                    try (
                        InputStream is = OIDNLoader.class
                            .getClassLoader()
                            .getResourceAsStream(RESOURCE_BASE + "/" + name)
                    ) {
                        if (is != null) {
                            Files.copy(
                                is,
                                dest,
                                StandardCopyOption.REPLACE_EXISTING
                            );
                        }
                    }
                    dest.toFile().deleteOnExit();
                }
            }

            return tempDir;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract OIDN libraries", e);
        }
    }

    private static final Set<String> OPTIONAL_LIBS = new HashSet<>(Set.of(
        "libOpenImageDenoise_device_cuda.so.2.4.1",
        "libOpenImageDenoise_device_hip.so.2.4.1",
        "libOpenImageDenoise_device_sycl.so.2.4.1",
        "libsycl.so.8.0.0-0"
    ));

    private static final Map<String, String> FILES_AND_LINKS =
        new LinkedHashMap<>();
    private static final List<String> LOAD_ORDER = new ArrayList<>();

    static {
        FILES_AND_LINKS.put("libtbb.so.12.17", null);
        FILES_AND_LINKS.put("libtbb.so.12", "libtbb.so.12.17");
        FILES_AND_LINKS.put("libtbb.so", "libtbb.so.12.17");

        FILES_AND_LINKS.put("libtbbbind_2_0.so.3.17", null);
        FILES_AND_LINKS.put("libtbbbind_2_0.so.3", "libtbbbind_2_0.so.3.17");

        FILES_AND_LINKS.put("libtbbbind_2_5.so.3.17", null);
        FILES_AND_LINKS.put("libtbbbind_2_5.so.3", "libtbbbind_2_5.so.3.17");

        FILES_AND_LINKS.put("libtbbbind.so.3.17", null);
        FILES_AND_LINKS.put("libtbbbind.so.3", "libtbbbind.so.3.17");

        FILES_AND_LINKS.put("libumf.so.0.11.0", null);
        FILES_AND_LINKS.put("libumf.so.0", "libumf.so.0.11.0");
        FILES_AND_LINKS.put("libumf.so", "libumf.so.0.11.0");

        FILES_AND_LINKS.put("libur_loader.so.0.12.0", null);
        FILES_AND_LINKS.put("libur_loader.so.0", "libur_loader.so.0.12.0");
        FILES_AND_LINKS.put("libur_loader.so", "libur_loader.so.0.12.0");

        FILES_AND_LINKS.put("libur_adapter_level_zero.so.0.12.0", null);
        FILES_AND_LINKS.put(
            "libur_adapter_level_zero.so.0",
            "libur_adapter_level_zero.so.0.12.0"
        );
        FILES_AND_LINKS.put(
            "libur_adapter_level_zero.so",
            "libur_adapter_level_zero.so.0.12.0"
        );

        FILES_AND_LINKS.put("libsycl.so.8.0.0-0", null);
        FILES_AND_LINKS.put("libsycl.so.8", "libsycl.so.8.0.0-0");

        FILES_AND_LINKS.put("libOpenImageDenoise_core.so.2.4.1", null);

        FILES_AND_LINKS.put("libOpenImageDenoise_device_cpu.so.2.4.1", null);
        FILES_AND_LINKS.put("libOpenImageDenoise_device_cuda.so.2.4.1", null);
        FILES_AND_LINKS.put("libOpenImageDenoise_device_hip.so.2.4.1", null);
        FILES_AND_LINKS.put("libOpenImageDenoise_device_sycl.so.2.4.1", null);

        FILES_AND_LINKS.put("libOpenImageDenoise.so.2.4.1", null);
        FILES_AND_LINKS.put(
            "libOpenImageDenoise.so.2",
            "libOpenImageDenoise.so.2.4.1"
        );
        FILES_AND_LINKS.put(
            "libOpenImageDenoise.so",
            "libOpenImageDenoise.so.2.4.1"
        );

        LOAD_ORDER.add("libtbb.so.12.17");
        LOAD_ORDER.add("libtbbbind_2_0.so.3.17");
        LOAD_ORDER.add("libtbbbind_2_5.so.3.17");
        LOAD_ORDER.add("libtbbbind.so.3.17");
        LOAD_ORDER.add("libumf.so.0.11.0");
        LOAD_ORDER.add("libur_loader.so.0.12.0");
        LOAD_ORDER.add("libur_adapter_level_zero.so.0.12.0");
        LOAD_ORDER.add("libsycl.so.8.0.0-0");
        LOAD_ORDER.add("libOpenImageDenoise_core.so.2.4.1");
        LOAD_ORDER.add("libOpenImageDenoise_device_cpu.so.2.4.1");
        LOAD_ORDER.add("libOpenImageDenoise_device_cuda.so.2.4.1");
        LOAD_ORDER.add("libOpenImageDenoise_device_hip.so.2.4.1");
        LOAD_ORDER.add("libOpenImageDenoise_device_sycl.so.2.4.1");
        LOAD_ORDER.add("libOpenImageDenoise.so.2.4.1");
    }
}

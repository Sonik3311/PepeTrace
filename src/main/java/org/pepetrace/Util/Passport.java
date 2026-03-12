package org.pepetrace.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Passport {

    final private String gitBranchHash;
    final private String javaVersion;
    final private int buildNumber;
    final private String buildOS;
    final private String buildTime;


    public Passport() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("build-passport.properties")) {
            if (input == null) {
                throw new RuntimeException("build-passport.properties not found in resources");
            }

            properties.load(input);

            // Инициализация полей
            this.gitBranchHash = properties.getProperty("build.git.commit");
            this.javaVersion = properties.getProperty("build.java.version");
            this.buildNumber = Integer.parseInt(properties.getProperty("build.number"));
            this.buildOS = properties.getProperty("build.os");
            this.buildTime = properties.getProperty("build.time");

            System.out.println("\n\u001B[33m==============");
            System.out.println("Build: " + buildNumber);
            System.out.println("Time: " + buildTime);
            System.out.println("Java: " + javaVersion);
            System.out.println("==============\u001B[0m\n");


        } catch (IOException e) {
            throw new RuntimeException("Failed to load build-passport.properties", e);
        }
    }

    public String getGitBranchHash() {
        return gitBranchHash;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public String getBuildOS () {
        return buildOS;
    }

    public String getBuildTime() {
        return buildTime;
    }
}

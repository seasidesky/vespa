package com.yahoo.vespa.hosted.node.maintenance;

import io.airlift.airline.Cli;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import io.airlift.airline.ParseArgumentsUnexpectedException;
import io.airlift.airline.ParseOptionMissingException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author valerijf
 */
public class Maintainer {
    private static final Path RELATIVE_APPLICATION_STORAGE_PATH = Paths.get("home/docker/container-storage");
    private static final Path APPLICATION_STORAGE_PATH_FOR_NODE_ADMIN = Paths.get("/host").resolve(RELATIVE_APPLICATION_STORAGE_PATH);
    private static final Path APPLICATION_STORAGE_PATH_FOR_HOST = Paths.get("/").resolve(RELATIVE_APPLICATION_STORAGE_PATH);

    public static final String APPLICATION_STORAGE_CLEANUP_PATH_PREFIX = "cleanup_";

    public static final String JOB_DELETE_OLD_APP_DATA = "delete-old-app-data";
    public static final String JOB_CLEAN_CORE_DUMPS = "clean-core-dumps";
    public static final String JOB_CLEAN_HOME = "clean-home";

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        Cli.CliBuilder<Runnable> builder = Cli.<Runnable>builder("maintainer.jar")
                .withDescription("This tool makes it easy to delete old log files and other node-admin app data.")
                .withDefaultCommand(Help.class)
                .withCommands(Help.class, DeleteOldAppDataArguments.class, CleanCoreDumpsArguments.class, CleanHomeArguments.class);

        Cli<Runnable> gitParser = builder.build();
        try {
            gitParser.parse(args).run();
        } catch (ParseArgumentsUnexpectedException | ParseOptionMissingException e) {
            System.err.println(e.getMessage());
            gitParser.parse("help").run();
        }
    }

    @Command(name = JOB_DELETE_OLD_APP_DATA, description = "Deletes old app data")
    public static class DeleteOldAppDataArguments implements Runnable {
        @Override
        public void run() {
            String path = applicationStoragePathForNode("/").toString();
            String regex = "^" + Pattern.quote(APPLICATION_STORAGE_CLEANUP_PATH_PREFIX);

            DeleteOldAppData.deleteDirectories(path, Duration.ofDays(7).getSeconds(), regex);
        }
    }

    @Command(name = JOB_CLEAN_CORE_DUMPS, description = "Clean core dumps")
    public static class CleanCoreDumpsArguments implements Runnable {
        @Override
        public void run() {
            File coreDumpsDir = new File("/home/y/var/crash");

            if (coreDumpsDir.exists()) {
                DeleteOldAppData.deleteFilesExceptNMostRecent(coreDumpsDir.getAbsolutePath(), 1);
            }
        }
    }

    @Command(name = JOB_CLEAN_HOME, description = "Clean home directories for large files")
    public static class CleanHomeArguments implements Runnable {
        @Override
        public void run() {
            List<String> exceptions = Arrays.asList("docker", "y", "yahoo");
            File homeDir = new File("/home/");
            long MB = 1 << 20;

            if (homeDir.exists() && homeDir.isDirectory()) {
                for (File file : homeDir.listFiles()) {
                    if (! exceptions.contains(file.getName())) {
                        DeleteOldAppData.deleteFilesLargerThan(file, 100*MB);
                    }
                }
            }
        }
    }


    public static Path applicationStoragePathForHost(String containerName) {
        return APPLICATION_STORAGE_PATH_FOR_HOST.resolve(containerName);
    }

    public static Path applicationStoragePathForNode(String containerName) {
        return APPLICATION_STORAGE_PATH_FOR_NODE_ADMIN.resolve(containerName);
    }
}

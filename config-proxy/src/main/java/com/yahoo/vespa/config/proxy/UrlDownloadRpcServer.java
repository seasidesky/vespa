// Copyright 2018 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.config.proxy;

import com.yahoo.concurrent.DaemonThreadFactory;
import com.yahoo.jrt.Method;
import com.yahoo.jrt.Request;
import com.yahoo.jrt.StringValue;
import com.yahoo.jrt.Supervisor;
import com.yahoo.log.LogLevel;
import com.yahoo.text.Utf8;
import com.yahoo.vespa.defaults.Defaults;
import net.jpountz.xxhash.XXHashFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static com.yahoo.vespa.config.UrlDownloader.DOES_NOT_EXIST;
import static com.yahoo.vespa.config.UrlDownloader.HTTP_ERROR;
import static com.yahoo.vespa.config.UrlDownloader.INTERNAL_ERROR;

/**
 * An RPC server that handles URL download requests.
 *
 * @author lesters
 */
public class UrlDownloadRpcServer {
    private final static Logger log = Logger.getLogger(UrlDownloadRpcServer.class.getName());

    private static final String CONTENTS_FILE_NAME = "contents";
    private static final String LAST_MODFIED_FILE_NAME = "lastmodified";

    private final File downloadBaseDir;
    private final ExecutorService rpcDownloadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                                                                                     new DaemonThreadFactory("Rpc download executor"));

    public UrlDownloadRpcServer(Supervisor supervisor) {
        supervisor.addMethod(new Method("url.waitFor", "s", "s", this, "download")
                                    .methodDesc("get path to url download")
                                    .paramDesc(0, "url", "url")
                                    .returnDesc(0, "path", "path to file"));
        downloadBaseDir = new File(Defaults.getDefaults().underVespaHome("var/db/vespa/download"));
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public final void download(Request req) {
        req.detach();
        rpcDownloadExecutor.execute(() -> downloadFile(req));
    }

    private void downloadFile(Request req) {
        String url = req.parameters().get(0).asString();
        File downloadDir = new File(this.downloadBaseDir, urlToDirName(url));

        try {
            URL website = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) website.openConnection();
            setIfModifiedSince(connection, downloadDir);  // don't download if we already have the file

            if (connection.getResponseCode() == 200) {
                log.log(LogLevel.INFO, "Downloading URL '" + url + "'");
                downloadFile(req, connection, downloadDir);

            } else if (connection.getResponseCode() == 304) {
                log.log(LogLevel.INFO, "URL '" + url + "' already downloaded (server response: 304)");
                req.returnValues().add(new StringValue(new File(downloadDir, CONTENTS_FILE_NAME).getAbsolutePath()));

            } else {
                log.log(LogLevel.ERROR, "Download of URL '" + url + "' got server response: " + connection.getResponseCode());
                req.setError(HTTP_ERROR, String.valueOf(connection.getResponseCode()));
            }

        } catch (Throwable e) {
            log.log(LogLevel.WARNING, "Download of URL '" + url + "' got exception: " + e.getMessage());
            req.setError(INTERNAL_ERROR, "Download of URL '" + url + "' internal error");
        }
        req.returnRequest();
    }

    private static void downloadFile(Request req, HttpURLConnection connection, File downloadDir) throws IOException {
        String url = connection.getURL().toString();
        Files.createDirectories(downloadDir.toPath());
        File contentsPath = new File(downloadDir, CONTENTS_FILE_NAME);
        try (ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream())) {
            try (FileOutputStream fos = new FileOutputStream((contentsPath.getAbsolutePath()))) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                if (contentsPath.exists() && contentsPath.length() > 0) {
                    writeLastModifiedTimestamp(downloadDir, connection.getLastModified());
                    req.returnValues().add(new StringValue(contentsPath.getAbsolutePath()));
                    log.log(LogLevel.DEBUG, () -> "URL '" + url + "' available at " + contentsPath);
                } else {
                    log.log(LogLevel.INFO, "Downloaded URL '" + url + "' not found, returning error");
                    req.setError(DOES_NOT_EXIST, "Downloaded '" + url + "' not found");
                }
            }
        }
    }

    private static String urlToDirName(String uri) {
        return String.valueOf(XXHashFactory.nativeInstance().hash64().hash(ByteBuffer.wrap(Utf8.toBytes(uri)), 0));
    }

    private static void setIfModifiedSince(HttpURLConnection connection, File downloadDir) throws IOException {
        File contents = new File(downloadDir, CONTENTS_FILE_NAME);
        if (contents.exists() && contents.length() > 0) {
            long lastModified = readLastModifiedTimestamp(downloadDir);
            if (lastModified > 0) {
                connection.setIfModifiedSince(lastModified);
            }
        }
    }

    private static long readLastModifiedTimestamp(File downloadDir) throws IOException {
        File lastModified = new File(downloadDir, LAST_MODFIED_FILE_NAME);
        if (lastModified.exists() && lastModified.length() > 0) {
            try (BufferedReader br = new BufferedReader(new FileReader(lastModified))) {
                String timestamp = br.readLine();
                return Long.parseLong(timestamp);
            }
        }
        return 0;
    }

    private static void writeLastModifiedTimestamp(File downloadDir, long timestamp) throws IOException {
        File lastModified = new File(downloadDir, LAST_MODFIED_FILE_NAME);
        try (BufferedWriter lastModifiedWriter = new BufferedWriter(new FileWriter(lastModified.getAbsolutePath()))) {
            lastModifiedWriter.write(Long.toString(timestamp));
        }
    }

}

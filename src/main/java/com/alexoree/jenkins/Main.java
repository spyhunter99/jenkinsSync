/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alexoree.jenkins;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.cli.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @author alex
 */
public class Main {

    final static int WAIT = 5000;

    public static void main(String[] args) throws Exception {
        // create Options object
        Options options = new Options();

        options.addOption("t", false, "throttle the downloads, waits 5 seconds in between each d/l");
        options.addOption("nozip", false, "skip zip (don't zip the output)");

        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("jenkins-sync", options);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        boolean zipOutput = !cmd.hasOption("nozip");
        boolean throttle = cmd.hasOption("t");

        String plugins = "https://updates.jenkins-ci.org/latest/";
        List<String> ps = new ArrayList<String>();
        Document doc = Jsoup.connect(plugins).get();
        for (Element file : doc.select("a")) {
            if (file.hasClass("version")) {
                //System.out.println(file.attr("href"));
                if (file.attr("href").endsWith(".hpi") || file.attr("href").endsWith(".war")) {
                    ps.add(file.attr("href"));
                }
            }
        }

        File root = new File(".");
        //https://updates.jenkins-ci.org/latest/AdaptivePlugin.hpi
        new File("./latest").mkdirs();

        //output zip file
        String zipFile = "jenkinsSync.zip";
        // create byte buffer
        byte[] buffer = new byte[1024];
        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        //download the plugins
        for (int i = 0; i < ps.size(); i++) {
            System.out.println("[" + i + "/" + ps.size() + "] downloading " + plugins + ps.get(i));
            String outputFile = download(root.getAbsolutePath() + "/latest/" + ps.get(i), plugins + ps.get(i));
            if ("SKIP".equals(outputFile)) {
                continue;
            }
            if (zipOutput) {
                FileInputStream fis = new FileInputStream(outputFile);
                // begin writing a new ZIP entry, positions the stream to the start of the entry data
                zos.putNextEntry(new ZipEntry(outputFile.replace(root.getAbsolutePath(), "").replace("updates.jenkins-ci.org/", "").replace("https:/", "")));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();
                new File(root.getAbsolutePath() + "/latest/" + ps.get(i)).deleteOnExit();
            }
            if (throttle) {
                Thread.sleep(WAIT);
            }

        }

        //download the json metadata
        plugins = "https://updates.jenkins-ci.org/";
        ps = new ArrayList<String>();
        doc = Jsoup.connect(plugins).get();
        for (Element file : doc.select("td a")) {
            //System.out.println(file.attr("href"));
            if (file.attr("href").endsWith(".json")) {
                ps.add(file.attr("href"));
            }
        }
        for (int i = 0; i < ps.size(); i++) {
            download(root.getAbsolutePath() + "/" + ps.get(i), plugins + ps.get(i));
            if (zipOutput) {
                FileInputStream fis = new FileInputStream(root.getAbsolutePath() + "/" + ps.get(i));
                // begin writing a new ZIP entry, positions the stream to the start of the entry data
                zos.putNextEntry(new ZipEntry(plugins + ps.get(i)));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();
                new File(root.getAbsolutePath() + "/" + ps.get(i)).deleteOnExit();
            }
            if (throttle) {
                Thread.sleep(WAIT);
            }
        }

        // close the ZipOutputStream
        zos.close();
    }

    private static String download(String localName, String remoteUrl) throws Exception {

        URL obj = new URL(remoteUrl);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) obj.openConnection();
            conn.setReadTimeout(5000);

            System.out.println("Request URL ... " + remoteUrl);

            boolean redirect = false;

            // normally, 3xx is redirect
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    redirect = true;
                }
            }

            if (redirect) {

                // get redirect url from "location" header field
                String newUrl = conn.getHeaderField("Location");

                // get the cookie if need, for login
                String cookies = conn.getHeaderField("Set-Cookie");

                // open the new connnection again
                conn.disconnect();
                conn = (HttpURLConnection) new URL(newUrl).openConnection();

                String version = newUrl.substring(newUrl.lastIndexOf("/", newUrl.lastIndexOf("/") - 1) + 1, newUrl.lastIndexOf("/"));
                String pluginname = localName.substring(localName.lastIndexOf("/") + 1);
                String ext = "";
                if (pluginname.endsWith(".war")) {
                    ext = ".war";
                } else {
                    ext = ".hpi";
                }

                pluginname = pluginname.replace(ext, "");
                localName = localName.replace(pluginname + ext, "/download/plugins/" + pluginname + "/" + version + "/");
                new File(localName).mkdirs();
                localName += pluginname + ext;
                System.out.println("Redirect to URL : " + newUrl);

            }
            if (new File(localName).exists()) {
                System.out.println(localName + " exists, skipping");
                return "SKIP";
            }

            byte[] buffer = new byte[2048];

            int retries = 3;
            while (retries > 0) {
                FileOutputStream baos = null;
                InputStream inputStream = null;
                try {
                    retries--;
                    baos = new FileOutputStream(localName);
                    inputStream = conn.getInputStream();
                    int totalBytes = 0;
                    int read = inputStream.read(buffer);
                    while (read > 0) {
                        totalBytes += read;
                        baos.write(buffer, 0, read);
                        read = inputStream.read(buffer);
                    }
                    inputStream.close();
                    baos.close();
                    System.out.println("Retrieved " + totalBytes + "bytes");
                    break;
                } catch (Exception ex) {
                    System.out.println(remoteUrl + " failed " + ex.getMessage());
                } finally {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                    }
                    try {
                        baos.close();
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception ex) {
        } finally {
            conn.disconnect();
        }
        return localName;

    }
}

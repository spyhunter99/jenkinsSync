/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alexoree.jenkins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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

    final static int WAIT=5000;

    public static void main(String[] args) throws Exception {
        // create Options object
        Options options = new Options();

        options.addOption("t", false, "throttle the downloads, waits 5 seconds in between each d/l");

        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "jenkins-sync", options );


        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);
        boolean throttle = cmd.hasOption("t");

        String plugins = "https://updates.jenkins-ci.org/latest/";
        List<String> ps = new ArrayList<String>();
        Document doc = Jsoup.connect(plugins).get();
        for (Element file : doc.select("td a")) {
            //System.out.println(file.attr("href"));
            if (file.attr("href").endsWith(".hpi") || file.attr("href").endsWith(".war")) {
                ps.add(file.attr("href"));
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
            download(root.getAbsolutePath() + "/latest/" + ps.get(i), plugins + ps.get(i));

            FileInputStream fis = new FileInputStream(root.getAbsolutePath() + "/latest/" + ps.get(i));
            // begin writing a new ZIP entry, positions the stream to the start of the entry data
            zos.putNextEntry(new ZipEntry(plugins + ps.get(i)));
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
            fis.close();
            if (throttle)
                Thread.sleep(WAIT);
            new File(root.getAbsolutePath() + "/latest/" + ps.get(i)).deleteOnExit();
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
            if (throttle)
                Thread.sleep(WAIT);
        }


        // close the ZipOutputStream
        zos.close();
    }

    private static void download(String localName, String remoteUrl) throws Exception {
        URL website = new URL(remoteUrl);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(new File(localName));
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }
}

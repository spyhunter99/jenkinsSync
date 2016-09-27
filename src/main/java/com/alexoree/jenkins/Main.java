/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alexoree.jenkins;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author alex
 */
public class Main {

     public static void main(String[] args) throws Exception {
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
          for (int i = 0; i < ps.size(); i++) {
               System.out.println("[" + i + "/" + ps.size() + "] downloading " + plugins + ps.get(i));
               download(root.getAbsolutePath() + "/latest/" + ps.get(i), plugins + ps.get(i));
          }

          plugins = "https://updates.jenkins-ci.org/";
          ps = new ArrayList<String>();
          doc = Jsoup.connect(plugins).get();
          for (Element file : doc.select("td a")) {
               System.out.println(file.attr("href"));
               if (file.attr("href").endsWith(".json")) {
                    ps.add(file.attr("href"));
               }
          }
          for (int i = 0; i < ps.size(); i++) {
               download(root.getAbsolutePath() + "/" + ps.get(i), plugins + ps.get(i));
          }

     }

     private static void download(String localName, String remoteUrl) throws Exception {

          URL website = new URL(remoteUrl);
          ReadableByteChannel rbc = Channels.newChannel(website.openStream());

          FileOutputStream fos = new FileOutputStream(new File(localName));
          fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
     }
}

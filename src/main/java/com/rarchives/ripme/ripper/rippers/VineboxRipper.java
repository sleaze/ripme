package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rarchives.ripme.ripper.AlbumRipper;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Http;

public class VineboxRipper extends AlbumRipper {

    private static final String DOMAIN1 = "vinebox.co",
                                DOMAIN2 = "finebox.co",
                                HOST    = "finebox";

    public VineboxRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public boolean canRip(URL url) {
        return url.getHost().endsWith(DOMAIN1) || url.getHost().endsWith(DOMAIN2);
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return new URL("http://finebox.co/u/" + getGID(url));
    }

    @Override
    public void rip() throws IOException {
        int page = 0;
        Document doc;
        while (true) {
            page++;
            String urlPaged = this.url.toExternalForm() + "?page=" + page;
            logger.info("Retrieving " + urlPaged);
            sendUpdate(STATUS.LOADING_RESOURCE, urlPaged);
            try {
                doc = Http.url(this.url).get();
            } catch (HttpStatusException e) {
                logger.debug("Hit end of pages at page " + page, e);
                break;
            }
            for (Element element : doc.select("video")) {
                String srcUrl = element.attr("src");
                if (srcUrl == null) {
                    srcUrl = "";
                }
                if (element.attr("src") != null && element.attr("src").startsWith("/")) {
                    srcUrl = "http://" + DOMAIN2 + srcUrl;
                }
                System.out.println("YOYOYO srcUrl=" + srcUrl + "\n");
                addURLToDownload(new URL(srcUrl));
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("[!] Interrupted while waiting to load next page", e);
                break;
            }
        }
        waitForThreads();
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://(www\\.)?[vf]inebox\\.co/u/([a-zA-Z0-9]{1,}).*$");
        Matcher m = p.matcher(url.toExternalForm());
        if (!m.matches()) {
            throw new MalformedURLException("Expected format: http://vinebox.co/u/USERNAME or http://finebox.co/u/USERNAME");
        }
        return m.group(m.groupCount());
    }

}

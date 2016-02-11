package com.rarchives.ripme.ripper.rippers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rarchives.ripme.ripper.AlbumRipper;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;

public class XhamsterRipper extends AlbumRipper {

    private static final String HOST = "xhamster";

    private static final Pattern GID_PATTERN = Pattern.compile("^(?:https?://)?(?:[a-z0-9.]*?)xhamster\\.com/photos/(?:gallery|view)/([0-9]+)(/|-[0-9]+).*\\.html.*$");

    public XhamsterRipper(URL url) throws IOException { super(url); }

    @Override
    public boolean canRip(URL url) {
        Pattern p = Pattern.compile("^https?://[wmde.]*xhamster\\.com/photos/gallery/[0-9]+.*$");
        Matcher m = p.matcher(url.toExternalForm());
        return m.matches();
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        System.out.println(url.toString() + ": " + url.toExternalForm().indexOf("/photos/view/"));
        if (url.toString().indexOf("/photos/view/") != -1) {
            // Replace photo link with gallery link.
            System.out.println("Z in="+url.toString());
            //http://xhamster.com/photos/view/3667292-61338493.html#imgTop
            url = new URL(url.toString().replaceAll("/view/([0-9]+)-([0-9]+.*)", "/gallery/$1/$2"));
            System.out.println("Zout="+url.toString());
        }
        return url;
    }

    @Override
    public void rip() throws IOException {
        int index = 0;
        String nextURL = this.url.toExternalForm();
        while (nextURL != null) {
            logger.info("    Retrieving " + nextURL);
            Document doc = Http.url(nextURL).get();
            FileOutputStream htmlFile = new FileOutputStream(this.getSaveFileAs(new URL(nextURL), "", ""));
            htmlFile.write(doc.toString().getBytes());

            for (Element thumb : doc.select("table.iListing div.img img")) {
                if (!thumb.hasAttr("src")) {
                    continue;
                }
                String image = thumb.attr("src");
                image = image.replaceAll("http://p[0-9]*\\.", "http://up.");
                image = image.replaceAll("_160\\.", "_1000.");
                index += 1;
                String prefix = "";
                if (Utils.getConfigBoolean("download.save_order", true)) {
                    prefix = String.format("%03d_", index);
                }
                addURLToDownload(new URL(image), prefix);
                if (isThisATest()) {
                    break;
                }
            }
            if (isThisATest()) {
                break;
            }
            nextURL = null;
            for (Element element : doc.select("a.last")) {
                nextURL = element.attr("href");
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
        //System.out.println("hello, url=" + url.toExternalForm());
        Matcher m = GID_PATTERN.matcher(url.toExternalForm());
        if (m.matches()) {
            //System.out.println("m#1=" + m.group(1));
            return m.group(1);
        }
        throw new MalformedURLException(
            "Expected xhamster.com gallery formats: " +
            "http://xhamster.com/photos/gallery/#####/xxxxx.html or " +
            "http://xhamster.com/photos/view/#####-xxxxx.html but " +
            " got: " + url
        );
    }

}

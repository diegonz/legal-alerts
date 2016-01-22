package es.smartidea.android.legalalerts.boeHandler;

/**
 * @BoeXMLHandler
 *
 * Manages downloading BOE´s XML summary parse it looking for other links,
 * then downloads all BOE´s announcements & dispositions found in XML format too.
 * Also extracts documents paragraphs (<p> tags) to a HashMap<>,
 * and holds it to offer a query by containing text method.
 *
 * */

import android.support.annotation.NonNull;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import es.smartidea.android.legalalerts.okHttp.OkHttpGetURL;

public class BoeXMLHandler {

    public final static String BOE_BASE_URL = "http://www.boe.es";
    public final static String BOE_BASE_ID = "/diario_boe/xml.php?id=BOE-S-";
    private final static Pattern SPACE_REGEXP = Pattern.compile("\\s");

    private XmlPullParserFactory xmlFactoryObject;
    // String HashMap where raw data are stored
    private Map<String,String> boeXmlTodayRawData = new HashMap<>();
    // String HashMap where url of xml´s are stored
    public Map<String,String> urls = new HashMap<>();
    private String boeBaseURLString, currentBoeSummaryURLString;
    // XML error flag, only write-accessed from summary thread
    private boolean xmlError = false;

    /*
    * Public Constructor with @NonNull String argument
    * Creates new currentBoeSummaryURLString string with current date
    * or according to given String receivedDate (format yyyyMMdd)
    * */
    public BoeXMLHandler(@NonNull String receivedDate) {
        // Set null this listener
        this.boeXMLHandlerEvents = null;
        this.boeBaseURLString = BOE_BASE_URL;
        this.currentBoeSummaryURLString = BOE_BASE_URL + BOE_BASE_ID + receivedDate;
        Log.d("BOE", "SummaryURL: " + currentBoeSummaryURLString);
    }

    /*
    * Callback interface to enable async communication with parent object
    * This interface defines what type of messages can be communicated to owner object
    */
    public interface BoeXMLHandlerEvents {
        // Notify fetching summary completed sending error value
        void onBoeSummaryFetchCompleted(final boolean xmlSummaryError);

        // Notify fetching attachments completed
        void onBoeAttachmentsFetchCompleted();

        // Search completed, send result data
        void onSearchQueryCompleted(final int searchQueryResults,
                                    final String searchTerm,
                                    final boolean isLiteralSearch);

        // Send error tag data
        void onFoundXMLErrorTag(final String description);
    }

    // Assign the listener implementing events interface that will receive the events
    public void setBoeXMLHandlerEvents(BoeXMLHandlerEvents listener) {
        this.boeXMLHandlerEvents = listener;
    }

    // Listener un-setter/un-binder method.
    public void unsetBoeXMLHandlerEvents() {
        this.boeXMLHandlerEvents = null;
    }

    // This variable represents the listener passed in by the owning object
    // The listener must implement the events interface and passes messages up to the parent.
    private BoeXMLHandlerEvents boeXMLHandlerEvents;

    // Returns number of urlXml tags found (Announcements and disposals)
    public int getURLsCount() {
        return urls.size();
    }

    /**
     * boeXmlWorker is a void method to parse and store xml data
     * or download attached XML docs according to isSummary flag.
     *
     * @param boeParser BOE´s summary or attached doc associated XMLPullParser.
     * @param AttachmentUrlXml BOE´s document ID, can send null when parsing BOE´s summary.
     * @param isSummary boolean flag indicating type of document (summary or not).
     *
     * Attached doc URLs is stored on HashMap<String,String> urls.
     * Data is stored on HashMap<String,String> boeXmlTodayRawData.
    **/
    public void boeXmlWorker(XmlPullParser boeParser, String AttachmentUrlXml, boolean isSummary){
        // TODO: get docID substring(eachUrlXML.indexOf("=") + 1)
        int event;
        StringBuilder rawTextStringBuilder = new StringBuilder();
        String text = null;

        try {
            event = boeParser.getEventType();
            final Set<String> rawTextTags = new HashSet<>(
                    Arrays.asList("alerta", "materia", "p", "palabra", "titulo", "texto")
            );

            // New BoeUrlPair
            BoeUrlPair boeUrlPair = new BoeUrlPair();

            while (event != XmlPullParser.END_DOCUMENT) {
                String name = boeParser.getName();
                switch (event) {
                    case XmlPullParser.TEXT:
                        text = boeParser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (!isSummary && (rawTextTags.contains(name))){
                            rawTextStringBuilder.append(text);
                        } else if (isSummary){
                            if (name.equals("urlPdf")) { boeUrlPair.setUrlPdf(text); }
                            if (name.equals("urlXml")) { boeUrlPair.setUrlXml(text); }
                            if (name.equals("error")){
                                // Set XML error flag and notify listeners
                                xmlError = true;
                                boeXMLHandlerEvents.onFoundXMLErrorTag(text);
                            }
                        }
                        break;
                }
                event = boeParser.next();
            }
            if (!isSummary){
                boeXmlTodayRawData.put(AttachmentUrlXml, rawTextStringBuilder.toString());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    /*
    * boeRawDataQuery is a method to query/search data in the object.
    * returns a Map<String,String> with matching BOE´s PDF & XML URLs.
    */
    public Map<String, String> boeRawDataQuery(String searchQuery, boolean isLiteralSearch) {
        Map<String,String> resultUrls = new HashMap<>();
        if (isLiteralSearch){
            try {
                for (Map.Entry<String,String> eachBoe : boeXmlTodayRawData.entrySet()) {
                    if (normalizedStringFinder(eachBoe.getValue(), searchQuery)){
                        resultUrls.put(eachBoe.getKey(), searchQuery);
                    }
                }
                // Notify Listeners
                boeXMLHandlerEvents.onSearchQueryCompleted(resultUrls.size(), searchQuery, true);
            } catch (Exception e) {
                e.printStackTrace();
                boeXMLHandlerEvents.onSearchQueryCompleted(resultUrls.size(), searchQuery, true);
            }
        } else {
            // Split alert items by space with pre-compiled regexp
            String[] searchItemArray = SPACE_REGEXP.split(searchQuery);
            try {
                // For eachBoe item
                for (Map.Entry<String, String> eachBoe : boeXmlTodayRawData.entrySet()) {
                    // Flag that indicates every search query where successful
                    boolean hasAllSearchItems = true;
                    for (String eachSearchItem : searchItemArray) {
                        if (!normalizedStringFinder(eachBoe.getValue(), eachSearchItem)) {
                            // If item is not contained, set flag to false
                            hasAllSearchItems = false;
                        }
                    }
                    // Add Boe to result if "hasAllSearchItems"
                    if (hasAllSearchItems) {
                        resultUrls.put(eachBoe.getKey(), searchQuery);
                    }
                }
                boeXMLHandlerEvents.onSearchQueryCompleted(resultUrls.size(), searchQuery, false);
            } catch (Exception e) {
                Log.d("BOE", "ERROR while searching for: " + searchQuery);
                e.printStackTrace();
            }
        }
        return resultUrls;
    }

    /**
     * boolean method normalizedStringFinder search for searchItem in mainText
     * Normalizes and converts text to lower-case for comparing
     *
     * @param mainText String XML´s raw text, <titulo> and <p> tags
     *                 its normalized and converted to lower-case
     *                 for comparing purposes.
     * @param searchItem String item to search into mainText
     *                 its normalized and converted to lower-case
     *                 for comparing purposes.
     **/
    private static boolean normalizedStringFinder(String mainText, String searchItem){
        // TODO: Check alternatives like: org.apache.commons.lang3.StringUtils.containsIgnoreCase
        String normalizedMainText =
                Normalizer
                        .normalize(mainText, Normalizer.Form.NFD)
                        .replaceAll("[^\\p{ASCII}]", "");
        String normalizedSearchItem =
                Normalizer
                        .normalize(searchItem, Normalizer.Form.NFD)
                        .replaceAll("[^\\p{ASCII}]", "");
        return normalizedMainText.toLowerCase().contains(normalizedSearchItem.toLowerCase());
    }

    /*
    * fetchXMLSummary() runs a thread to fetch URLs for summary and others, based on OkHttp library
    */
    public void fetchXMLSummary() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                // Fetches XML´s summary URL and sends it to parse rawData URLs
                try {
                    InputStream boeSummaryStream = new OkHttpGetURL().run(currentBoeSummaryURLString);
                    xmlFactoryObject = XmlPullParserFactory.newInstance();
                    XmlPullParser boeSummaryParser = xmlFactoryObject.newPullParser();
                    boeSummaryParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

                    // Set xml´s encoding to latin1
                    boeSummaryParser.setInput(boeSummaryStream, "ISO-8859-1");

                    // Send parser, BOE´s document id set to null (summary)
                    // and isSummary flag set to true.
                    boeXmlWorker(boeSummaryParser, null, true);
                    boeSummaryStream.close();
                } catch (Exception e) {
                    Log.d("BOE", "ERROR while trying to download BOE´s summary!");
                    e.printStackTrace();
                }

                // Notifies summary fetching complete passing xmlError value
                boeXMLHandlerEvents.onBoeSummaryFetchCompleted(xmlError);
            }
        }).start();
    }

    /*
    * fetchXMLSummary() runs a thread to fetch URLs for summary and others, based on OkHttp library
    */
    public void fetchXMLAttachments() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Start fetch and parse thread if xmlError flag maintains its FALSE original state.
                if (!xmlError && !urls.isEmpty()) {
                    OkHttpGetURL okHttpGetURL = new OkHttpGetURL();
                    // Fetches each rawXML and passes each one to parse and store data
                    for (Map.Entry<String, String> eachUrlPair : urls.entrySet()){
                        try {
                            InputStream boeStream = okHttpGetURL.run(boeBaseURLString + eachUrlPair.getKey());
                            xmlFactoryObject = XmlPullParserFactory.newInstance();
                            XmlPullParser boeParser = xmlFactoryObject.newPullParser();
                            boeParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                            // Set xml´s encoding to latin1 (ISO-8859-1)
                            boeParser.setInput(boeStream, "ISO-8859-1");
                            // Send parser, BOE´s document id and isSummary flag set to false.
                            boeXmlWorker(boeParser, eachUrlPair.getKey(), false);
                            // Close Stream
                            boeStream.close();
                        } catch (Exception e) {
                            Log.d("BOE", "ERROR while trying to download BOE´s attachments!");
                            e.printStackTrace();
                        }
                    }
                    // Notify fetch completed to listener
                    boeXMLHandlerEvents.onBoeAttachmentsFetchCompleted();
                }
            }
        }).start();
    }

    /*
    * Inner class BoeUrlPair representing each pair of URLs (XML & PDF)
    * and boolean flag setters to manage storing in urls HashMap<String,String>
    */
    private class BoeUrlPair {

        private String urlXml, urlPdf;
        private boolean urlXmlFlag, urlPdfFlag;

        protected BoeUrlPair() {
            this.urlXmlFlag = false;
            this.urlPdfFlag = false;
            this.urlXml = null;
            this.urlPdf = null;
        }

        // If the other flag set to true (already have pair)
        public void setUrlXml(String urlXml) {
            if (this.urlPdfFlag) {
                this.urlXml = urlXml;
                saveUrlPair();
            } else {
                this.urlXml = urlXml;
                this.urlXmlFlag = true;
            }
        }

        // If the other flag set to true (already have pair)
        public void setUrlPdf(String urlPdf) {
            if (this.urlXmlFlag){
                this.urlPdf = urlPdf;
                saveUrlPair();
            } else {
                this.urlPdf = urlPdf;
                this.urlPdfFlag = true;
            }
        }

        // Method to store URLs and restore flags
        private void saveUrlPair(){
            urls.put(this.urlXml, this.urlPdf);
            this.urlPdfFlag = false;
            this.urlXmlFlag = false;
        }
    }
}
package es.smartidea.android.legalalerts.boeHandler;

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

/*
 * BoeXMLHandler class
 *
 * Manages downloading BOE´s XML summary parse it looking for other links,
 * then downloads all BOE´s announcements & dispositions found in XML format too.
 * Also extracts documents paragraphs (<p> tags) to a HashMap<>,
 * and holds it to offer a query by containing text method.
 *
 * */

public class BoeXMLHandler {

    public final static String BOE_BASE_URL = "http://www.boe.es";
    public final static String BOE_BASE_ID = "/diario_boe/xml.php?id=BOE-S-";
    private final static Pattern SPACE_REGEXP = Pattern.compile("\\s");

    private XmlPullParserFactory xmlFactoryObject;
    // String HashMaps where raw data are stored
    private Map<String,String> boeXmlRawTextData;
    // String HashMap where url of xml´s are stored
    public Map<String,String> urls;
    private String boeBaseURLString;
    private String[] currentBoeSummaryURLStrings;

    /**
     * Public constructor, it sets to null internal listener reference and
     * sets receivedDates elements as suffix to boeBaseURLStrings´ elements,
     * creating currentBoeSummaryURLStrings which references to BOE´s summary,
     * according to receivedDates.
     *
     * It also initializes other used variables.
     *
     * @param receivedDates VarArgs String[] containing dates in yyyyMMdd format
     */
    public BoeXMLHandler(@NonNull String... receivedDates) {
        // Set null this listener
        this.boeXMLHandlerEvents = null;

        this.boeXmlRawTextData = new HashMap<>();
        this.urls = new HashMap<>();


        this.boeBaseURLString = BOE_BASE_URL;
        this.currentBoeSummaryURLStrings = new String[receivedDates.length];
        for (int i = 0; i < receivedDates.length; i++) {
            this.currentBoeSummaryURLStrings[i] = BOE_BASE_URL + BOE_BASE_ID + receivedDates[i];
            Log.d("BOE", "SummaryURL: " + currentBoeSummaryURLStrings[i]);
        }

    }

    /*
    * Callback interface to enable async communication with parent object
    * This interface defines what type of messages can be communicated to owner object
    */
    public interface BoeXMLHandlerEvents {
        /**
         * Notify fetching summary completed sending error value
         */
        void onBoeSummariesFetchCompleted();

        /**
         * Notify fetching attachments completed
         */
        void onBoeAttachmentsFetchCompleted();

        /**
         * Notifies search completed successfully, sends result data to the listener
         * through defined parameters
         *
         * @param searchQueryResults number of results found for given searchTerm
         * @param searchTerm term which was used as pattern to search for
         * @param isLiteralSearch flag indicating if searchQuery has to be
         *                        searched literally or term by term splitting
         *                        by searchQuery words.
         */
        void onSearchQueryCompleted(final int searchQueryResults,
                                    final String searchTerm,
                                    final boolean isLiteralSearch);

        /**
         * Notifies and sends the associated description of BOE XML summary's error tag
         *
         * @param description associated description of BOE XML summary's error tag
         */
        void onFoundXMLErrorTag(final String description);
    }

    /**
     * Assign the listener implementing events interface that will receive the events
     * Binds given listener to internal (this) listener
     *
     * @param listener received listener to reference in (bind)
     */
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
     * Data is stored on HashMap<String,String> boeXmlRawTextData.
    **/
    public void boeXmlWorker(XmlPullParser boeParser, String AttachmentUrlXml, boolean isSummary){
        // TODO: get docID substring(eachUrlXML.indexOf("=") + 1)
        int event;

        try {
            event = boeParser.getEventType();
            final Set<String> rawTextTags = new HashSet<>(
                    Arrays.asList("alerta", "materia", "p", "palabra", "titulo", "texto")
            );

            StringBuilder rawTextStringBuilder = new StringBuilder();
            // New BoeUrlPair
            BoeUrlPair boeUrlPair = new BoeUrlPair();
            String name, text = null;

            while (event != XmlPullParser.END_DOCUMENT) {
                name = boeParser.getName();
                switch (event) {
                    case XmlPullParser.TEXT:
                        text = boeParser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (!isSummary){
                            if (rawTextTags.contains(name)) rawTextStringBuilder.append(text);
                        } else {
                            switch (name) {
                                case "urlPdf":
                                    boeUrlPair.setUrlPdf(text);
                                    break;
                                case "urlXml":
                                    boeUrlPair.setUrlXml(text);
                                    break;
                                case "error":
                                    boeXMLHandlerEvents.onFoundXMLErrorTag(text);
                                    break;
                            }
                        }
                        break;
                }
                event = boeParser.next();
            }
            if (!isSummary){
                boeXmlRawTextData.put(AttachmentUrlXml, rawTextStringBuilder.toString());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    /*
    * boeRawDataQuery is a method to query/search data in the object.
    * returns a Map<String,String> with matching BOE´s PDF & XML URLs.
    */

    /**
     * Query/search data in the object for given pattern searchQuery
     * handling if isLiteralSearch is enabled
     *
     * @param searchQuery search term to look for into downloaded data
     * @param isLiteralSearch flag indicating if searchQuery has to be
     *                        searched literally or term by term splitting
     *                        by searchQuery words.
     * @return Map containing matching BOE´s PDF & XML URLs.
     */
    public Map<String, String> boeRawDataQuery(String searchQuery, boolean isLiteralSearch) {

        Map<String,String> resultUrls = new HashMap<>();
        if (isLiteralSearch){
            try {
                for (Map.Entry<String,String> eachBoe : boeXmlRawTextData.entrySet()) {
                    if (isNormalizedStringContained(eachBoe.getValue(), searchQuery)){
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
                for (Map.Entry<String, String> eachBoe : boeXmlRawTextData.entrySet()) {
                    // Flag that indicates every search query where successful
                    boolean hasAllSearchItems = true;
                    for (String eachSearchItem : searchItemArray) {
                        if (!isNormalizedStringContained(eachBoe.getValue(), eachSearchItem)) {
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
     * Normalizes and converts text to lower-case to look for searchItem into mainText
     *
     * @param mainText String XML´s raw text, <titulo> and <p> tags
     *                 its normalized and converted to lower-case
     *                 for comparing purposes.
     * @param searchItem String item to search into mainText
     *                 its normalized and converted to lower-case
     *                 for comparing purposes.
     * @return boolean value if second parameter is contained onto first
     **/
    private static boolean isNormalizedStringContained(String mainText, String searchItem){
        // TODO: Check alternatives like: org.apache.commons.lang3.StringUtils.containsIgnoreCase
        String normalizedMainText =
                Normalizer.normalize(mainText, Normalizer.Form.NFD)
                        .replaceAll("[^\\p{ASCII}]", "");
        String normalizedSearchItem =
                Normalizer.normalize(searchItem, Normalizer.Form.NFD)
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

                OkHttpGetURL okHttpGetURL = new OkHttpGetURL();
                for (String currentBoeSummaryURLString : currentBoeSummaryURLStrings) {
                    // Fetches XML´s summarys URLs and sends it to parse rawData URLs
                    try {
                        InputStream boeSummaryStream = okHttpGetURL.run(currentBoeSummaryURLString);
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

                }
                // Notifies summaries fetching complete passing xmlError value
                boeXMLHandlerEvents.onBoeSummariesFetchCompleted();
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
                if (!urls.isEmpty()) {
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
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
    private final static Pattern NOT_ASCII_REGEXP = Pattern.compile("[^\\p{ASCII}]");

    private XmlPullParserFactory xmlFactoryObject;
    private String[] boeSummariesURLStrings;

    final static Set<String> rawTextTags = new HashSet<>(
            Arrays.asList("alerta", "materia", "p", "palabra", "titulo", "texto")
    );


    /**
     * Public constructor, it sets to null internal listener reference and
     * sets receivedDates elements as suffix to boeBaseURLStrings´ elements,
     * creating boeSummariesURLStrings which references to BOE´s summary,
     * according to receivedDates.
     *
     * It also initializes other used variables.
     *
     * @param receivedDates VarArgs String[] containing dates in yyyyMMdd format
     */
    public BoeXMLHandler(@NonNull String... receivedDates) {
        // Set null this listener
        this.boeXMLHandlerEvents = null;
        this.boeSummariesURLStrings = new String[receivedDates.length];
        for (int i = 0; i < receivedDates.length; i++) {
            this.boeSummariesURLStrings[i] = BOE_BASE_URL + BOE_BASE_ID + receivedDates[i];
            Log.d("BOE", "SummaryURL: " + boeSummariesURLStrings[i]);
        }
    }

    /*
    * Callback interface to enable async communication with parent object
    * This interface defines what type of messages can be communicated to owner object
    */
    public interface BoeXMLHandlerEvents {
        /**
         * Notifies search completed successfully, sends result data to the listener
         * through defined parameters
         *
         * @param searchResults Map containing search results as url of XML as key
         *                      and search term as value.
         */
        void onWorkCompleted(final Map<String, String> searchResults,
                             final Map<String, String> xmlPdfUrls);
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

    /**
     * Starts fetching summaries and attachments and search for every item received
     *
     * @param alertsListFullData Map String,Boolean containing search items and literal
     *                           search flag.
     */
    public void startFetchAndSearch(@NonNull Map<String, Boolean> alertsListFullData) {
        if (!alertsListFullData.isEmpty()) {
            OkHttpGetURL okHttpGetURL = new OkHttpGetURL();
            Map<String, String> xmlPdfUrls = fetchXMLSummaries(okHttpGetURL);
            // If there is any attachment
            if (!xmlPdfUrls.isEmpty()){
                // Send search results to listener
                boeXMLHandlerEvents.onWorkCompleted(
                        fetchAttachmentsAndSearch(okHttpGetURL, xmlPdfUrls, alertsListFullData),
                        xmlPdfUrls);
            } else {
                boeXMLHandlerEvents.onWorkCompleted(
                        new HashMap<String, String>(0),
                        new HashMap<String, String>(0)
                );
            }
        }
    }

    /**
     * boeXmlWorker is a void method to parse and store xml data
     * or download attached XML docs according to isSummary flag.
     *
     * @param boeParser BOE´s summary or attached doc associated XMLPullParser.
     * @param attachmentUrlXml BOE´s document ID, can send null when parsing BOE´s summary.
     *
     * Attached doc URLs is stored on HashMap<String,String> urls.
     * Data is stored on HashMap<String,String> boeXmlRawTextData.
    **/
    private static Map<String,String> boeXmlWorker(@NonNull XmlPullParser boeParser,
                                           String attachmentUrlXml) {
        // TODO: get docID substring(eachUrlXML.indexOf("=") + 1)
        int event;

        try {
            event = boeParser.getEventType();

            StringBuilder rawTextStringBuilder;
            Map<String,String> resultDataMap;
            // If is summary (attachmentUrlXml == null)
            if (attachmentUrlXml == null){
                rawTextStringBuilder = new StringBuilder(0);
                resultDataMap = new HashMap<>();
            } else {
                rawTextStringBuilder = new StringBuilder(0);
                resultDataMap = new HashMap<>(1);
            }

            String name, text = null, tempUrl = null;

            while (event != XmlPullParser.END_DOCUMENT) {
                name = boeParser.getName();
                switch (event) {
                    case XmlPullParser.TEXT:
                        text = boeParser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (attachmentUrlXml == null){
                            switch (name) {
                                case "urlPdf":
                                    tempUrl = text;
                                    break;
                                case "urlXml":
                                    // If tempUrl not null (urlPdf exists)
                                    if (tempUrl != null) {
                                        resultDataMap.put(text, tempUrl);
                                        // Set tempUrl to null again
                                        tempUrl = null;
                                    }
                                    break;
                            }
                        } else if (rawTextTags.contains(name)) rawTextStringBuilder.append(text);
                        break;
                }
                event = boeParser.next();
            }
            if (attachmentUrlXml != null) {
                resultDataMap.put(attachmentUrlXml, rawTextStringBuilder.toString());
            }
            // Return results
            return resultDataMap;
        } catch (Exception e) { e.printStackTrace(); }
        // return empty map after error
        return new HashMap<>(0);
    }

    /**
     * Query/search data in the object for given pattern searchQuery
     * handling if isLiteralSearch is enabled.
     *
     * @param searchQuery search term to look for into downloaded data
     * @param isLiteralSearch flag indicating if searchQuery has to be
     *                        searched literally or term by term splitting
     *                        by searchQuery words.
     * @return Map containing matching BOE´s PDF & XML URLs.
     */
    private static Map<String,String> boeRawDataQuery(@NonNull String searchQuery,
                                                     boolean isLiteralSearch,
                                                     @NonNull String rawText,
                                                     @NonNull String urlXml) {

        Map<String, String> resultUrls = new HashMap<>(0);
        if (isLiteralSearch){
            try {
                if (isNormalizedStringContained(rawText, searchQuery)){
                    resultUrls.put(urlXml, searchQuery);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                // Split alert items by space with pre-compiled regexp
                String[] searchItemArray = SPACE_REGEXP.split(searchQuery);
                // Flag that indicates every search query where successful
                boolean hasAllSearchItems = true;
                for (String eachSearchItem : searchItemArray) {
                    // If item is not contained, set flag to false
                    if (!isNormalizedStringContained(rawText, eachSearchItem)) hasAllSearchItems = false;
                }
                // Add Boe to result if "hasAllSearchItems"
                if (hasAllSearchItems) resultUrls.put(urlXml, searchQuery);
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
        String normalizedMainText = NOT_ASCII_REGEXP
                .matcher(Normalizer.normalize(mainText, Normalizer.Form.NFD))
                .replaceAll("");
        String normalizedSearchItem = NOT_ASCII_REGEXP
                .matcher(Normalizer.normalize(searchItem, Normalizer.Form.NFD))
                .replaceAll("");

        return normalizedMainText.toLowerCase().contains(normalizedSearchItem.toLowerCase());
    }

    /*
    * fetchXMLSummaries() fetches URLs for summary and others, based on OkHttp library
    */
    private Map<String, String> fetchXMLSummaries(OkHttpGetURL okHttpGetURL){
        Map<String, String> urlPairs = new HashMap<>();
        InputStream boeSummaryStream = null;
        for (String summaryURLString : boeSummariesURLStrings) {
            // Fetches XML´s summaries URLs and sends it to parse rawData URLs
            try {
                boeSummaryStream = okHttpGetURL.run(summaryURLString);
                xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser boeSummaryParser = xmlFactoryObject.newPullParser();
                boeSummaryParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

                // Set xml´s encoding to latin1
                boeSummaryParser.setInput(boeSummaryStream, "ISO-8859-1");
                // return parsed document data, sending to parser BOE id set to null (summary)
                urlPairs.putAll(boeXmlWorker(boeSummaryParser, null));
            } catch (Exception e) {
                Log.d("BOE", "ERROR while trying to download BOE´s summary!");
                e.printStackTrace();
            } finally {
                if (boeSummaryStream != null){
                    try {
                        // Close Stream
                        boeSummaryStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return urlPairs;
    }

    /*
    * fetchAttachmentsAndSearch()
    */
    private Map<String, String> fetchAttachmentsAndSearch(@NonNull OkHttpGetURL okHttpGetURL,
                                                         @NonNull Map<String, String> mUrls,
                                                         @NonNull Map<String, Boolean> searchTerms){
        // Fetches each rawXML and passes each one to parse and store data
        Map<String, String> searchResults = new HashMap<>();
        Map<String, String> rawTextData = new HashMap<>(1);
        InputStream boeStream = null;
        for (Map.Entry<String, String> eachUrlPair : mUrls.entrySet()){
            try {
                // Reset map if its not empty
                if (!rawTextData.isEmpty()) rawTextData.clear();

                boeStream = okHttpGetURL.run(BOE_BASE_URL + eachUrlPair.getKey());
                xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser boeParser = xmlFactoryObject.newPullParser();
                boeParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                // Set xml´s encoding to latin1 (ISO-8859-1)
                boeParser.setInput(boeStream, "ISO-8859-1");
                // Send parser, BOE´s document id to enable not-summary features.
                rawTextData.putAll(boeXmlWorker(boeParser, eachUrlPair.getKey()));
                Map.Entry<String,String> rawTextEntry = rawTextData.entrySet().iterator().next();
                for (Map.Entry<String, Boolean> eachAlert : searchTerms.entrySet()) {
                    searchResults.putAll(
                            boeRawDataQuery(
                                    eachAlert.getKey(),
                                    eachAlert.getValue(),
                                    rawTextEntry.getValue(),
                                    rawTextEntry.getKey()
                            )
                    );
                }
            } catch (Exception e) {
                Log.d("BOE", "ERROR while trying to download BOE´s attachments!");
                e.printStackTrace();
            } finally {
                if (boeStream != null){
                    try {
                        // Close Stream
                        boeStream.close();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }

        // Return results, can be empty
        return searchResults;
    }
}
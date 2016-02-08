package es.smartidea.android.legalalerts.alerts.alertsServices.boeHandler;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import es.smartidea.android.legalalerts.okHttp.OkHttpGetURL;
import es.smartidea.android.legalalerts.textUtils.TextSearchUtils;

/*
 * BoeXMLHandler class
 *
 * Manages downloading BOE´s XML summary parse it looking for other links,
 * then downloads all BOE´s announcements & dispositions found in XML format too.
 * Also extracts documents paragraphs (<p> tags) to a HashMap<>,
 * and holds it to offer a query by containing text method.
 *
 * */

@SuppressWarnings("StringConcatenationMissingWhitespace")
public class BoeXMLHandler {

    // BOE base string tokens
    public final static String BOE_BASE_URL = "http://www.boe.es";
    public final static String BOE_BASE_ID = "/diario_boe/xml.php?id=BOE-S-";

    private XmlPullParserFactory xmlFactoryObject;
    private String[] boeSummariesURLStrings;
    private String todayDateString;

    /**
     * Set of strings containing all XML tags to be gathered
     * for each attached document to any BOE summary
     */
    @SuppressWarnings("SpellCheckingInspection")
    private final static Set<String> rawTextTags =
            new HashSet<>(Arrays.asList("alerta", "materia", "p", "palabra", "titulo", "texto"));

    /**
    * This variable represents the listener passed in by the owning object,
    * the observer must implement the events interface and passes messages up to the parent
    * */
    private BoeXMLHandlerEvents boeXMLHandlerEvents;

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
        // Get today´s date in string format
        this.todayDateString = receivedDates[receivedDates.length - 1];
    }

    /**
    * Inner callback interface class to enable async communication with parent object
    * This interface defines what type of messages can be communicated to owner object
    */
    public interface BoeXMLHandlerEvents {
        /**
         * Notifies search completed successfully, sends result data to the listener
         * through defined parameters
         *
         * @param searchResults Map containing search results as url of XML as key
         *                      and search term as value.
         * @param xmlPdfUrls    Map containing XML and PDF urls for each attachment document
         */
        void onWorkCompleted(final Map<String, String> searchResults,
                             final Map<String, String> xmlPdfUrls);

        /**
         * Notifies summary fetching completed successfully, sending info about today sync status
         *
         * @param todaySyncResultOK boolean flag indicating today´s boe summary
         *                          was fetched ok and with contents
         */
        void todaySummaryResultOK(final boolean todaySyncResultOK);
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

    /**
     * Binds to null inner BoeXMLHandler reference
     */
    public void unsetBoeXMLHandlerEvents() {
        this.boeXMLHandlerEvents = null;
    }

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
                boeXMLHandlerEvents.onWorkCompleted(xmlPdfUrls, xmlPdfUrls);
            }
        }
    }

    /**
     * Fetches URLs for summary and others using custom OkHttp object
     *
     * @param okHttpGetURL  Custom OkHttp implementation object instance
     * @return  Map of String,String containing pairs (PDF and XML)
     * of BOE´s summary attached documents
     */
    private Map<String, String> fetchXMLSummaries(OkHttpGetURL okHttpGetURL){
        //noinspection CollectionWithoutInitialCapacity
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
                int sizeBefore = urlPairs.size();
                // return parsed document data, sending to parser BOE id set to null (summary)
                urlPairs.putAll(boeXmlWorker(boeSummaryParser, null));
                // TODO check void summary check implementation
                if (summaryURLString.equals(BOE_BASE_URL + BOE_BASE_ID + todayDateString)){
                    boeXMLHandlerEvents.todaySummaryResultOK(urlPairs.size() > sizeBefore);
                }
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

    /**
     * Fetches BOE´s summary attached documents using custom OkHttp object
     * and searches in each document looking for received search terms.
     *
     * @param okHttpGetURL  Custom OkHttp implementation object instance
     * @param urlPairs  Map of String,String containing pairs (PDF and XML)
     *                  of BOE´s summary attached documents
     * @param searchTerms   MAp containing search terms an
     *                      its associated "literal search" flag
     * @return  Map containing each search term that was found
     * at least once and its associated PDF url
     */
    private Map<String, String> fetchAttachmentsAndSearch(@NonNull OkHttpGetURL okHttpGetURL,
                                                         @NonNull Map<String, String> urlPairs,
                                                         @NonNull Map<String, Boolean> searchTerms){
        //noinspection CollectionWithoutInitialCapacity
        Map<String, String> searchResults = new HashMap<>();
        Map<String, String> rawTextData = new HashMap<>(1);
        InputStream boeStream = null;
        for (Map.Entry<String, String> eachUrlPair : urlPairs.entrySet()){
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
                            TextSearchUtils.rawDataSearchQuery(
                                    eachAlert.getKey(),
                                    eachAlert.getValue(),
                                    rawTextEntry.getValue(),
                                    rawTextEntry.getKey()
                    ));
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

    /**
     * Void static method to parse and store xml data or download attached XML docs
     * according to given @Nullable String attachmentUrlXml.
     *
     * @param boeParser        XMLPullParser object associated with current
     *                         BOE´s summary or attached doc.
     * @param attachmentUrlXml @Nullable BOE´s document ID, can receive null
     *                         when parsing BOE´s summary.
     *                         <p>
     *                         Attached doc URLs is stored on HashMap<String,String> urls.
     *                         Data is stored on HashMap<String,String> boeXmlRawTextData.
     **/
    private static Map<String, String> boeXmlWorker(@NonNull XmlPullParser boeParser,
                                                    @Nullable String attachmentUrlXml) {
        try {
            int event = boeParser.getEventType();

            StringBuilder rawTextStringBuilder;
            Map<String, String> resultDataMap;
            // If is summary (attachmentUrlXml == null)
            //noinspection VariableNotUsedInsideIf
            if (attachmentUrlXml == null) {
                rawTextStringBuilder = new StringBuilder(0);
                //noinspection CollectionWithoutInitialCapacity
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
                        //noinspection VariableNotUsedInsideIf
                        if (attachmentUrlXml == null) {
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
        } catch (Exception e) {
            e.printStackTrace();
            // return empty map after error recovery
            return new HashMap<>(0);
        }
    }
}
package es.smartidea.android.legalalerts.services.boeHandler;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import es.smartidea.android.legalalerts.network.NetWorker;
import es.smartidea.android.legalalerts.utils.FileLogger;
import es.smartidea.android.legalalerts.utils.TextSearchUtils;

/*
 * BoeHandler class
 *
 * Manages downloading BOE´s XML summary parse it looking for other links,
 * then downloads all BOE´s announcements & dispositions found in XML format too.
 * Also extracts documents paragraphs (<p> tags) to a HashMap<>,
 * and holds it to offer a query by containing text method.
 *
 * */
@SuppressWarnings("StringConcatenationMissingWhitespace")
public class BoeHandler {

    private final static String LOG_TAG = "BoeHandler";

    public final static String BOE_BASE_URL = "http://www.boe.es";
    public final static String BOE_BASE_ID = "/diario_boe/xml.php?id=BOE-S-";
    @SuppressWarnings("SpellCheckingInspection")
    private final static Set<String> searchableTextTags =
            new HashSet<>(Arrays.asList("alerta", "materia", "p", "palabra", "titulo", "texto"));
    private XmlPullParserFactory xmlPullParserFactory;
    private String[] summariesUrlStringArray;
    private Map<String, Boolean> alertsMap;
    private BoeListener boeListener;

    /**
     * Public constructor, it sets to null internal listener reference
     */
    public BoeHandler() {
        this.boeListener = null;
    }

    /**
     * <p>
     * Sets receivedDates elements as suffix to boeBaseURLStrings´ elements,
     * creating summariesUrlStringArray which references to BOE´s summary,
     * according to receivedDates.
     *
     * It also initializes other used variables.
     * <p/>
     *
     * @param alertsMap String Boolean map representing alerts to search
     * @param receivedDates VarArgs String[] containing dates in yyyyMMdd format
     */
    public void setAlertsAndDates(@NonNull Map<String, Boolean> alertsMap,
                                  @NonNull String... receivedDates) {
        this.alertsMap = alertsMap;
        summariesUrlStringArray = new String[receivedDates.length];
        for (int i = 0; i < receivedDates.length; i++) {
            this.summariesUrlStringArray[i] = BOE_BASE_URL + BOE_BASE_ID + receivedDates[i];
            FileLogger.logToExternalFile(LOG_TAG + " - SummaryURL: " + summariesUrlStringArray[i]);
        }
    }

    /**
     * Starts fetching summaries and attachments and search for every item received
     * sending work results via listener interface
     */
    public void start() {
        if (!alertsMap.isEmpty()) {
            Map<String, String> resultMap, xmlPdfUrls;
            xmlPdfUrls = handleSummaries(new NetWorker());
            if (!xmlPdfUrls.isEmpty()){
                resultMap = handleAttachments(new NetWorker(), xmlPdfUrls, alertsMap);
            } else {
                resultMap = xmlPdfUrls;
            }
            boeListener.onWorkCompleted(resultMap, xmlPdfUrls);
        }
    }

    /**
     * Fetches URLs for summary and others using custom OkHttp object
     *
     * @param netWorker  Custom OkHttp implementation object instance
     * @return  Map of String,String containing pairs (PDF and XML)
     * of BOE´s summary attached documents
     */
    private Map<String, String> handleSummaries(@NonNull NetWorker netWorker){
        //noinspection CollectionWithoutInitialCapacity
        Map<String, String> urlPairs = new HashMap<>();
        InputStream boeSummaryStream = null;
        for (String summaryURLString : summariesUrlStringArray) {
            // Fetches XML´s summaries URLs and sends it to parse rawData URLs
            try {
                boeSummaryStream = netWorker.getUrlAsByteStream(summaryURLString);
                xmlPullParserFactory = XmlPullParserFactory.newInstance();
                XmlPullParser boeSummaryParser = xmlPullParserFactory.newPullParser();
                boeSummaryParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

                // Set xml´s encoding to latin1
                boeSummaryParser.setInput(boeSummaryStream, "ISO-8859-1");
                int sizeBefore = urlPairs.size();
                // return parsed document data, sending to parser BOE id set to null (summary)
                urlPairs.putAll(getDataFromXML(boeSummaryParser, null));
                // If fetching summary was ok,  notify listener to update last successful sync date
                if (urlPairs.size() > sizeBefore){
                    // Extract date from url, getting a substring from position 46
                    // Example URL: http://www.boe.es/diario_boe/xml.php?id=BOE-S-yyyyMMdd
                    boeListener.onSummaryFetchSuccess(summaryURLString.substring(46));
                }
            } catch (Exception e) {
                // Log to file for debugging
                FileLogger.logToExternalFile(LOG_TAG + " - ERROR while trying to download BOE´s summary!");

                e.printStackTrace();
            } finally {     // Close the stream
                if (boeSummaryStream != null){
                    try {boeSummaryStream.close();} catch (Exception e) {e.printStackTrace();}
                }
            }
        }
        return urlPairs;
    }

    /**
     * Fetches BOE´s summary attached documents using custom OkHttp object
     * and searches in each document looking for received search terms.
     *
     * @param netWorker  Custom OkHttp implementation object instance
     * @param urlPairs  Map of String,String containing pairs (PDF and XML)
     *                  of BOE´s summary attached documents
     * @param searchTerms   MAp containing search terms an
     *                      its associated "literal search" flag
     * @return  Map containing each search term that was found
     * at least once and its associated PDF url
     */
    private Map<String, String> handleAttachments(@NonNull NetWorker netWorker,
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

                boeStream = netWorker.getUrlAsByteStream(BOE_BASE_URL + eachUrlPair.getKey());
                xmlPullParserFactory = XmlPullParserFactory.newInstance();
                XmlPullParser boeParser = xmlPullParserFactory.newPullParser();
                boeParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                // Set xml´s encoding to latin1 (ISO-8859-1)
                boeParser.setInput(boeStream, "ISO-8859-1");
                // Send parser, BOE´s document id to enable not-summary features. TODO Check implementation
                rawTextData.putAll(getDataFromXML(boeParser, eachUrlPair.getKey()));
                Map.Entry<String,String> rawTextEntry = rawTextData.entrySet().iterator().next();
                for (Map.Entry<String, Boolean> eachAlert : searchTerms.entrySet()) {
                    // TODO Decouple Search functionality
                    searchResults.putAll(
                            TextSearchUtils.rawDataSearchQuery(
                                    eachAlert.getKey(), eachAlert.getValue(),
                                    rawTextEntry.getValue(), rawTextEntry.getKey()
                            )
                    );
                }
            } catch (Exception e) {
                FileLogger.logToExternalFile(LOG_TAG + " ERROR while trying to download BOE´s attachments!");
                e.printStackTrace();
            } finally {
                if (boeStream != null){
                    try {
                        boeStream.close();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
        return searchResults;
    }

    /**
     * Void static method to parse and store xml data or download attached XML docs
     * according to given @Nullable String attachmentUrlXml, when passed null
     * document is handled as a Boe Summary.
     *
     * @param boeParser        XMLPullParser object associated with current
     *                         BOE´s summary or attached doc.
     * @param attachmentUrlXml @Nullable BOE´s document ID, can receive null
     *                         when parsing BOE´s summary.
     *                         <p>
     *                         Attached doc URLs is stored on HashMap<String,String> urls.
     *                         Data is stored on HashMap<String,String> boeXmlRawTextData.
     **/
    private static Map<String, String> getDataFromXML(@NonNull XmlPullParser boeParser,
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
                        } else if (searchableTextTags.contains(name)) rawTextStringBuilder.append(text);
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

    /**
     * Inner callback interface class to enable async communication with parent object
     * This interface defines what type of messages can be communicated to owner object
     */
    public interface BoeListener {
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
         * Notifies summary has been processed successfully , sending a string
         * representing those summary date
         *
         * @param summaryDateInString string representing summary´s date
         *                            that has been processed ok
         */
        void onSummaryFetchSuccess(final String summaryDateInString);
    }

    /**
     * Assign the listener implementing events interface that will receive the events
     * Binds given listener to internal (this) listener
     *
     * @param listener received listener to reference in (bind)
     */
    public void setListener(BoeListener listener) {
        this.boeListener = listener;
    }

    /**
     * Binds to null inner BoeHandler reference
     */
    public void unsetListener() {
        this.boeListener = null;
    }

}
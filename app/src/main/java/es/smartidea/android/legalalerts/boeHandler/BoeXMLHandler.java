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

import android.annotation.SuppressLint;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.smartidea.android.legalalerts.okHttp.OkHttpGetURL;

public class BoeXMLHandler {

    private XmlPullParserFactory xmlFactoryObject;

    // XML error flag
    // Only write-accessed from summary thread
    public boolean xmlError = false;

    public final static String BOE_BASE_URL = "http://www.boe.es";
    public final static String BOE_BASE_ID = "/diario_boe/xml.php?id=BOE-S-";

    // String List where url of xml´s are stored
    private List<String> urlXMLs = new ArrayList<>();
    private String boeBaseURLString, currentBoeSummaryURLString;

    // HashMap where raw data are stored
    private Map<String, String> boeXmlTodayRawData = new HashMap<>();

    // Public Constructor with empty arguments
    // Creates new BoeXMLHandler object with current date (yyyyMMdd)
    public BoeXMLHandler() {

        // Set null or default listener
        this.boeXMLHandlerEvents = null;

        // Setup base data
        boeSetup();
    }

    @SuppressLint("SimpleDateFormat")
    private void boeSetup() {
        Date curDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String todayDateString = dateFormat.format(curDate);
        final String currentBoeURL = BOE_BASE_ID + todayDateString;

//        String fakeTodayDate = "20160114";
//        final String currentBoeURL = BOE_BASE_ID + fakeTodayDate;

        this.boeBaseURLString = BOE_BASE_URL;
        this.currentBoeSummaryURLString = BOE_BASE_URL + currentBoeURL;

        Log.d("BOE", "SummaryURL: " + currentBoeSummaryURLString);
    }

    // Callback interface to enable async communication with parent object
    // This interface defines the type of messages I want to communicate to owner object
    public interface BoeXMLHandlerEvents {
        void onBoeSummaryFetchCompleted(boolean xmlSummaryError);

        void onBoeAttachmentsFetchCompleted();

        void onSearchQueryCompleted(int searchQueryResults, String searchTerm, boolean isLiteralSearch);

        void onFoundXMLErrorTag(String description);
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
    public int getURLXMLsCount() {
        return urlXMLs.size();
    }

    /**
     * boeXmlWorker is a void method to parse and store xml data
     * or download attached XML docs according to isSummary flag.
     *
     * @param boeParser BOE´s summary or attached doc associated XMLPullParser.
     * @param docID BOE´s document ID, can send null when parsing BOE´s summary.
     * @param isSummary boolean flag indicating type of document (summary or not).
     *
     * Attached doc URLs is stored on List<String> urlXMLs.
     * Data is stored on HashMap<String, String> boeXmlTodayRawData.
    **/
    public void boeXmlWorker(XmlPullParser boeParser, String docID, boolean isSummary){
        int event;
        StringBuilder rawTextStringBuilder = new StringBuilder();
        String text = null;

        try {
            event = boeParser.getEventType();
            final Set<String> rawTextTags = new HashSet<>(Arrays.asList("p", "titulo", "palabra"));

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
                            if (name.equals("urlXml")){ urlXMLs.add(text); }
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
            boeXmlTodayRawData.put(docID, rawTextStringBuilder.toString());
        } catch (Exception e) {
            Log.d("BOE", "ERROR while parsing attached XML file!");
            e.printStackTrace();
        }
    }

    /*
    * boeRawDataQuery is a method to query/search data in the object.
    * It returns a list<> with matching BOE´s ids.
    */
    // TODO Review/re-do search query method execution
    public List<String> boeRawDataQuery(String searchQuery, boolean isLiteralSearch) {
        List<String> queryResults = new ArrayList<>();
        if (isLiteralSearch){
            try {
                for (Map.Entry<String, String> eachBoe : boeXmlTodayRawData.entrySet()) {
                    if (normalizedStringSearch(eachBoe.getValue(), searchQuery)){
                        queryResults.add(eachBoe.getKey());
                    }
                }
                // Notify Listeners
                boeXMLHandlerEvents.onSearchQueryCompleted(queryResults.size(), searchQuery, true);
                return queryResults;
            } catch (Exception e) {
                Log.d("BOE", "ERROR while searching for: " + searchQuery);
                e.printStackTrace();
                // Notify Listeners
                boeXMLHandlerEvents.onSearchQueryCompleted(queryResults.size(), searchQuery, true);
                return queryResults;
            }
        } else {
            // Split alert items by space
            String[] searchItemArray = searchQuery.split("\\s");
            try {
                // For eachBoe item
                for (Map.Entry<String, String> eachBoe : boeXmlTodayRawData.entrySet()) {
                    // Flag that indicates every search query where successful
                    boolean hasAllSearchItems = true;
                    for (String eachSearchItem : searchItemArray) {
                        if (!normalizedStringSearch(eachBoe.getValue(), eachSearchItem)) {
                            // If item is not contained, set flag to false
                            hasAllSearchItems = false;
                        }
                    }
                    // Add Boe to result if "hasAllSearchItems"
                    if (hasAllSearchItems) {
                        queryResults.add(eachBoe.getKey());
                    }
                }
                // Notify Listeners
                boeXMLHandlerEvents.onSearchQueryCompleted(queryResults.size(), searchQuery, false);
                return queryResults;
            } catch (Exception e) {
                Log.d("BOE", "ERROR while searching for: " + searchQuery);
                e.printStackTrace();
                // Notify Listeners
                boeXMLHandlerEvents.onSearchQueryCompleted(queryResults.size(), searchQuery, false);
                return queryResults;
            }
        }
    }

    /**
     * Boolean class normalizedStringSearch search for searchItem in mainText
     * Normalizes and converts text to lower-case for comparing
     *
     * @param mainText String XML´s raw text, <titulo> and <p> tags
     *                 its normalized and converted to lower-case
     *                 for comparing purposes.
     * @param searchItem String item to search into mainText
     *                 its normalized and converted to lower-case
     *                 for comparing purposes.
     **/
    private boolean normalizedStringSearch(String mainText, String searchItem){
        // TODO: Check: org.apache.commons.lang3.StringUtils.containsIgnoreCase
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
        Thread fetchThread = new Thread(new Runnable() {
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
        });
        fetchThread.start();
    }

    /*
* fetchXMLSummary() runs a thread to fetch URLs for summary and others, based on OkHttp library
*/
    public void fetchXMLAttachments() {
        Thread fetchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Start fetch and parse thread if xmlError flag maintains its FALSE original state.
                if (!xmlError && !urlXMLs.isEmpty()) {
                    // Fetches each rawXML and passes each one to parse and store data
                    for (String eachUrlXML : urlXMLs) {
                        try {
                            InputStream boeStream = new OkHttpGetURL().run(boeBaseURLString + eachUrlXML);
                            xmlFactoryObject = XmlPullParserFactory.newInstance();
                            XmlPullParser boeParser = xmlFactoryObject.newPullParser();
                            boeParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

                            // Set xml´s encoding to latin1 (ISO-8859-1)
                            boeParser.setInput(boeStream, "ISO-8859-1");

                            // Send parser, BOE´s document id and isSummary flag set to false.
                            boeXmlWorker(boeParser, eachUrlXML.substring(eachUrlXML.indexOf("=") + 1), false);
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
        });
        fetchThread.start();
    }

}
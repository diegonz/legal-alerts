package es.smartidea.android.legalalerts.boeHandler;

/**
 * @BoeXMLHandler
 *
 * Manages downloading BOE´s XML summary parse it looking for other links,
 * then downloads all BOE´s announcements & dispositions found in XML format too.
 * Also extracts documents paragraphs (<p> tags) to a HashMap<>,
 * and holds it to offer a query by containing text method.
 *
 * BOE´s summary XML tag structure:
 * Diario >
 * Seccion >
 * Departamento >
 * Epigrafe >
 * Item >
 * <urlXml>
 * */

import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoeXMLHandler {

    private XmlPullParserFactory xmlFactoryObject;

    final String BOE_BASE_URL = "http://www.boe.es";
    final String BOE_BASE_ID = "/diario_boe/xml.php?id=BOE-S-";

    // String List where url of xml´s are stored
    private List<String> urlXMLs = new ArrayList<>();
    private String boeBaseURLString, currentBoeSummaryURLString;

    // HashMap where raw data are stored
    private Map<String, String> boeXmlTodayRawData = new HashMap<>();

    // Public Constructor with empty arguments
    // Creates new BoeXMLHandler object with current date (yyyyMMdd)
    public BoeXMLHandler() {

        // Set null or default listener or accept as argument to constructor
        this.boeXMLHandlerEvents = null;

        // Setup base data
        boeSetup();
    }

    // Callback interface to enable async communication with parent object
    // This interface defines the type of messages I want to communicate to my owner
    public interface BoeXMLHandlerEvents {
        void onBoeFetchCompleted();

        void onSearchQueryCompleted(int searchQueryResults, String searchTerm);

        void onFoundXMLErrorTag(String description);
    }


    // Assign the listener implementing events interface that will receive the events
    public void setBoeXMLHandlerEvents(BoeXMLHandlerEvents listener) {
        this.boeXMLHandlerEvents = listener;
    }

    public void unsetBoeXMLHandlerEvents() {
        this.boeXMLHandlerEvents = null;
    }

    // This variable represents the listener passed in by the owning object
    // The listener must implement the events interface and passes messages up to the parent.
    private BoeXMLHandlerEvents boeXMLHandlerEvents;

    private void boeSetup(){
        Date curDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String todayDateString = dateFormat.format(curDate);
        final String currentBoeURL = this.BOE_BASE_ID + todayDateString;

        this.boeBaseURLString = this.BOE_BASE_URL;
        this.currentBoeSummaryURLString = this.BOE_BASE_URL + currentBoeURL;

        Log.d("BOE", "BaseURL: " + boeBaseURLString);
        Log.d("BOE", "SummaryURL: " + currentBoeSummaryURLString);
    }

    // Returns number of urlXml tags found
    public int getURLXMLsCount() {
        return urlXMLs.size();
    }

    // Returns number of rawXMLs parsed and stored
//    public int getRawDataHashMapCount() {
//        return boeXmlTodayRawData.size();
//    }

    /*
    parseXMLSumAndGetRawData is a method to parse summary and get urlXml´s.
    Data is stored on List<String> urlXMLs.
    */
    public void parseXMLSumAndGetRawData(XmlPullParser boeParser) {
        int event;
        String text = null;

        try {
            event = boeParser.getEventType();

            while (event != XmlPullParser.END_DOCUMENT) {
                String name = boeParser.getName();

                switch (event) {

                    case XmlPullParser.TEXT:
                        text = boeParser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        Log.d("BOE", "End TAG: " + name);
                        if (name.equals("urlXml")) {
                            urlXMLs.add(text);
                        } else if (name.equals("error")){
                            Log.d("BOE", "BOE´s summary XML ERROR TAG found!!!.");
                            Log.d("BOE", "BOE´s summary XML ERROR TAG content: " + text);
                            //Notify Listeners
                            boeXMLHandlerEvents.onFoundXMLErrorTag(text);
                        }
                        break;
                }
                event = boeParser.next();
            }
        } catch (Exception e) {
            Log.d("BOE", "ERROR while parsing XML summary file!");
            e.printStackTrace();
        }
    }

    /*
    parseXMLAndStoreIt is a method to parse and store xmls data.
    Data is stored on HashMap<String, String> boeXmlTodayRawData.
    */
    public void parseXMLAndStoreIt(XmlPullParser boeParser, String id) {
        int event;
        StringBuilder rawTextStringBuilder= new StringBuilder();
        String text = null;

        try {
            event = boeParser.getEventType();

            while (event != XmlPullParser.END_DOCUMENT) {
                String name = boeParser.getName();

                switch (event) {
                    case XmlPullParser.START_TAG:
                        break;

                    case XmlPullParser.TEXT:
                        text = boeParser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (name.equals("p")) {
                            rawTextStringBuilder.append(text);
                        }
                        break;

                }
                event = boeParser.next();
            }
            boeXmlTodayRawData.put(id, rawTextStringBuilder.toString());
        } catch (Exception e) {
            Log.d("BOE", "ERROR while parsing attached XML file!");
            e.printStackTrace();
        }
    }

    /*
     boeRawDataQuery is a method to query/search data in the object.
     It returns a list<> with matching BOE´s ids.
    */
    public List<String> boeRawDataQuery(String searchQuery) {
        List<String> queryResults = new ArrayList<>();
        try {
            // TODO Review/re-do search query method execution
            for (Map.Entry<String, String> eachBoe : boeXmlTodayRawData.entrySet()) {
                if (eachBoe.getValue().contains(searchQuery)) {
                    queryResults.add(eachBoe.getKey());
                }
            }
            // Notify Listeners
            boeXMLHandlerEvents.onSearchQueryCompleted(queryResults.size(), searchQuery);
            return queryResults;
        } catch (Exception e) {
            Log.d("BOE", "ERROR while searching for: " + searchQuery);
            e.printStackTrace();
            // Notify Listeners
            boeXMLHandlerEvents.onSearchQueryCompleted(queryResults.size(), searchQuery);
            return queryResults;
        }
    }

    /*
     fetchXML runs a thread to fetch URLs for summary and others
    */
    public void fetchXML() {
        Thread fetchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Fetches XML´s summary URL and sends it to parse rawData URLs
                try {
                    URL summaryURL = new URL(currentBoeSummaryURLString);
                    HttpURLConnection summaryConn = (HttpURLConnection) summaryURL.openConnection();

                    summaryConn.setReadTimeout(10000);      /* milliseconds */
                    summaryConn.setConnectTimeout(15000);   /* milliseconds */
                    summaryConn.setRequestMethod("GET");
                    summaryConn.setDoInput(true);
                    summaryConn.connect();

                    InputStream boeSummaryStream = summaryConn.getInputStream();
                    xmlFactoryObject = XmlPullParserFactory.newInstance();
                    XmlPullParser boeSummaryParser = xmlFactoryObject.newPullParser();

                    boeSummaryParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

                    // Set xml´s encoding to latin1
                    boeSummaryParser.setInput(boeSummaryStream, "ISO-8859-1");

                    parseXMLSumAndGetRawData(boeSummaryParser);
                    boeSummaryStream.close();
                } catch (Exception e) {
                    Log.d("BOE", "ERROR while trying to download BOE´s summary!");
                    e.printStackTrace();
                }

                if (urlXMLs.size() > 0){
                    // Fetches each rawXML and passes each one to parse and store data
                    for (String eachUrlXML : urlXMLs) {
                        try {
                            URL itemURL = new URL(boeBaseURLString + eachUrlXML);
                            HttpURLConnection itemConn = (HttpURLConnection) itemURL.openConnection();

                            itemConn.setReadTimeout(10000 /* milliseconds */);
                            itemConn.setConnectTimeout(15000 /* milliseconds */);
                            itemConn.setRequestMethod("GET");
                            itemConn.setDoInput(true);
                            itemConn.connect();

                            InputStream boeStream = itemConn.getInputStream();
                            xmlFactoryObject = XmlPullParserFactory.newInstance();
                            XmlPullParser boeParser = xmlFactoryObject.newPullParser();

                            boeParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

                            // Set xml´s encoding to latin1 (ISO-8859-1)
                            boeParser.setInput(boeStream, "ISO-8859-1");

                            // Send parser and BOE´s id.
//                            Log.d("BOE", "BOE id:" + eachUrlXML.substring(eachUrlXML.indexOf("=") + 1));
                            parseXMLAndStoreIt(boeParser, eachUrlXML.substring(eachUrlXML.indexOf("=") + 1));
                            boeStream.close();
                        } catch (Exception e) {
                            Log.d("BOE", "ERROR while trying to download BOE´s attachments!");
                            e.printStackTrace();
                        }
                    }
                } else {
                    Log.d("BOE", "ERROR No urlXml tags found.");
                }
                boeXMLHandlerEvents.onBoeFetchCompleted();
            }
        });
        fetchThread.start();
    }
}
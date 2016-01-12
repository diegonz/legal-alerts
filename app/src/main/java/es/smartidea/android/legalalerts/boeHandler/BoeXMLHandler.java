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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // TODO: Check need of un-setter/un-binder method.
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
        final String currentBoeURL = BOE_BASE_ID + todayDateString;

        this.boeBaseURLString = BOE_BASE_URL;
        this.currentBoeSummaryURLString = BOE_BASE_URL + currentBoeURL;

        Log.d("BOE", "BaseURL: " + boeBaseURLString);
        Log.d("BOE", "SummaryURL: " + currentBoeSummaryURLString);
    }

    // Returns number of urlXml tags found (Announcements and disposals)
    public int getURLXMLsCount() {
        return urlXMLs.size();
    }

    /*
    * parseXMLSumAndGetRawData is a method to parse summary and get urlXml´s.
    * Data is stored on List<String> urlXMLs.
    **/
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
                        if (name.equals("urlXml")) {
                            urlXMLs.add(text);
                        } else if (name.equals("error")){

                            // Set XML error flag
                            xmlError = true;
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
    * parseXMLAndStoreIt is a method to parse and store xml data.
    * Data is stored on HashMap<String, String> boeXmlTodayRawData.
    **/
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
    * boeRawDataQuery is a method to query/search data in the object.
    * It returns a list<> with matching BOE´s ids.
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
    * fetchXML runs a thread to fetch URLs for summary and others
    */
    public void fetchXML() {
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

                    parseXMLSumAndGetRawData(boeSummaryParser);
                    boeSummaryStream.close();
                } catch (Exception e) {
                    Log.d("BOE", "ERROR while trying to download BOE´s summary!");
                    e.printStackTrace();
                }

                // Start fetch and parse thread if xmlError is FALSE
                if (!xmlError){
                    // Fetches each rawXML and passes each one to parse and store data
                    for (String eachUrlXML : urlXMLs) {
                        try {
                            InputStream boeStream = new OkHttpGetURL().run(boeBaseURLString + eachUrlXML);
                            xmlFactoryObject = XmlPullParserFactory.newInstance();
                            XmlPullParser boeParser = xmlFactoryObject.newPullParser();

                            boeParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

                            // Set xml´s encoding to latin1 (ISO-8859-1)
                            boeParser.setInput(boeStream, "ISO-8859-1");

                            // Send parser and BOE´s id.
                            parseXMLAndStoreIt(boeParser, eachUrlXML.substring(eachUrlXML.indexOf("=") + 1));
                            boeStream.close();
                        } catch (Exception e) {
                            Log.d("BOE", "ERROR while trying to download BOE´s attachments!");
                            e.printStackTrace();
                        }
                        // Fetch Completed Listener
                        boeXMLHandlerEvents.onBoeFetchCompleted();
                    }
                } else {
                    Log.d("BOE", "ERROR No urlXml tags found.");
                }
            }
        });
        fetchThread.start();
    }

    // TODO: Check AsyncTask vs pure Threads
//    public class GetURL extends AsyncTask<String, Void, InputStream> {
//
//        @Override
//        protected InputStream doInBackground(String... strings) {
//            try {
//                return new OkHttpGetURL().run(strings[0]);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(InputStream inputStream) {
//            super.onPostExecute(inputStream);
//        }
//    }
}
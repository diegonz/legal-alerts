package es.smartidea.android.legalalerts.utils;

import android.support.annotation.NonNull;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TextSearchUtils {
    private final static String LOG_TAG = "TextSearchUtils";

    private final static Pattern NOT_ASCII_REGEXP = Pattern.compile("[^\\p{ASCII}]");
    private final static Pattern SPACE_REGEXP = Pattern.compile("\\s");

    // Public empty constructor
    private TextSearchUtils() {}

    /**
     * Query/search data in the object for given pattern searchQuery
     * handling if isLiteralSearch is enabled.
     *
     * @param searchQuery     search term to look for into downloaded data
     * @param isLiteralSearch flag indicating if searchQuery has to be
     *                        searched literally or term by term splitting
     *                        by searchQuery words.
     * @return Map containing matching BOE´s PDF & XML URLs.
     */
    public static Map<String, String> rawDataSearchQuery(@NonNull String searchQuery,
                                                         boolean isLiteralSearch,
                                                         @NonNull String rawText,
                                                         @NonNull String urlXml) {

        Map<String, String> resultUrls = new HashMap<>(0);
        if (isLiteralSearch) {
            try {
                if (isNormalizedStringContained(rawText, searchQuery)) {
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
                    if (!isNormalizedStringContained(rawText, eachSearchItem))
                        hasAllSearchItems = false;
                }
                // Add Boe to result if "hasAllSearchItems"
                if (hasAllSearchItems) resultUrls.put(urlXml, searchQuery);
            } catch (Exception e) {
                // Log to file for debugging
                FileLogger.logToExternalFile(LOG_TAG + " - ERROR: while searching for: " + searchQuery);
                e.printStackTrace();
            }
        }
        return resultUrls;
    }

    /**
     * Normalizes and converts text to lower-case to look for searchItem into mainText
     *
     * @param mainText   String XML´s raw text, <titulo> and <p> tags
     *                   its normalized and converted to lower-case
     *                   for comparing purposes.
     * @param searchItem String item to search into mainText
     *                   its normalized and converted to lower-case
     *                   for comparing purposes.
     * @return boolean value if second parameter is contained onto first
     **/
    private static boolean isNormalizedStringContained(String mainText, String searchItem) {
        // TODO: Check alternatives like: org.apache.commons.lang3.StringUtils.containsIgnoreCase
        String normalizedMainText = NOT_ASCII_REGEXP
                .matcher(Normalizer.normalize(mainText, Normalizer.Form.NFD))
                .replaceAll("");
        String normalizedSearchItem = NOT_ASCII_REGEXP
                .matcher(Normalizer.normalize(searchItem, Normalizer.Form.NFD))
                .replaceAll("");

        return normalizedMainText.toLowerCase().contains(normalizedSearchItem.toLowerCase());
    }

}

package es.smartidea.android.legalalerts.utils;

import android.support.annotation.NonNull;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TextSearchUtils {
    private static final String LOG_TAG = "TextSearchUtils";

    private static final Pattern NOT_ASCII_REGEXP = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern SPACE_REGEXP = Pattern.compile("\\s");

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
        // TODO Check literal search implementation
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
                String[] searchItemArray = SPACE_REGEXP.split(searchQuery);
                boolean hasAllSearchItems = true;
                for (String eachSearchItem : searchItemArray) {
                    if (!isNormalizedStringContained(rawText, eachSearchItem)) {
                        hasAllSearchItems = false;
                        break;
                    }
                }
                if (hasAllSearchItems) {
                    resultUrls.put(urlXml, searchQuery);
                }
            } catch (Exception e) {
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
        final String normalizedMainText = NOT_ASCII_REGEXP
                .matcher(Normalizer.normalize(mainText, Normalizer.Form.NFD))
                .replaceAll("");
        final String normalizedSearchItem = NOT_ASCII_REGEXP
                .matcher(Normalizer.normalize(searchItem, Normalizer.Form.NFD))
                .replaceAll("");
        return normalizedMainText.toLowerCase().contains(normalizedSearchItem.toLowerCase());
    }
}

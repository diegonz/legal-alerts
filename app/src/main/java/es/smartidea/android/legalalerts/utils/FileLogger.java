package es.smartidea.android.legalalerts.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

@SuppressWarnings("StringConcatenationMissingWhitespace")
public class FileLogger {

    //private static final String LOG_TAG = "FileLogger";

    private final static String SEPARATOR = File.separator;
    private final static String LOG_FILE_NAME = "legalalerts-log.txt";
    private final static String LOG_FOLDER_PATH = SEPARATOR + "LegalAlerts" + SEPARATOR + "log";
    private final static String LOG_FULL_PATH = LOG_FOLDER_PATH + SEPARATOR + LOG_FILE_NAME;
    private static final String LOG_TAG = "FileLogger";

    @SuppressWarnings("unused")
    public static void logToInternalFile(final Context context, final String receivedLogText){
        try {
            //BufferedWriter for performance, TRUE on FileWriter to set append to file flag
            BufferedWriter bufferedWriter =
                    new BufferedWriter(
                            new FileWriter(
                                    new File(context.getFilesDir(), LOG_FILE_NAME),
                                    true
                            )
                    );

            bufferedWriter.append(
                    String.format(Locale.getDefault(), "Timestamp: %d", System.currentTimeMillis())
            );
            bufferedWriter.newLine();
            bufferedWriter.append(receivedLogText);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Logs received string appending it to app external storage log file.
     *
     * NEED WRITE EXTERNAL PERMISSION DECLARED IN MANIFEST
     *
     * @param receivedLogText   String containing text to be logged to external log file.
     */
    public static void logToExternalFile(final String receivedLogText) {
        if (isExternalStorageWritable()){
            try {
                //BufferedWriter for performance, true to set append to file flag
                BufferedWriter buf = new BufferedWriter(new FileWriter(getExternalLogFile(), true));
                buf.append(
                        String.format(
                                Locale.getDefault(),
                                "Date: %1$tA %1$tb %1$td %1$tY - %1$tI:%1$tM %1$Tp",
                                Calendar.getInstance()
                        )
                );
                buf.newLine();
                buf.append(receivedLogText);
                buf.newLine();
                buf.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error writing external log file!");
                //e.printStackTrace();
            }
        }
    }

    /**
     * Checks for existing app folder on external storage and create it if not exist,
     * then returns a File object to Log file.
     *
     * @return existing or created File object referencing log file on external storage
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File getExternalLogFile(){
        File logPath = new File(Environment.getExternalStorageDirectory().getPath() + LOG_FOLDER_PATH);
        if (!logPath.exists()) logPath.mkdirs();
        File logFile = new File(Environment.getExternalStorageDirectory().getPath() + LOG_FULL_PATH);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error creating new external log file!");
                //e.printStackTrace();
            }
        }
        return logFile;
    }
    /**
     * Checks if external storage is available for read and write
     *
     * @return  TRUE if writing to external storage is available
     */
    private static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }
}

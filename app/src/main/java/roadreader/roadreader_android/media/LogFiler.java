package roadreader.roadreader_android.media;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogFiler {

    //values
    public static final int SAVE_DIRECTORY = CameraHelper.EXTERNAL_SAVE;
    public static final String LOGGER_NAME = "roadreader.roadreader_android";
    public static final String LOG_FILE_NAME = "myLog.log";


    protected File logFile;

    private Logger logger;





    /**
     * Constructor. Initializes logger. Creates or finds the log file.
     */
    public LogFiler() {
        logger = Logger.getLogger(LOGGER_NAME);
        createLogFile(SAVE_DIRECTORY);
    }

    /**
     * Initializes logger. Creates or finds the log file and writes to it.
     * @param str the String to be written to the log file. Log level defaults to INFO.
     */
    public LogFiler(String str) {
        logger = Logger.getLogger(LOGGER_NAME);
        createLogFile(SAVE_DIRECTORY);
        log(str);
    }


    /**
     * Initializes logger. Creates or finds the log file and writes to it.
     * @param str the String to be written to the log file.
     * @param level the log level to be assigned to the string.
     */
    public LogFiler(String str, Level level) {
        logger = Logger.getLogger(LOGGER_NAME);
        createLogFile(SAVE_DIRECTORY);
        log(str, level);
    }

    /**
     * Appends a string to log.txt file.
     * @param s String to be written to log file. Default level of INFO.
     * @return Returns true if logging to file was successful, false otherwise.
     */
    public boolean log (String s) {
        return log(s, Level.INFO);
    }

    /**
     * Logs a string to log file
     * @param s String to be written to log file
     * @param level logging level of message
     * @return
     */
    public boolean log (String s, Level level) {

        FileHandler fh;
        try {
            Log.d("logfile", "Writing to log.txt...");
            fh = new FileHandler(logFile.getAbsolutePath(), true);
            logger.addHandler(fh);
            logger.log(level, s);
            Log.d("logfile", "Success");
            fh.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }



    public boolean delete() {
        return logFile.delete();
    }


    /**
     * Creates log file if it does not already exist.
     * @param save Determines whether to create the log file in internal or external storage.
     */
    private void createLogFile(int save) {
        File logDir;
        if (save == CameraHelper.EXTERNAL_SAVE) {
            // To be safe, you should check that the SDCard is mounted
            // using Environment.getExternalStorageState() before doing this.
            if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
                Log.d("RoadReader", "no sd card");
                return;
            }

            // Create the storage directory if it does not exist
            //logDir = new File(Environment.getExternalStorageDirectory(), "RoadReader/Logs");
            logDir = new File(Environment.getExternalStorageDirectory(), "RoadReader/Logs");

            if (!logDir.exists()) {
                if (!logDir.mkdirs()) {
                    Log.d("RoadReader", "failed to create directory");
                    return;
                }
            }

        }
        else {

            // Create the storage directory if it does not exist
            // Using DIRECTORY_NOTIFICATIONS because DIRECTORY_DOCUMENTS requires a API level 19
            logDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS)
                    .getParent(), "RoadReader/Logs");

            if (!logDir.exists()) {
                if (!logDir.mkdirs()) {
                    Log.d("RoadReader", "failed to create directory");
                    return;
                }
            }
        }

        //Create File if it does not exist
        logFile = new File(logDir.getPath() + File.separator + LOG_FILE_NAME);
        Log.d("logfiler", "log file at:\n" + logFile.getAbsolutePath());


    }
}



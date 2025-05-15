package org.example.licentafromzero.Domain;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Util {
    private static final String LOG_FILE = "log.txt";
    private static BufferedWriter logWriter;
    private static boolean logInitialized = false;

    public static synchronized void log(int logLevel, Integer id, String text) { // Node log
        try {
            if (!logInitialized) {
                logWriter = new BufferedWriter(new FileWriter(LOG_FILE, false)); // overwrite
                logInitialized = true;
            } else if (logWriter == null) {
                logWriter = new BufferedWriter(new FileWriter(LOG_FILE, true)); // append
            }

            if (Constants.LOG_LEVEL <= logLevel) {
                String logEntry = "Node " + id + " " + text;
                System.out.println(logEntry);
                logWriter.write(logEntry);
                logWriter.newLine();
                logWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void log(String text) { //System log
        try {
            if (!logInitialized) {
                logWriter = new BufferedWriter(new FileWriter(LOG_FILE, false)); // overwrite
                logInitialized = true;
            } else if (logWriter == null) {
                logWriter = new BufferedWriter(new FileWriter(LOG_FILE, true)); // append
            }

            String logEntry = text;
            System.out.println(logEntry);
            logWriter.write(logEntry);
            logWriter.newLine();
            logWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void log(String text, boolean fileExclusive) { //System log
        try {
            if (!logInitialized) {
                logWriter = new BufferedWriter(new FileWriter(LOG_FILE, false)); // overwrite
                logInitialized = true;
            } else if (logWriter == null) {
                logWriter = new BufferedWriter(new FileWriter(LOG_FILE, true)); // append
            }

            if(fileExclusive) {
                String logEntry = " " + text;
                System.out.println(logEntry);
                logWriter.write(logEntry);
                logWriter.newLine();
                logWriter.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void close() {
        try {
            if (logWriter != null) {
                logWriter.close();
                logWriter = null;
                logInitialized = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


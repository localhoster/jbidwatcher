package com.jbidwatcher.util.config;
/*
 * Copyright (c) 2000-2007, CyberFOX Software, Inc. All Rights Reserved.
 *
 * Developed by mrs (Morgan Schweers)
 */

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class ErrorManagement implements LoggerInterface {
  private static PrintWriter mLogWriter = null;
  private List<ErrorHandler> sHandlers = new ArrayList<ErrorHandler>();

  public void addHandler(ErrorHandler eh) {
    sHandlers.add(eh);
  }

  public ErrorManagement() {
    String sep = System.getProperty("file.separator");
    String home = JConfig.getHomeDirectory("jbidwatcher");

    String doLogging = JConfig.queryConfiguration("logging", "true");
    if(doLogging.equals("true")) {
      if(mLogWriter == null) {
        try {
          File fp;
          String increment = "";
          int stepper = 1;
          do {
            fp = new File(home + sep + "errors" + increment + ".log");
            increment = "." + stepper++;
          } while(fp.exists());

          mLogWriter = new PrintWriter(new FileOutputStream(fp));
        } catch(IOException ioe) {
          System.err.println("FAILED TO OPEN AN ERROR LOG.");
          ioe.printStackTrace();
        }
      }
    }
  }

  public void closeLog() {
    if(mLogWriter != null) {
      mLogWriter.close();
      mLogWriter = null;
    }
  }

  public void logMessage(String msg) {
    Date log_time = new Date();

    System.err.println(log_time + ": " + msg);

    String logMsg = log_time + ": ";
    //  This should stop the annoying time duplication in log messages.
    if(msg.startsWith(logMsg)) logMsg = msg; else logMsg += msg;
    for(ErrorHandler handler : sHandlers) {
      handler.addLog(logMsg);
    }

    String doLogging = JConfig.queryConfiguration("logging", "true");
    if(doLogging.equals("true")) {
      if(mLogWriter != null) {
        mLogWriter.println(logMsg);
        mLogWriter.flush();
      }
    }
  }

  public void logDebug(String msg) {
    if(JConfig.debugging) logMessage(msg);
  }

  public void logVerboseDebug(String msg) {
    if(JConfig.queryConfiguration("debug.uber", "false").equals("true")) logMessage(msg);
  }

  public void handleDebugException(String sError, Throwable e) {
    if(JConfig.debugging) handleException(sError, e);
  }

  public static class LoggerWriter extends PrintWriter {
    private StringBuffer mSnapshot = null;

    public LoggerWriter() {
      super(System.out);
    }

    public void println(String x) {
      if (mSnapshot != null) {
        mSnapshot.append(x);
        mSnapshot.append('\n');
      }
    }

    public void setSnapshot(StringBuffer sb) {
      mSnapshot = sb;
    }
  }

  private static LoggerWriter sWriter = new LoggerWriter();

  public static String getStackTrace(Throwable e) {
    StringBuffer sb = new StringBuffer();
    sWriter.setSnapshot(sb);
    e.printStackTrace(sWriter);
    sWriter.setSnapshot(null);

    return sb.toString();
  }

  public void handleException(String sError, Throwable e) {
    Date log_time = new Date();

    if(sError == null || sError.length() == 0) {
      System.err.println("[" + log_time + "]");
    } else {
      System.err.println(log_time + ": " + sError);
    }
    e.printStackTrace();

    String doLogging = JConfig.queryConfiguration("logging", "true");

    String logMsg;
    if (sError == null || sError.length() == 0) {
      logMsg = "[" + log_time + "]";
    } else {
      logMsg = log_time + ": " + sError;
    }

    String trace = getStackTrace(e);
    for (ErrorHandler handler : sHandlers) {
      handler.exception(logMsg, e.getMessage(), trace);
    }

    if(doLogging.equals("true")) {
      if(mLogWriter != null) {
        mLogWriter.println(logMsg);
        mLogWriter.println(e.getMessage());
        e.printStackTrace(mLogWriter);
        mLogWriter.flush();
      }
    }
  }

  public void logFile(String msgtop, StringBuffer dumpsb) {
    String doLogging = JConfig.queryConfiguration("logging", "true");
    if(doLogging.equals("true")) {
      if(JConfig.debugging) {
        if(mLogWriter != null) {
          mLogWriter.println("+------------------------------");
          mLogWriter.println("| " + msgtop);
          mLogWriter.println("+------------------------------");
          if(dumpsb != null) {
            mLogWriter.println(dumpsb);
          } else {
            mLogWriter.println("(null)");
          }
          mLogWriter.println("+------------end---------------");
          mLogWriter.flush();
        }
        for(ErrorHandler handler : sHandlers) {
          handler.addLog("...");
        }
        logMessage("File contents logged with message: " + msgtop);
        for (ErrorHandler handler : sHandlers) {
          handler.addLog("...");
        }
      }
    }
  }

  /**
   * @param fname - The filename to output to.
   * @param sb    - The StringBuffer to dump out.
   * @brief Debugging function to dump a string buffer out to a file.
   * <p/>
   * This is used for 'emergency' debugging efforts.
   */
  public void dump2File(String fname, StringBuffer sb) {
    if (JConfig.queryConfiguration("debug.filedump", "false").equals("false")) return;

    FileWriter fw = null;
    try {
      fw = new FileWriter(fname);

      fw.write(sb.toString());
    } catch (IOException ioe) {
      handleException("Threw exception in dump2File!", ioe);
    } finally {
      if (fw != null) try {
        fw.close();
      } catch (IOException ignored) { /* I don't care about exceptions on close. */ }
    }
  }
}

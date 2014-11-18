package eu.herrn.loglevelchanger;

import com.sun.tools.attach.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.management.*;
import javax.management.remote.*;

/**
 *  TODO: Erlaube das Setzen der Ignore-Prefixes
 *  TOODO: Erlaube --verbose; Das sollte nach dem Setzen eine Zeile mit dem Loggernamen und dem dann aktuellen Level ausgeben
 * @author Marco Herrn
 */
public class LogLevelChanger {
   private static final String[] IGNORE_LOGGER_PREFIXES= {
     "java.",
     "javax.",
     "sun."
   };

   private static final String USAGE=
             "SYNOPSIS\n"
           + "       LogLevelChanger PID  [LOGGER [LOGLEVEL]]\n"
           + "\n"
           + "DESCRIPTION\n"
           + "       Prints or changes the LogLevels of Loggers of a Java process.\n"
           + "       Without any arguments (apart from the PID), it prints all the \n"
           + "       loggers and their configured LogLevels.\n"
           + "       If only the LOGGER is given, it prints out the LogLevel of that\n"
           + "       Logger.\n"
           + "       If both, the LOGGER and the LOGLEVEL are given, it sets the\n"
           + "       LogLevel of the given Logger.\n"
           + "\n"
           + "PARAMETERS\n"
           + "       PID\n"
           + "               The PID of the process for which to print or change the LogLevels.\n"
           + "\n"
           + "       LOGGER\n"
           + "               The name of the Logger to print or the change the LogLevel of.\n"
           + "\n"
           + "       LOGLEVEL\n"
           + "               The log level to set for the given Logger.\n";



   /////////////////////////////////////////////////////////////////////////////
   //
   // Attributes

   private final Options options;


   /////////////////////////////////////////////////////////////////////////////
   //
   // Constructors

   public LogLevelChanger(final Options options) {
     this.options= options;
   }

   /////////////////////////////////////////////////////////////////////////////
   //
   // Methods

   @SuppressWarnings("UseOfSystemOutOrSystemErr")
   public void execute() throws AttachNotSupportedException, IOException, MalformedObjectNameException {
     final LoggingMXBean mxbeanProxy= connect(this.options.pid);
     final List<String> loggerNames= mxbeanProxy.getLoggerNames();

     if (this.options.logger == null) {
       //print out all loggers and their levels
       final Map<String, String> loggersAndLevels= new TreeMap<String, String>();
       for (final String loggerName : loggerNames) {
         if (!isIgnored(loggerName)) {
           final String loggerLevel= mxbeanProxy.getLoggerLevel(loggerName);
           loggersAndLevels.put(loggerName, loggerLevel);
         }
       }
       System.out.println(buildLoggerList(loggersAndLevels));
       System.exit(0);
     } else if (this.options.loglevel == null) {
       //print out the level of the given logger
       System.out.println(mxbeanProxy.getLoggerLevel(this.options.logger));
       System.exit(0);
     } else {
       //set the level of the given logger
       mxbeanProxy.setLoggerLevel(this.options.logger, this.options.loglevel.getName());
       System.exit(0);
     }
   }


  private static LoggingMXBean connect(final String pid) throws AttachNotSupportedException, IOException, MalformedObjectNameException, MalformedURLException {
    final VirtualMachine vm= VirtualMachine.attach(pid);
    final Properties agentProperties = vm.getAgentProperties();
    final String connectorAddress= agentProperties.getProperty("com.sun.management.jmxremote.localConnectorAddress");
    final JMXServiceURL serviceUrl= new JMXServiceURL(connectorAddress);
    final JMXConnector connector= JMXConnectorFactory.connect(serviceUrl);
    final MBeanServerConnection connection= connector.getMBeanServerConnection();
    final ObjectName mbeanName = new ObjectName(LogManager.LOGGING_MXBEAN_NAME);
    final LoggingMXBean mxbeanProxy = JMX.newMXBeanProxy(connection, mbeanName, LoggingMXBean.class);
    return mxbeanProxy;
  }


  private static String buildLoggerList(final Map<String, String> loggersAndLevels) {
    final StringBuilder sb= new StringBuilder();

    final int longestLoggerName= getLongestLoggerName(loggersAndLevels.keySet());

    for (final Map.Entry<String, String> e : loggersAndLevels.entrySet()) {
      final String loggerName= e.getKey();
      final String loggerLevel= e.getValue();

      sb.append(String.format("%-"+longestLoggerName+"s [%-7s]%n", loggerName, loggerLevel));
    }

    return sb.toString();
  }


  private static int getLongestLoggerName(final Collection<String> loggerNames) {
    int longestLoggerName= 0;
    for (final String loggerName : loggerNames) {
      longestLoggerName= Math.max(longestLoggerName, loggerName.length());
    }
    return longestLoggerName;
  }


  private static boolean isIgnored(final String loggerName) {
    for (final String ignoredLoggerPrefix : IGNORE_LOGGER_PREFIXES) {
      if (loggerName.startsWith(ignoredLoggerPrefix)) {
        return true;
      }
    }
    return false;
  }


  private static Options parseOptions(final String[] args) {
    final String pid;
    final String logger;
    final Level loglevel;

    if (args.length == 0 || args.length > 3) {
      throw new IllegalArgumentException("Invalid number of arguments.");
    }


    pid= args[0];

    if (args.length > 1) {
      logger= args[1];
    } else {
      logger= null;
    }

    if (args.length == 3) {
      loglevel= Level.parse(args[2]);
    } else {
      loglevel= null;
    }

    return new Options(pid, logger, loglevel);
  }



  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public static void main(String[] args) throws Exception {
    try {
      final Options options= parseOptions(args);
      final LogLevelChanger llc= new LogLevelChanger(options);
      llc.execute();
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
      System.out.println();
      System.out.println(USAGE);
      System.exit(1);
    }
  }
}

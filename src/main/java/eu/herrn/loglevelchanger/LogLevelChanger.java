package eu.herrn.loglevelchanger;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LoggingMXBean;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * .
 *  TODO: Erlaube das Setzen der Ignore-Prefixes
 *  TODO: Erlaube --verbose; Das sollte nach dem Setzen eine Zeile mit dem Loggernamen und dem dann aktuellen Level ausgeben
 *  TODO: Predefined Ingore-Prefixes (JDK, JGOODIES, ...)
 *  TODO: Konfigurationsdatei (vorwiegend für Ignore-Prefixes, aber auch für weitere Optionen (wie --verbose)
 *        Sollte auch aus aktuellem Verzeichnis geladen werden können. Dann kann man das auch projektspezifisch erstellen
 *  TODO: Assembly-Plugin verfeinern (bin soll das Startskript an korrekter Position haben)
 *
 *  Thanks go to http://marxsoftware.blogspot.de/2010/04/dynamic-java-log-levels-with.html
 *
 * @author Marco Herrn
 */
public class LogLevelChanger {
   private static final String[] IGNORE_LOGGER_PREFIXES= {
     "java.",
     "javax.",
     "sun.",
     "com.sun.",
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


  private  LoggingMXBean connect(final String pid) throws AttachNotSupportedException, IOException, MalformedObjectNameException, MalformedURLException {
    final VirtualMachine vm= VirtualMachine.attach(pid);
    this.loadManagementAgent(vm);
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

  // Shamelessly copied from sun.tools.jconsole.LocalVirtualMachine#loadManagementAgent()
  private void loadManagementAgent(final VirtualMachine vm) throws IOException {
    final String home = vm.getSystemProperties().getProperty("java.home");
    // Normally in ${java.home}/jre/lib/management-agent.jar but might
    // be in ${java.home}/lib in build environments.

    String agent = home + File.separator + "jre" + File.separator + "lib" + File.separator + "management-agent.jar";
    File f = new File(agent);
    if (!f.exists()) {
      agent = home + File.separator +  "lib" + File.separator +
              "management-agent.jar";
      f = new File(agent);
      if (!f.exists()) {
        throw new IOException("Management agent not found");
      }
    }

    agent = f.getCanonicalPath();
    try {
      vm.loadAgent(agent, "com.sun.management.jmxremote");
    } catch (AgentLoadException x) {
      throw new IOException(x);
    } catch (AgentInitializationException x) {
      throw new IOException(x);
    }
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

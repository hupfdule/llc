/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.herrn.loglevelchanger;

import java.util.logging.*;

/**
 *
 * @author mherrn
 */
public class Options {
   private static final Logger LOGGER= Logger.getLogger(Options.class.getName());

   /////////////////////////////////////////////////////////////////////////////
   //
   // Attributes

   public final String pid;
   public final String logger;
   public final Level loglevel;


   /////////////////////////////////////////////////////////////////////////////
   //
   // Constructors

   public Options(final String pid, final String logger, final Level loglevel) {
     this.pid = pid;
     this.logger = logger;
     this.loglevel = loglevel;
   }

   /////////////////////////////////////////////////////////////////////////////
   //
   // Methods

}

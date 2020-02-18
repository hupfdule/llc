/*
 * Copyright (c) 2020, Marco Herrn.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  This particular file is
 * subject to the "Classpath" exception as provided in the LICENSE file
 * that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, see <http://www.gnu.org/licenses/>.
 */
package eu.herrn.loglevelchanger;

import java.util.logging.Level;
import java.util.logging.Logger;


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

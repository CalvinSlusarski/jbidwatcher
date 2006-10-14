package com.jbidwatcher.util;
/*
 * Copyright (c) 2000-2005 CyberFOX Software, Inc. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the
 *  Free Software Foundation, Inc.
 *  59 Temple Place
 *  Suite 330
 *  Boston, MA 02111-1307
 *  USA
 */

import com.jbidwatcher.xml.XMLElement;
import com.jbidwatcher.xml.XMLParseException;
import com.jbidwatcher.xml.XMLSerialize;
import com.jbidwatcher.util.ErrorManagement;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Date;

/*!@class EventLogger
 *
 * @brief Encapsulates a super-simple event log for display purposes.
 *
 * Each EventLogger object has a 'title' and an 'id' which will be
 * shown once for all the events.
 *
 * Arbitrary number of events can be added, and when they are, it
 * time-stamps when they happened.  It keeps track of the 'last
 * message', and the number of times it happened, only tracking the
 * first time it happened.
 *
 * It also allows for saving and loading to/from XML, so that any
 * given object can have its own event log as well.
 *
 * The naming isn't quite right, it should probably be 'addEvent' and
 * 'getEvents'.
 *
 */
public class EventLogger implements XMLSerialize {
  /** Records all status messages that are added.
   * 
   */
  private String _localId=null;
  private String _localTitle=null;
  private List _allStatus = new Vector();
  private final EntryStatus nullStatus = new EntryStatus("Nothing has happened.", new Date());

  /*!@class EventLogger
   *
   * @brief A single 'event'.
   *
   * This contains a single event, a string saying 'what' happened, a
   * date for when it happened first, and a count of times that it has
   * happened.
   *
   */
  private class EntryStatus {
    public String _what;
    public Date _when;
    public int _count;

    public EntryStatus(String what, Date when) {
      _what = what;
      _when = when;
      _count = 1;
    }

    public String toBulkString() {
      String outStatus;
      outStatus = _when + ": " + _what + " (" + _count + ")";

      return(outStatus);
    }

    public String toString() {
      String outStatus;
      outStatus = _when + ": " + _localId + " (" + _localTitle + ") - " + _what + " (" + _count + ")";

      return(outStatus);
    }
  }

  /** 
   * @brief Load events from XML.
   *
   * BUGBUG -- Fix this to work with XMLSerializeSimple if possible.  -- mrs: 23-February-2003 21:00
   * 
   * @param curElement 
   */
  public void fromXML(XMLElement curElement) {
    Iterator logStep = curElement.getChildren();
    EntryStatus newEntry;

    while(logStep.hasNext()) {
      XMLElement curEntry = (XMLElement) logStep.next();

      if(curEntry.getTagName().equals("entry")) {
        long msgtime = System.currentTimeMillis();
        String msg = "Nothing has happened.";
        int curCount = Integer.parseInt(curEntry.getProperty("COUNT"));

        Iterator entryStep = curEntry.getChildren();
        while(entryStep.hasNext()) {
          XMLElement entryField = (XMLElement)entryStep.next();
          if(entryField.getTagName().equals("message")) msg = entryField.getContents();
          if(entryField.getTagName().equals("date")) msgtime = Long.parseLong(entryField.getContents());
        } 

        newEntry = new EntryStatus(msg, new Date(msgtime));
        newEntry._count = curCount;

        _allStatus.add(newEntry);
      } else {
        throw new XMLParseException(curEntry.getTagName(), "Expected 'entry' tag!");
      }
    }
  }

  public XMLElement toXML() {
    XMLElement xmlLog;

    if(_allStatus.size() == 0) return null;

    xmlLog = new XMLElement("log");

    for(int i=0; i< _allStatus.size(); i++) {
      EntryStatus curEntry = (EntryStatus) _allStatus.get(i);
      XMLElement xmlResult;
      XMLElement xmsg, xdate;

      xmlResult = new XMLElement("entry");
      xmlResult.setProperty("count", Integer.toString(curEntry._count));

      xmsg = new XMLElement("message");
      xmsg.setContents(curEntry._what);
      xmlResult.addChild(xmsg);

      xdate = new XMLElement("date");
      xdate.setContents(Long.toString(curEntry._when.getTime()));
      xmlResult.addChild(xdate);

      xmlLog.addChild(xmlResult);
    }
    return(xmlLog);
  }

  public EventLogger(String identifier, String title) {
    _localId = identifier;
    _localTitle = title;
  }

  /** Store the status for the most recent event to occur, and format it with the date
   * in front, the item identifier, the title, and the status all in the log line.  This
   * eventually needs to log to a file, or something similar.
   * 
   * @param inStatus A piece of text describing this, the most recent event 
   *     occuring to this auction entry.
   */
  public void setLastStatus(String inStatus) {
    EntryStatus whatHappened;
    EntryStatus lastStatus;

    if(inStatus != null) {
      if(_allStatus.size() > 0) {
        lastStatus = (EntryStatus) _allStatus.get(_allStatus.size()-1);
      } else {
        lastStatus = nullStatus;
      }
      if(inStatus.equals(lastStatus._what)) {
        lastStatus._count++;
      } else {
        whatHappened = new EntryStatus(inStatus, new Date());

        _allStatus.add(whatHappened);
        ErrorManagement.logMessage(whatHappened.toString());
      }
    }
  }

  public String getLastStatus() { return getLastStatus(false); }
  public int getStatusCount() { return _allStatus.size(); }

  /** What is the most recent thing that happened to this particular auction?
   *
   * @param bulk - Whether to return them as a bulk set of entries, or not.
   * 
   * @return A string, formatted, that details the most recent event in plain words.
   */
  public String getLastStatus(boolean bulk) {
    if(_allStatus.size() == 0) {
      if(bulk) {
        return(nullStatus.toBulkString() + "<br>");
      } else {
        return(nullStatus.toString() + "<br>");
      }
    } else {
      StringBuffer sb = new StringBuffer();
      EntryStatus lastStatus;
      int i;

      for(i=0; i<_allStatus.size(); i++) {
        lastStatus = (EntryStatus) _allStatus.get(i);
        if(bulk) {
          sb.append(lastStatus.toBulkString());
        } else {
          sb.append(lastStatus.toString());
        }
        sb.append("<br>");
      }
      return(sb.toString());
    }
  }
}
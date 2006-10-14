package com.jbidwatcher.ui;
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

import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import com.stevesoft.pat.*;
import com.jbidwatcher.queue.MQFactory;
import com.jbidwatcher.ui.Selector;
import com.jbidwatcher.auction.AuctionEntry;

public class JTabManager extends JBidMouse {
  private JTabbedPane _auctionTypes;

  private Map _nameTableMap = new TreeMap();

  public JTabManager() {
    _auctionTypes = new JTabbedPane();
    _auctionTypes.addMouseListener(new JTabPopupMenu(_auctionTypes));
  }

  public JTabbedPane getTabs() {
    return _auctionTypes;
  }

  public void add(String tabName, JComponent tabComponent, TableSorter inTS) {
    _auctionTypes.add(tabName, tabComponent);
    _nameTableMap.put(tabName, inTS);
  }

  private TableSorter getCurrentTable() {
    int currentIndex = _auctionTypes.getSelectedIndex();
    String currentTitle = _auctionTypes.getTitleAt(currentIndex);

    return( (TableSorter) _nameTableMap.get(currentTitle) );
  }

  protected int[] getPossibleRows() {
    return(getCurrentTable().getSelectedRows());
  }

  private class mySelector implements Selector {
    private String _search;

    mySelector(String s) {
      _search = s;
    }

    public boolean select(JTable inTable) {
      String trueSearch = _search;
      boolean foundOne = false;
      boolean match = false, invert = false;
      boolean comment_t = false, seller_t = false, buyer_t = false, all_t = false;
      AuctionEntry ae;

      if (trueSearch.startsWith("~!")) {
        invert = true;
        trueSearch = trueSearch.substring(2);
        if (trueSearch.startsWith(" ")) trueSearch = trueSearch.substring(1);
      }

      if (trueSearch.startsWith("~")) {
        if (trueSearch.startsWith("~a")) {
          comment_t = true;
          seller_t = true;
          buyer_t = true;
          all_t = true;
        }
        if (trueSearch.startsWith("~b")) buyer_t = true;
        if (trueSearch.startsWith("~c")) comment_t = true;
        if (trueSearch.startsWith("~s")) seller_t = true;
        if (trueSearch.startsWith("~u")) {
          buyer_t = true;
          seller_t = true;
        }

        if (seller_t || buyer_t || comment_t) trueSearch = trueSearch.substring(2);
        if (trueSearch.startsWith(" ")) trueSearch = trueSearch.substring(1);
      }

      Regex tableSearch = new Regex(trueSearch);

      tableSearch.setIgnoreCase(true);
      tableSearch.optimize();
      inTable.clearSelection();

      for (int i = 0; i < inTable.getRowCount(); i++) {
        match = false;
        ae = (AuctionEntry) inTable.getValueAt(i, -1);

        if (!match && seller_t) match = tableSearch.search(ae.getSeller());
        if (!match && buyer_t && ae.getHighBidder() != null) match = tableSearch.search(ae.getHighBidder());
        if (!match && comment_t && ae.getComment() != null) match = tableSearch.search(ae.getComment());
        //  If seller or buyer search was set, ignore the title / comments.
        if (!match && (all_t || (!seller_t && !buyer_t && !comment_t))) {
          match = tableSearch.search(ae.getTitle());
        }
        if (invert) match = !match;
        if (match) {
          inTable.addRowSelectionInterval(i, i);
          foundOne = true;
        }
      }
      return foundOne;
    }
  }

  public void selectBySearch(String srch) {
    TableSorter curTable = getCurrentTable();
    Selector mySelector = new mySelector(srch);
    if(!curTable.select(mySelector)) {
      java.awt.Toolkit.getDefaultToolkit().beep();
      MQFactory.getConcrete("Swing").enqueue("No entries matched!");
    }
  }

  protected Object getIndexedEntry(int i) {
    return getCurrentTable().getValueAt(i, -1);
  }

  public void actionPerformed(ActionEvent ae) {
    AuctionEntry whichAuction = null;
    String actionString = ae.getActionCommand();
    TableSorter chosenTable = getCurrentTable();
    boolean isButton = false;

    if(actionString.startsWith("BT-")) {
      actionString = actionString.substring(3);
      isButton = true;
    }

    if(chosenTable != null) {
      if(!isButton) {
        whichAuction = (AuctionEntry)chosenTable.getObjectAt(this.getPopupX(), this.getPopupY());
      } else {
        int temp[] = chosenTable.getSelectedRows();
        if(temp.length == 0) {
          whichAuction = null;
        } else {
          whichAuction = (AuctionEntry)chosenTable.getValueAt(temp[0], -1);
        }
      }
    }

    DoAction(ae.getSource(), actionString, whichAuction);
  }
}
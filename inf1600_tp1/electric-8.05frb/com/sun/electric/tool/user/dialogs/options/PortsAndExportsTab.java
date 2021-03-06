/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortsAndExportsTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;

import javax.swing.JPanel;

/**
 * Class to handle the "Ports/Exports" tab of the Preferences dialog.
 */
public class PortsAndExportsTab extends PreferencePanel
{
	/** Creates new form PortsAndExportsTab */
	public PortsAndExportsTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return port; }

	/** return the name of this preferences tab. */
	public String getName() { return "Ports/Exports"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Ports tab.
	 */
	public void init()
	{
		switch (User.getPortDisplayLevel())
		{
			case 0: portFullPort.setSelected(true);    break;
			case 1: portShortPort.setSelected(true);   break;
			case 2: portCrossPort.setSelected(true);   break;
		}

		switch (User.getExportDisplayLevel())
		{
			case 0: portFullExport.setSelected(true);    break;
			case 1: portShortExport.setSelected(true);   break;
			case 2: portCrossExport.setSelected(true);   break;
		}

		portMoveNode.setSelected(User.isMoveNodeWithExport());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Ports tab.
	 */
	public void term()
	{
		int curInt = 0;
        boolean redraw = false;
		if (portShortPort.isSelected()) curInt = 1; else
			if (portCrossPort.isSelected()) curInt = 2;
		if (curInt != User.getPortDisplayLevel())
        {
			User.setPortDisplayLevels(curInt);
            redraw = true;
        }

		curInt = 0;
		if (portShortExport.isSelected()) curInt = 1; else
			if (portCrossExport.isSelected()) curInt = 2;
		if (curInt != User.getExportDisplayLevel())
        {
			User.setExportDisplayLevels(curInt); 
            redraw = true;
        }

		boolean currentMoveNodeWithExport = portMoveNode.isSelected();
		if (currentMoveNodeWithExport != User.isMoveNodeWithExport())
			User.setMoveNodeWithExport(currentMoveNodeWithExport);

		// redisplay everything if port options changed
		if (redraw) EditWindow.repaintAllContents();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        portGroup = new javax.swing.ButtonGroup();
        exportGroup = new javax.swing.ButtonGroup();
        port = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        portFullPort = new javax.swing.JRadioButton();
        portFullExport = new javax.swing.JRadioButton();
        portShortPort = new javax.swing.JRadioButton();
        portShortExport = new javax.swing.JRadioButton();
        portCrossPort = new javax.swing.JRadioButton();
        portCrossExport = new javax.swing.JRadioButton();
        jSeparator2 = new javax.swing.JSeparator();
        portMoveNode = new javax.swing.JCheckBox();
        jSeparator9 = new javax.swing.JSeparator();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        port.setLayout(new java.awt.GridBagLayout());

        jLabel11.setText("Ports (in instances):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(jLabel11, gridBagConstraints);

        jLabel12.setText("Exports (in cells):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(jLabel12, gridBagConstraints);

        portFullPort.setText("Full Names");
        portGroup.add(portFullPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portFullPort, gridBagConstraints);

        portFullExport.setText("Full Names");
        exportGroup.add(portFullExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portFullExport, gridBagConstraints);

        portShortPort.setText("Short Names");
        portGroup.add(portShortPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portShortPort, gridBagConstraints);

        portShortExport.setText("Short Names");
        exportGroup.add(portShortExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portShortExport, gridBagConstraints);

        portCrossPort.setText("Crosses");
        portGroup.add(portCrossPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portCrossPort, gridBagConstraints);

        portCrossExport.setText("Crosses");
        exportGroup.add(portCrossExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portCrossExport, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        port.add(jSeparator2, gridBagConstraints);

        portMoveNode.setText("Move node with export name");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portMoveNode, gridBagConstraints);

        jSeparator9.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        port.add(jSeparator9, gridBagConstraints);

        getContentPane().add(port, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup exportGroup;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JPanel port;
    private javax.swing.JRadioButton portCrossExport;
    private javax.swing.JRadioButton portCrossPort;
    private javax.swing.JRadioButton portFullExport;
    private javax.swing.JRadioButton portFullPort;
    private javax.swing.ButtonGroup portGroup;
    private javax.swing.JCheckBox portMoveNode;
    private javax.swing.JRadioButton portShortExport;
    private javax.swing.JRadioButton portShortPort;
    // End of variables declaration//GEN-END:variables

}

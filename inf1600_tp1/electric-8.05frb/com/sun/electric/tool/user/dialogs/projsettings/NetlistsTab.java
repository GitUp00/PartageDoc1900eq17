/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetlistsTab.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.projsettings;

import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.text.Setting;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.ProjectSettingsFrame;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import javax.swing.JTextArea;


/**
 * Class to handle the "Netlists" tab of the Project Settings dialog.
 */
public class NetlistsTab extends ProjSettingsPanel
{
    private Setting ignoreResistorsSetting = NetworkTool.getIgnoreResistorsSetting();
    private Setting includeDateAndVersionInOutputSetting = User.getIncludeDateAndVersionInOutputSetting();
    private Setting useCopyrightMessageSetting = IOTool.getUseCopyrightMessageSetting();
    private Setting copyrightMessageSetting = IOTool.getCopyrightMessageSetting();
    
	/** Creates new form NetlistsTab */
	public NetlistsTab(ProjectSettingsFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return netlists; }

	/** return the name of this preferences tab. */
	public String getName() { return "Netlists"; }

	private JTextArea copyrightTextArea;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Copyright tab.
	 */
	public void init()
	{
		netIgnoreResistors.setSelected(getBoolean(ignoreResistorsSetting));
		generalIncludeDateAndVersion.setSelected(getBoolean(includeDateAndVersionInOutputSetting));

		if (getBoolean(useCopyrightMessageSetting)) copyrightUse.setSelected(true); else
			copyrightNone.setSelected(true);

		copyrightTextArea = new JTextArea();
		copyrightMessage.setViewportView(copyrightTextArea);
		copyrightTextArea.setText(getString(copyrightMessageSetting));
		copyrightTextArea.addKeyListener(new KeyAdapter()
		{
			public void keyTyped(KeyEvent evt) { copyrightMessageKeyTyped(evt); }
		});
	}

	private void copyrightMessageKeyTyped(KeyEvent evt)
	{
		copyrightUse.setSelected(true);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Copyright tab.
	 */
	public void term()
	{
        setBoolean(ignoreResistorsSetting, netIgnoreResistors.isSelected());
        setBoolean(includeDateAndVersionInOutputSetting, generalIncludeDateAndVersion.isSelected());
        setBoolean(useCopyrightMessageSetting, copyrightUse.isSelected());
        setString(copyrightMessageSetting, copyrightTextArea.getText());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        copyrightGroup = new javax.swing.ButtonGroup();
        netlists = new javax.swing.JPanel();
        generalIncludeDateAndVersion = new javax.swing.JCheckBox();
        copyright = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        copyrightNone = new javax.swing.JRadioButton();
        copyrightUse = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        copyrightMessage = new javax.swing.JScrollPane();
        netIgnoreResistors = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("IO Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        getAccessibleContext().setAccessibleName("Netlist Settings");
        netlists.setLayout(new java.awt.GridBagLayout());

        generalIncludeDateAndVersion.setText("Include date and version in output files");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        netlists.add(generalIncludeDateAndVersion, gridBagConstraints);

        copyright.setLayout(new java.awt.GridBagLayout());

        copyright.setBorder(javax.swing.BorderFactory.createTitledBorder("Copyright Information"));
        jLabel4.setText("A Copyright message can be added to every generated deck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(jLabel4, gridBagConstraints);

        copyrightGroup.add(copyrightNone);
        copyrightNone.setText("No copyright message");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 4);
        copyright.add(copyrightNone, gridBagConstraints);

        copyrightGroup.add(copyrightUse);
        copyrightUse.setText("Use this copyright message:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 4);
        copyright.add(copyrightUse, gridBagConstraints);

        jLabel5.setText("Do not put comment characters in this message");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(jLabel5, gridBagConstraints);

        copyrightMessage.setMinimumSize(new java.awt.Dimension(200, 200));
        copyrightMessage.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(copyrightMessage, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        netlists.add(copyright, gridBagConstraints);

        netIgnoreResistors.setText("Ignore Resistors when building netlists");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        netlists.add(netIgnoreResistors, gridBagConstraints);

        getContentPane().add(netlists, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel copyright;
    private javax.swing.ButtonGroup copyrightGroup;
    private javax.swing.JScrollPane copyrightMessage;
    private javax.swing.JRadioButton copyrightNone;
    private javax.swing.JRadioButton copyrightUse;
    private javax.swing.JCheckBox generalIncludeDateAndVersion;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JCheckBox netIgnoreResistors;
    private javax.swing.JPanel netlists;
    // End of variables declaration//GEN-END:variables

}

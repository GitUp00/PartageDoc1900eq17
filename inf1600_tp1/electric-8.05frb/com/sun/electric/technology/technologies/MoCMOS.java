/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MoCMOS.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.technology.technologies;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.XMLRules;
import com.sun.electric.technology.Xml;
import com.sun.electric.technology.technologies.utils.MOSRules;

import java.awt.Color;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the MOSIS CMOS technology.
 */
public class MoCMOS extends Technology
{
	/** Value for standard SCMOS rules. */		public static final int SCMOSRULES = 0;
	/** Value for submicron rules. */			public static final int SUBMRULES  = 1;
	/** Value for deep rules. */				public static final int DEEPRULES  = 2;

	/** key of Variable for saving technology state. */
	public static final Variable.Key TECH_LAST_STATE = Variable.newKey("TECH_last_state");

    public static final Version changeOfMetal6 =Version.parseVersion("8.02o"); // Fix of bug #357
            
	// layers to share with subclasses
    private Layer[] viaLayers = new Layer[5];
	private Layer poly1Layer, poly2_lay, transistorPolyLayer;
    private Layer silicideBlockLayer;
    private Layer[] selectLayers;
    private Layer[] metalLayers = new Layer[6]; // 1 -> 6
    private Layer polyCutLayer;
    private Layer pActiveWellLayer;
    private Layer[] activeLayers = new Layer[2];
    private Layer[] pseudoActiveLayers = new Layer[2];
    private Layer[] pseudoSelectLayers = new Layer[2];
    private Layer[] pseudoWellLayers = new Layer[2];
    private Layer[] wellLayers = new Layer[2];
    private Layer activeCutLayer;
    private Layer thickActiveLayer;
    private Layer passivationLayer;
    private Layer polyCapLayer;
    private Layer padFrameLayer;
    private Layer pBaseLayer;

	// arcs
    /** metal 1->6 arc */						private ArcProto[] metalArcs = new ArcProto[6];
	/** polysilicon 1/2 arc */					private ArcProto[] polyArcs = new ArcProto[2];
    /** P/N-active arc */                       private ArcProto[] activeArcs = new ArcProto[2];
    /** General active arc */                   private ArcProto active_arc;

	// nodes. Storing nodes only whe they are need in outside the constructor
    /** metal-1->6-pin */				        private PrimitiveNode[] metalPinNodes = new PrimitiveNode[6];
	/** active pins */					        private PrimitiveNode[] activePinNodes = new PrimitiveNode[2];
	/** polysilicon-1-pin/2-pin */			    private PrimitiveNode[] polyPinNodes = new PrimitiveNode[2];
    /** metal-1-P/N-active-contacts */          private PrimitiveNode[] metalActiveContactNodes = new PrimitiveNode[2];
	/** metal-1-polysilicon-1/2/1-2/-contact */	private PrimitiveNode[] metal1PolyContactNodes = new PrimitiveNode[3];
    /** P/N-Transistors */                      private PrimitiveNode[] transistorNodes = new PrimitiveNode[2];
	/** ThickOxide Transistors */				private PrimitiveNode[] thickTransistorNodes = new PrimitiveNode[2];
    /** Scalable Transistors */			        private PrimitiveNode[] scalableTransistorNodes;
    /** NPN Transistor */                       private PrimitiveNode npnTransistorNode;
    /** M1M2 -> M5M6 contacts */				private PrimitiveNode[] metalContactNodes = new PrimitiveNode[5];
    /** metal-1-P/N-Well-contacts */            private PrimitiveNode[] metalWellContactNodes = new PrimitiveNode[2];
//	/** Polysilicon-1/2-Node */					private PrimitiveNode[] polyNodes = new PrimitiveNode[2];
//	/** Via-1 -. Via-5 Nodes */					private PrimitiveNode[] viaNodes = new PrimitiveNode[5];

    // for dynamically modifying the transistor geometry
	private Technology.NodeLayer[] transistorPolyLayers = new Technology.NodeLayer[2];
	private Technology.NodeLayer[] transistorActiveLayers = new Technology.NodeLayer[2];
	private Technology.NodeLayer[] transistorActiveTLayers = new Technology.NodeLayer[2];
    private Technology.NodeLayer[] transistorActiveBLayers = new Technology.NodeLayer[2];
	private Technology.NodeLayer[] transistorPolyLLayers = new Technology.NodeLayer[2];
    private Technology.NodeLayer[] transistorPolyRLayers = new Technology.NodeLayer[2];
    private Technology.NodeLayer[] transistorPolyCLayers = new Technology.NodeLayer[2];
	private Technology.NodeLayer[] transistorWellLayers = new Technology.NodeLayer[2];
	private Technology.NodeLayer[] transistorSelectLayers = new Technology.NodeLayer[2];
//	/** Metal-1 -> Metal-6 Nodes */			    private PrimitiveNode[] metalNodes = new PrimitiveNode[6];

	// design rule constants
//	/** wide rules apply to geometry larger than this */				private static final double WIDELIMIT = 100;

	// -------------------- private and protected methods ------------------------

    public MoCMOS(Xml.Technology t) {
        super(t);
		setNoNegatedArcs();
		setStaticTechnology();
        setFactoryResolution(0.01); // value in lambdas   0.005um -> 0.05 lambdas
        // Logical Effort Tech-dependent settings
        setFactoryLESettings(0.167, 0.16, 0.7);

		//**************************************** LAYERS ****************************************
		metalLayers[0] = findLayer("Metal-1");
		metalLayers[1] = findLayer("Metal-2");
		metalLayers[2] = findLayer("Metal-3");
		metalLayers[3] = findLayer("Metal-4");
		metalLayers[4] = findLayer("Metal-5");
		metalLayers[5] = findLayer("Metal-6");
		poly1Layer = findLayer("Polysilicon-1");
		poly2_lay = findLayer("Polysilicon-2");
		activeLayers[P_TYPE] = findLayer("P-Active");
		activeLayers[N_TYPE] = findLayer("N-Active");
        selectLayers = new Layer[2];
		selectLayers[P_TYPE] = findLayer("P-Select");
		selectLayers[N_TYPE] = findLayer("N-Select");
		wellLayers[P_TYPE] = findLayer("P-Well");
		wellLayers[N_TYPE] = findLayer("N-Well");
		polyCutLayer = findLayer("Poly-Cut");
		activeCutLayer = findLayer("Active-Cut");
        for (int i = 0; i < viaLayers.length; i++)
		    viaLayers[i] = findLayer("Via");
		passivationLayer = findLayer("Passivation");
		transistorPolyLayer = findLayer("Transistor-Poly");
		polyCapLayer = findLayer("Poly-Cap");
		pActiveWellLayer = findLayer("P-Active-Well");
		silicideBlockLayer = findLayer("Silicide-Block");
		thickActiveLayer = findLayer("Thick-Active");
		padFrameLayer = findLayer("Pad-Frame");
        /***************************************/
        // createExtraLayers
		pBaseLayer = findLayer("P-Base");
        /***************************************/
		Layer pseudoMetal1_lay = metalLayers[0].getPseudoLayer();		// Pseudo-Metal-1
		Layer pseudoMetal2_lay = metalLayers[1].getPseudoLayer();		// Pseudo-Metal-2
		Layer pseudoMetal3_lay = metalLayers[2].getPseudoLayer();		// Pseudo-Metal-3
		Layer pseudoMetal4_lay = metalLayers[3].getPseudoLayer();		// Pseudo-Metal-4
		Layer pseudoMetal5_lay = metalLayers[4].getPseudoLayer();		// Pseudo-Metal-5
		Layer pseudoMetal6_lay = metalLayers[5].getPseudoLayer();		// Pseudo-Metal-6
		Layer pseudoPoly1_lay = poly1Layer.getPseudoLayer();            // Pseudo-Polysilicon-1
		Layer pseudoPoly2_lay = poly2_lay.getPseudoLayer();             // Pseudo-Polysilicon-2
		pseudoActiveLayers[P_TYPE] = activeLayers[P_TYPE].getPseudoLayer();		// Pseudo-P-Active
		pseudoActiveLayers[N_TYPE] = activeLayers[N_TYPE].getPseudoLayer();		// Pseudo-N-Active
		pseudoSelectLayers[P_TYPE] = selectLayers[P_TYPE].getPseudoLayer();	// Pseudo-P-Select
		pseudoSelectLayers[N_TYPE] = selectLayers[N_TYPE].getPseudoLayer();	// Pseudo-N-Select
		pseudoWellLayers[P_TYPE] = wellLayers[P_TYPE].getPseudoLayer();		// Pseudo-P-Well
		pseudoWellLayers[N_TYPE] = wellLayers[N_TYPE].getPseudoLayer();		// Pseudo-N-Well

		//**************************************** ARCS ****************************************

		metalArcs[0] = findArcProto("Metal-1");
		metalArcs[1] = findArcProto("Metal-2");
		metalArcs[2] = findArcProto("Metal-3");
		metalArcs[3] = findArcProto("Metal-4");
		metalArcs[4] = findArcProto("Metal-5");
		metalArcs[5] = findArcProto("Metal-6");
		polyArcs[0] = findArcProto("Polysilicon-1");
		polyArcs[1] = findArcProto("Polysilicon-2");
		activeArcs[P_TYPE] = findArcProto("P-Active");
		activeArcs[N_TYPE] = findArcProto("N-Active");
		active_arc = findArcProto("Active");

		//**************************************** NODES ****************************************

		metalPinNodes[0] = findNodeProto("Metal-1-Pin");
		metalPinNodes[1] = findNodeProto("Metal-2-Pin");
		metalPinNodes[2] = findNodeProto("Metal-3-Pin");
		metalPinNodes[3] = findNodeProto("Metal-4-Pin");
		metalPinNodes[4] = findNodeProto("Metal-5-Pin");
		metalPinNodes[5] = findNodeProto("Metal-6-Pin");
		polyPinNodes[0] = findNodeProto("Polysilicon-1-Pin");
		polyPinNodes[1] = findNodeProto("Polysilicon-2-Pin");
		activePinNodes[P_TYPE] = findNodeProto("P-Active-Pin");
		activePinNodes[N_TYPE] = findNodeProto("N-Active-Pin");
		PrimitiveNode activeGenPinNode = findNodeProto("Active-Pin");
		metalActiveContactNodes[P_TYPE] = findNodeProto("Metal-1-P-Active-Con");
		metalActiveContactNodes[N_TYPE] = findNodeProto("Metal-1-N-Active-Con");
		metal1PolyContactNodes[0] = findNodeProto("Metal-1-Polysilicon-1-Con");
		metal1PolyContactNodes[1] = findNodeProto("Metal-1-Polysilicon-2-Con");
		metal1PolyContactNodes[2] = findNodeProto("Metal-1-Polysilicon-1-2-Con");
        String[] stdNames = {"p", "n"};
        for (int i = 0; i < 2; i++)
            transistorNodes[i] = findNodeProto(stdNames[i].toUpperCase()+"-Transistor");

		/** Thick oxide transistors */
		String[] thickNames = {"Thick-P", "Thick-N"};
		for (int i = 0; i < thickTransistorNodes.length; i++)
			thickTransistorNodes[i] = findNodeProto(thickNames[i] + "-Transistor");

        scalableTransistorNodes = new PrimitiveNode[2];
        scalableTransistorNodes[P_TYPE] = findNodeProto("P-Transistor-Scalable");
        scalableTransistorNodes[N_TYPE] = findNodeProto("N-Transistor-Scalable");
        npnTransistorNode = findNodeProto("NPN-Transistor");

		metalContactNodes[0] = findNodeProto("Metal-1-Metal-2-Con");
		metalContactNodes[1] = findNodeProto("Metal-2-Metal-3-Con");
		metalContactNodes[2] = findNodeProto("Metal-3-Metal-4-Con");
		metalContactNodes[3] = findNodeProto("Metal-4-Metal-5-Con");
		metalContactNodes[4] = findNodeProto("Metal-5-Metal-6-Con");

        for (int i = 0; i < metalWellContactNodes.length; i++)
            metalWellContactNodes[i] = findNodeProto(metalLayers[0].getName()+"-"+wellLayers[i].getName()+"-Con");
    }

    /**
     * Constructs MOCMOS technology without TSMC180 foundry.
     */
	public MoCMOS()
    {
		this("mocmos", "MOSIS CMOS", "MOSIS CMOS", Foundry.Type.MOSIS, 200, true); // in nanometers: really 0.2 micron
        initFoundryMOSIS();
    }

    private MoCMOS(String techName, String techShortName, String techDesc, Foundry.Type defaultFoundryType, double factoryScale, boolean haveMocmosExtensions) {
        super(techName, defaultFoundryType, 6);
        setFactoryScale(factoryScale, true);
        setTechShortName(techShortName);
        setTechDesc(techDesc);
        
		setNoNegatedArcs();
		setStaticTechnology();
		setFactoryTransparentLayers(new Color []
		{
			new Color( 96,209,255), // Metal-1
			new Color(255,155,192), // Polysilicon-1
			new Color(107,226, 96), // Active
			new Color(224, 95,255), // Metal-2
			new Color(247,251, 20)  // Metal-3
		});

        setFactoryResolution(0.01); // value in lambdas   0.005um -> 0.05 lambdas

		//**************************************** LAYERS ****************************************
		/** metal-1 layer */
		metalLayers[0] = Layer.newInstance(this, "Metal-1",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_1, 96,209,255, 0.8,true,
			new int[] { 0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000}));//

		/** metal-2 layer */
		metalLayers[1] = Layer.newInstance(this, "Metal-2",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_4, 224,95,255, 0.7,true,
			new int[] { 0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808,   //     X       X
						0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808}));//     X       X

		/** metal-3 layer */
		metalLayers[2] = Layer.newInstance(this, "Metal-3",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_5, 247,251,20, 0.6,true,
			new int[] { 0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000}));//

		/** metal-4 layer */
		metalLayers[3] = Layer.newInstance(this, "Metal-4",
			new EGraphics(true, true, null, 0, 150,150,255, 0.5,true,
			new int[] { 0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000}));//

		/** metal-5 layer */
		metalLayers[4] = Layer.newInstance(this, "Metal-5",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 255,190,6, 0.4,true,
			new int[] { 0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444,   //  X   X   X   X
						0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444,   //  X   X   X   X
						0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444,   //  X   X   X   X
						0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444}));//  X   X   X   X

		/** metal-6 layer */
		metalLayers[5] = Layer.newInstance(this, "Metal-6",
			new EGraphics(true, true, null, 0, 0,255,255, 0.3,true,
			new int[] { 0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111}));//    X   X   X   X

		/** poly layer */
		poly1Layer = Layer.newInstance(this, "Polysilicon-1",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_2, 255,155,192, 1,true,
			new int[] { 0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** poly2 layer */
		poly2_lay = Layer.newInstance(this, "Polysilicon-2",
			new EGraphics(true, true, null, 0, 255,190,6, 1,true,
			new int[] { 0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888,   // X   X   X   X
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888,   // X   X   X   X
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888,   // X   X   X   X
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888}));// X   X   X   X

		/** P active layer */
		activeLayers[P_TYPE] = Layer.newInstance(this, "P-Active",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*107,204,0,*/ 1,true,
			new int[] { 0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030,   //   XX      XX
						0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030}));//   XX      XX

		/** N active layer */
		activeLayers[N_TYPE] = Layer.newInstance(this, "N-Active",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*107,204,0,*/ 1,true,
			new int[] { 0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030,   //   XX      XX
						0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030}));//   XX      XX

        selectLayers = new Layer[2];
		/** P Select layer */
		selectLayers[P_TYPE] = Layer.newInstance(this, "P-Select",
			new EGraphics(true, true, null, 0, 255,255,0, 1,false,
			new int[] { 0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808,   //     X       X
						0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808}));//     X       X

		/** N Select layer */
		selectLayers[N_TYPE] = Layer.newInstance(this, "N-Select",
			new EGraphics(true, true, null, 0, 255,255,0, 1,false,
			new int[] { 0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000,   //
						0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000,   //
						0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000,   //
						0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000}));//

		/** P Well layer */
		wellLayers[P_TYPE] = Layer.newInstance(this, "P-Well",
			new EGraphics(true, true, null, 0, 139,99,46, 1,false,
			new int[] { 0x0202,   //       X       X
						0x0101,   //        X       X
						0x8080,   // X       X
						0x4040,   //  X       X
						0x2020,   //   X       X
						0x1010,   //    X       X
						0x0808,   //     X       X
						0x0404,   //      X       X
						0x0202,   //       X       X
						0x0101,   //        X       X
						0x8080,   // X       X
						0x4040,   //  X       X
						0x2020,   //   X       X
						0x1010,   //    X       X
						0x0808,   //     X       X
						0x0404}));//      X       X

		/** N Well implant */
		wellLayers[N_TYPE] = Layer.newInstance(this, "N-Well",
			new EGraphics(true, true, null, 0, 139,99,46, 1,false,
			new int[] { 0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000,   //
						0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000,   //
						0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000,   //
						0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000}));//

		/** poly cut layer */
		polyCutLayer = Layer.newInstance(this, "Poly-Cut",
			new EGraphics(false, false, null, 0, 100,100,100, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** active cut layer */
		activeCutLayer = Layer.newInstance(this, "Active-Cut",
			new EGraphics(false, false, null, 0, 100,100,100, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via1->via5 layer */
        for (int i = 0; i < viaLayers.length; i++)
		    viaLayers[i] = Layer.newInstance(this, "Via"+(i+1),
                    new EGraphics(false, false, null, 0, 180,180,180, 1,true,
                            new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** passivation layer */
		passivationLayer = Layer.newInstance(this, "Passivation",
			new EGraphics(true, true, null, 0, 100,100,100, 1,true,
			new int[] { 0x1C1C,   //    XXX     XXX
						0x3E3E,   //   XXXXX   XXXXX
						0x3636,   //   XX XX   XX XX
						0x3E3E,   //   XXXXX   XXXXX
						0x1C1C,   //    XXX     XXX
						0x0000,   //
						0x0000,   //
						0x0000,   //
						0x1C1C,   //    XXX     XXX
						0x3E3E,   //   XXXXX   XXXXX
						0x3636,   //   XX XX   XX XX
						0x3E3E,   //   XXXXX   XXXXX
						0x1C1C,   //    XXX     XXX
						0x0000,   //
						0x0000,   //
						0x0000}));//

		/** poly/trans layer */
		transistorPolyLayer = Layer.newInstance(this, "Transistor-Poly",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_2, 255,155,192, 1,true,
			new int[] { 0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** poly cap layer */
		polyCapLayer = Layer.newInstance(this, "Poly-Cap",
			new EGraphics(false, false, null, 0, 0,0,0, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P act well layer */
		pActiveWellLayer = Layer.newInstance(this, "P-Active-Well",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*107,204,0,*/ 1,false,
			new int[] { 0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030,   //   XX      XX
						0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030}));//   XX      XX

		/** Silicide block */
        /** Resist Protection Oxide (RPO) Same graphics as in 90nm tech */
		silicideBlockLayer = Layer.newInstance(this, "Silicide-Block",
            new EGraphics(true, true, null, EGraphics.TRANSPARENT_2, 255,155,192,/*192,255,255,*/ 1,true,
            new int[] { 0x1010,  /*    X       X     */
                        0x2828,   /*   X X     X X    */
                        0x4444,   /*  X   X   X   X   */
                        0x8282,   /* X     X X     X  */
                        0x0101,   /*        X       X */
                        0x0000,   /*                  */
                        0x0000,   /*                  */
                        0x0000,   /*                  */
                        0x1010,   /*    X       X     */
                        0x2828,   /*   X X     X X    */
                        0x4444,   /*  X   X   X   X   */
                        0x8282,   /* X     X X     X  */
                        0x0101,   /*        X       X */
                        0x0000,   /*                  */
                        0x0000,   /*                  */
                        0x0000}));/*                  */


		/** Thick active */
		thickActiveLayer = Layer.newInstance(this, "Thick-Active",
			new EGraphics(true, true, null, 0, 0,0,0, 1,false,
			new int[] { 0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0101,   //        X       X
						0x8080,   // X       X
						0x4040,   //  X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0101,   //        X       X
						0x8080,   // X       X
						0x4040,   //  X       X
						0x2020}));//   X       X

		/** pad frame */
		padFrameLayer = Layer.newInstance(this, "Pad-Frame",
			new EGraphics(false, true, null, 0, 255,0,0, 1,false,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));
        
        createExtraLayers();

//		/** pseudo metal 1 */
//		Layer pseudoMetal1_lay = Layer.newInstance(this, "Pseudo-Metal-1",
//			new EGraphics(false, true, null, EGraphics.TRANSPARENT_1, 96,209,255, 1,true,
//			new int[] { 0x2222,   //   X   X   X   X
//						0x0000,   //
//						0x8888,   // X   X   X   X
//						0x0000,   //
//						0x2222,   //   X   X   X   X
//						0x0000,   //
//						0x8888,   // X   X   X   X
//						0x0000,   //
//						0x2222,   //   X   X   X   X
//						0x0000,   //
//						0x8888,   // X   X   X   X
//						0x0000,   //
//						0x2222,   //   X   X   X   X
//						0x0000,   //
//						0x8888,   // X   X   X   X
//						0x0000}));//
//
//		/** pseudo metal-2 */
//		Layer pseudoMetal2_lay = Layer.newInstance(this, "Pseudo-Metal-2",
//			new EGraphics(false, true, null, EGraphics.TRANSPARENT_4, 224,95,255, 1,true,
//			new int[] { 0x1010,   //    X       X
//						0x2020,   //   X       X
//						0x4040,   //  X       X
//						0x8080,   // X       X
//						0x0101,   //        X       X
//						0x0202,   //       X       X
//						0x0404,   //      X       X
//						0x0808,   //     X       X
//						0x1010,   //    X       X
//						0x2020,   //   X       X
//						0x4040,   //  X       X
//						0x8080,   // X       X
//						0x0101,   //        X       X
//						0x0202,   //       X       X
//						0x0404,   //      X       X
//						0x0808}));//     X       X
//
//		/** pseudo metal-3 */
//		Layer pseudoMetal3_lay = Layer.newInstance(this, "Pseudo-Metal-3",
//			new EGraphics(false, true, null, EGraphics.TRANSPARENT_5, 247,251,20, 1,true,
//			new int[] { 0x1010,   //    X       X
//						0x2020,   //   X       X
//						0x4040,   //  X       X
//						0x8080,   // X       X
//						0x0101,   //        X       X
//						0x0202,   //       X       X
//						0x0404,   //      X       X
//						0x0808,   //     X       X
//						0x1010,   //    X       X
//						0x2020,   //   X       X
//						0x4040,   //  X       X
//						0x8080,   // X       X
//						0x0101,   //        X       X
//						0x0202,   //       X       X
//						0x0404,   //      X       X
//						0x0808}));//     X       X
//
//		/** pseudo metal-4 */
//		Layer pseudoMetal4_lay = Layer.newInstance(this, "Pseudo-Metal-4",
//			new EGraphics(true, true, null, 0, 150,150,255, 1,true,
//			new int[] { 0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x0000,   //
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x0000,   //
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x0000,   //
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x0000,   //
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x0000,   //
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x0000,   //
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x0000,   //
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x0000}));//
//
//		/** pseudo metal-5 */
//		Layer pseudoMetal5_lay = Layer.newInstance(this, "Pseudo-Metal-5",
//			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 255,190,6, 1,true,
//			new int[] { 0x8888,   // X   X   X   X
//						0x1111,   //    X   X   X   X
//						0x2222,   //   X   X   X   X
//						0x4444,   //  X   X   X   X
//						0x8888,   // X   X   X   X
//						0x1111,   //    X   X   X   X
//						0x2222,   //   X   X   X   X
//						0x4444,   //  X   X   X   X
//						0x8888,   // X   X   X   X
//						0x1111,   //    X   X   X   X
//						0x2222,   //   X   X   X   X
//						0x4444,   //  X   X   X   X
//						0x8888,   // X   X   X   X
//						0x1111,   //    X   X   X   X
//						0x2222,   //   X   X   X   X
//						0x4444}));//  X   X   X   X
//
//		/** pseudo metal-6 */
//		Layer pseudoMetal6_lay = Layer.newInstance(this, "Pseudo-Metal-6",
//			new EGraphics(true, true, null, 0, 0,255,255, 1,true,
//			new int[] { 0x8888,   // X   X   X   X
//						0x4444,   //  X   X   X   X
//						0x2222,   //   X   X   X   X
//						0x1111,   //    X   X   X   X
//						0x8888,   // X   X   X   X
//						0x4444,   //  X   X   X   X
//						0x2222,   //   X   X   X   X
//						0x1111,   //    X   X   X   X
//						0x8888,   // X   X   X   X
//						0x4444,   //  X   X   X   X
//						0x2222,   //   X   X   X   X
//						0x1111,   //    X   X   X   X
//						0x8888,   // X   X   X   X
//						0x4444,   //  X   X   X   X
//						0x2222,   //   X   X   X   X
//						0x1111}));//    X   X   X   X
//
//		/** pseudo poly layer */
//		Layer pseudoPoly1_lay = Layer.newInstance(this, "Pseudo-Polysilicon",
//			new EGraphics(false, true, null, EGraphics.TRANSPARENT_2, 255,155,192, 1,true,
//			new int[] { 0x1111,   //    X   X   X   X
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x1111,   //    X   X   X   X
//						0x5555,   //  X X X X X X X X
//						0x1111,   //    X   X   X   X
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x1111,   //    X   X   X   X
//						0x5555,   //  X X X X X X X X
//						0x1111,   //    X   X   X   X
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x1111,   //    X   X   X   X
//						0x5555,   //  X X X X X X X X
//						0x1111,   //    X   X   X   X
//						0xFFFF,   // XXXXXXXXXXXXXXXX
//						0x1111,   //    X   X   X   X
//						0x5555}));//  X X X X X X X X
//
//		/** pseudo poly2 layer */
//		Layer pseudoPoly2_lay = Layer.newInstance(this, "Pseudo-Electrode",
//			new EGraphics(true, true, null, 0, 255,190,6, 1,true,
//			new int[] { 0xAFAF,   // X X XXXXX X XXXX
//						0x8888,   // X   X   X   X
//						0xFAFA,   // XXXXX X XXXXX X
//						0x8888,   // X   X   X   X
//						0xAFAF,   // X X XXXXX X XXXX
//						0x8888,   // X   X   X   X
//						0xFAFA,   // XXXXX X XXXXX X
//						0x8888,   // X   X   X   X
//						0xAFAF,   // X X XXXXX X XXXX
//						0x8888,   // X   X   X   X
//						0xFAFA,   // XXXXX X XXXXX X
//						0x8888,   // X   X   X   X
//						0xAFAF,   // X X XXXXX X XXXX
//						0x8888,   // X   X   X   X
//						0xFAFA,   // XXXXX X XXXXX X
//						0x8888}));// X   X   X   X
//
//		/** pseudo P active */
//		pseudoActiveLayers[P_TYPE] = Layer.newInstance(this, "Pseudo-P-Active",
//			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*107,204,0,*/ 1,true,
//			new int[] { 0x0000,   //
//						0x0303,   //       XX      XX
//						0x4848,   //  X  X    X  X
//						0x0303,   //       XX      XX
//						0x0000,   //
//						0x3030,   //   XX      XX
//						0x8484,   // X    X  X    X
//						0x3030,   //   XX      XX
//						0x0000,   //
//						0x0303,   //       XX      XX
//						0x4848,   //  X  X    X  X
//						0x0303,   //       XX      XX
//						0x0000,   //
//						0x3030,   //   XX      XX
//						0x8484,   // X    X  X    X
//						0x3030}));//   XX      XX
//
//		/** pseudo N active */
//		pseudoActiveLayers[N_TYPE] = Layer.newInstance(this, "Pseudo-N-Active",
//			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*107,204,0,*/ 1,true,
//			new int[] { 0x0000,   //
//						0x0303,   //       XX      XX
//						0x4848,   //  X  X    X  X
//						0x0303,   //       XX      XX
//						0x0000,   //
//						0x3030,   //   XX      XX
//						0x8484,   // X    X  X    X
//						0x3030,   //   XX      XX
//						0x0000,   //
//						0x0303,   //       XX      XX
//						0x4848,   //  X  X    X  X
//						0x0303,   //       XX      XX
//						0x0000,   //
//						0x3030,   //   XX      XX
//						0x8484,   // X    X  X    X
//						0x3030}));//   XX      XX
//
//		/** pseudo P Select */
//		pseudoSelectLayers[P_TYPE] = Layer.newInstance(this, "Pseudo-P-Select",
//			new EGraphics(true, true, null, 0, 255,255,0, 1,false,
//			new int[] { 0x1010,   //    X       X
//						0x2020,   //   X       X
//						0x4040,   //  X       X
//						0x8080,   // X       X
//						0x0101,   //        X       X
//						0x0202,   //       X       X
//						0x0404,   //      X       X
//						0x0808,   //     X       X
//						0x1010,   //    X       X
//						0x2020,   //   X       X
//						0x4040,   //  X       X
//						0x8080,   // X       X
//						0x0101,   //        X       X
//						0x0202,   //       X       X
//						0x0404,   //      X       X
//						0x0808}));//     X       X
//
//		/** pseudo N Select */
//		pseudoSelectLayers[N_TYPE] = Layer.newInstance(this, "Pseudo-N-Select",
//			new EGraphics(true, true, null, 0, 255,255,0, 1,false,
//			new int[] { 0x0101,   //        X       X
//						0x0000,   //
//						0x1010,   //    X       X
//						0x0000,   //
//						0x0101,   //        X       X
//						0x0000,   //
//						0x1010,   //    X       X
//						0x0000,   //
//						0x0101,   //        X       X
//						0x0000,   //
//						0x1010,   //    X       X
//						0x0000,   //
//						0x0101,   //        X       X
//						0x0000,   //
//						0x1010,   //    X       X
//						0x0000}));//
//
//		/** pseudo P Well */
//		pseudoWellLayers[P_TYPE] = Layer.newInstance(this, "Pseudo-P-Well",
//			new EGraphics(true, true, null, 0, 139,99,46, 1,false,
//			new int[] { 0x0202,   //       X       X
//						0x0101,   //        X       X
//						0x8080,   // X       X
//						0x4040,   //  X       X
//						0x2020,   //   X       X
//						0x1010,   //    X       X
//						0x0808,   //     X       X
//						0x0404,   //      X       X
//						0x0202,   //       X       X
//						0x0101,   //        X       X
//						0x8080,   // X       X
//						0x4040,   //  X       X
//						0x2020,   //   X       X
//						0x1010,   //    X       X
//						0x0808,   //     X       X
//						0x0404}));//      X       X
//
//		/** pseudo N Well */
//		pseudoWellLayers[N_TYPE] = Layer.newInstance(this, "Pseudo-N-Well",
//			new EGraphics(true, true, null, 0, 139,99,46, 1,false,
//			new int[] { 0x0202,   //       X       X
//						0x0000,   //
//						0x2020,   //   X       X
//						0x0000,   //
//						0x0202,   //       X       X
//						0x0000,   //
//						0x2020,   //   X       X
//						0x0000,   //
//						0x0202,   //       X       X
//						0x0000,   //
//						0x2020,   //   X       X
//						0x0000,   //
//						0x0202,   //       X       X
//						0x0000,   //
//						0x2020,   //   X       X
//						0x0000}));//

		// The layer functions
		metalLayers[0].setFunction(Layer.Function.METAL1);									// Metal-1
		metalLayers[1].setFunction(Layer.Function.METAL2);									// Metal-2
		metalLayers[2].setFunction(Layer.Function.METAL3);									// Metal-3
		metalLayers[3].setFunction(Layer.Function.METAL4);									// Metal-4
		metalLayers[4].setFunction(Layer.Function.METAL5);									// Metal-5
		metalLayers[5].setFunction(Layer.Function.METAL6);									// Metal-6
		poly1Layer.setFunction(Layer.Function.POLY1);									// Polysilicon-1
		poly2_lay.setFunction(Layer.Function.POLY2);									// Polysilicon-2
		activeLayers[P_TYPE].setFunction(Layer.Function.DIFFP);									// P-Active
		activeLayers[N_TYPE].setFunction(Layer.Function.DIFFN);									// N-Active
		selectLayers[P_TYPE].setFunction(Layer.Function.IMPLANTP);								// P-Select
		selectLayers[N_TYPE].setFunction(Layer.Function.IMPLANTN);								// N-Select
		wellLayers[P_TYPE].setFunction(Layer.Function.WELLP);									// P-Well
		wellLayers[N_TYPE].setFunction(Layer.Function.WELLN);									// N-Well
		polyCutLayer.setFunction(Layer.Function.CONTACT1, Layer.Function.CONPOLY);		// Poly-Cut
		activeCutLayer.setFunction(Layer.Function.CONTACT1, Layer.Function.CONDIFF);		// Active-Cut
		viaLayers[0].setFunction(Layer.Function.CONTACT2, Layer.Function.CONMETAL);			// Via-1
		viaLayers[1].setFunction(Layer.Function.CONTACT3, Layer.Function.CONMETAL);			// Via-2
		viaLayers[2].setFunction(Layer.Function.CONTACT4, Layer.Function.CONMETAL);			// Via-3
		viaLayers[3].setFunction(Layer.Function.CONTACT5, Layer.Function.CONMETAL);			// Via-4
		viaLayers[4].setFunction(Layer.Function.CONTACT6, Layer.Function.CONMETAL);			// Via-5
		passivationLayer.setFunction(Layer.Function.OVERGLASS);							// Passivation
		transistorPolyLayer.setFunction(Layer.Function.GATE);							// Transistor-Poly
		polyCapLayer.setFunction(Layer.Function.CAP);									// Poly-Cap
		pActiveWellLayer.setFunction(Layer.Function.DIFFP);								// P-Active-Well
		silicideBlockLayer.setFunction(Layer.Function.ART);								// Silicide-Block

		thickActiveLayer.setFunction(Layer.Function.DIFF, Layer.Function.THICK);			// Thick-Active
		padFrameLayer.setFunction(Layer.Function.ART);									// Pad-Frame
        if (pBaseLayer != null)
            pBaseLayer.setFunction(Layer.Function.BASE);
		Layer pseudoMetal1_lay = metalLayers[0].makePseudo();		// Pseudo-Metal-1
		Layer pseudoMetal2_lay = metalLayers[1].makePseudo();		// Pseudo-Metal-2
		Layer pseudoMetal3_lay = metalLayers[2].makePseudo();		// Pseudo-Metal-3
		Layer pseudoMetal4_lay = metalLayers[3].makePseudo();		// Pseudo-Metal-4
		Layer pseudoMetal5_lay = metalLayers[4].makePseudo();		// Pseudo-Metal-5
		Layer pseudoMetal6_lay = metalLayers[5].makePseudo();		// Pseudo-Metal-6
		Layer pseudoPoly1_lay = poly1Layer.makePseudo();            // Pseudo-Polysilicon-1
		Layer pseudoPoly2_lay = poly2_lay.makePseudo();             // Pseudo-Polysilicon-2
		pseudoActiveLayers[P_TYPE] = activeLayers[P_TYPE].makePseudo();		// Pseudo-P-Active
		pseudoActiveLayers[N_TYPE] = activeLayers[N_TYPE].makePseudo();		// Pseudo-N-Active
		pseudoSelectLayers[P_TYPE] = selectLayers[P_TYPE].makePseudo();	// Pseudo-P-Select
		pseudoSelectLayers[N_TYPE] = selectLayers[N_TYPE].makePseudo();	// Pseudo-N-Select
		pseudoWellLayers[P_TYPE] = wellLayers[P_TYPE].makePseudo();		// Pseudo-P-Well
		pseudoWellLayers[N_TYPE] = wellLayers[N_TYPE].makePseudo();		// Pseudo-N-Well
//		pseudoMetal1_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo-Metal-1
//		pseudoMetal2_lay.setFunction(Layer.Function.METAL2, Layer.Function.PSEUDO);		// Pseudo-Metal-2
//		pseudoMetal3_lay.setFunction(Layer.Function.METAL3, Layer.Function.PSEUDO);		// Pseudo-Metal-3
//		pseudoMetal4_lay.setFunction(Layer.Function.METAL4, Layer.Function.PSEUDO);		// Pseudo-Metal-4
//		pseudoMetal5_lay.setFunction(Layer.Function.METAL5, Layer.Function.PSEUDO);		// Pseudo-Metal-5
//		pseudoMetal6_lay.setFunction(Layer.Function.METAL6, Layer.Function.PSEUDO);		// Pseudo-Metal-6
//		pseudoPoly1_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);		// Pseudo-Polysilicon-1
//		pseudoPoly2_lay.setFunction(Layer.Function.POLY2, Layer.Function.PSEUDO);		// Pseudo-Polysilicon-2
//		pseudoActiveLayers[P_TYPE].setFunction(Layer.Function.DIFFP, Layer.Function.PSEUDO);		// Pseudo-P-Active
//		pseudoActiveLayers[N_TYPE].setFunction(Layer.Function.DIFFN, Layer.Function.PSEUDO);		// Pseudo-N-Active
//		pseudoSelectLayers[P_TYPE].setFunction(Layer.Function.IMPLANTP, Layer.Function.PSEUDO);	// Pseudo-P-Select
//		pseudoSelectLayers[N_TYPE].setFunction(Layer.Function.IMPLANTN, Layer.Function.PSEUDO);	// Pseudo-N-Select
//		pseudoWellLayers[P_TYPE].setFunction(Layer.Function.WELLP, Layer.Function.PSEUDO);		// Pseudo-P-Well
//		pseudoWellLayers[N_TYPE].setFunction(Layer.Function.WELLN, Layer.Function.PSEUDO);		// Pseudo-N-Well

		// The CIF names
		metalLayers[0].setFactoryCIFLayer("CMF");				// Metal-1
		metalLayers[1].setFactoryCIFLayer("CMS");				// Metal-2
		metalLayers[2].setFactoryCIFLayer("CMT");				// Metal-3
		metalLayers[3].setFactoryCIFLayer("CMQ");				// Metal-4
		metalLayers[4].setFactoryCIFLayer("CMP");				// Metal-5
		metalLayers[5].setFactoryCIFLayer("CM6");				// Metal-6
		poly1Layer.setFactoryCIFLayer("CPG");				// Polysilicon-1
		poly2_lay.setFactoryCIFLayer("CEL");				// Polysilicon-2
		activeLayers[P_TYPE].setFactoryCIFLayer("CAA");				// P-Active
		activeLayers[N_TYPE].setFactoryCIFLayer("CAA");				// N-Active
		selectLayers[P_TYPE].setFactoryCIFLayer("CSP");				// P-Select
		selectLayers[N_TYPE].setFactoryCIFLayer("CSN");				// N-Select
		wellLayers[P_TYPE].setFactoryCIFLayer("CWP");				// P-Well
		wellLayers[N_TYPE].setFactoryCIFLayer("CWN");				// N-Well
		polyCutLayer.setFactoryCIFLayer("CCC");				// Poly-Cut
		activeCutLayer.setFactoryCIFLayer("CCC");			// Active-Cut
		viaLayers[0].setFactoryCIFLayer("CVA");					// Via-1
		viaLayers[1].setFactoryCIFLayer("CVS");					// Via-2
		viaLayers[2].setFactoryCIFLayer("CVT");					// Via-3
		viaLayers[3].setFactoryCIFLayer("CVQ");					// Via-4
		viaLayers[4].setFactoryCIFLayer("CV5");					// Via-5
		passivationLayer.setFactoryCIFLayer("COG");			// Passivation
		transistorPolyLayer.setFactoryCIFLayer("CPG");		// Transistor-Poly
		polyCapLayer.setFactoryCIFLayer("CPC");				// Poly-Cap
		pActiveWellLayer.setFactoryCIFLayer("CAA");			// P-Active-Well
		silicideBlockLayer.setFactoryCIFLayer("CSB");		// Silicide-Block
		thickActiveLayer.setFactoryCIFLayer("CTA");			// Thick-Active
		padFrameLayer.setFactoryCIFLayer("XP");				// Pad-Frame
//		pseudoMetal1_lay.setFactoryCIFLayer("");			// Pseudo-Metal-1
//		pseudoMetal2_lay.setFactoryCIFLayer("");			// Pseudo-Metal-2
//		pseudoMetal3_lay.setFactoryCIFLayer("");			// Pseudo-Metal-3
//		pseudoMetal4_lay.setFactoryCIFLayer("");			// Pseudo-Metal-4
//		pseudoMetal5_lay.setFactoryCIFLayer("");			// Pseudo-Metal-5
//		pseudoMetal6_lay.setFactoryCIFLayer("");			// Pseudo-Metal-6
//		pseudoPoly1_lay.setFactoryCIFLayer("");				// Pseudo-Polysilicon-1
//		pseudoPoly2_lay.setFactoryCIFLayer("");				// Pseudo-Polysilicon-2
//		pseudoActiveLayers[P_TYPE].setFactoryCIFLayer("");			// Pseudo-P-Active
//		pseudoActiveLayers[N_TYPE].setFactoryCIFLayer("");			// Pseudo-N-Active
//		pseudoSelectLayers[P_TYPE].setFactoryCIFLayer("CSP");		// Pseudo-P-Select
//		pseudoSelectLayers[N_TYPE].setFactoryCIFLayer("CSN");		// Pseudo-N-Select
//		pseudoWellLayers[P_TYPE].setFactoryCIFLayer("CWP");			// Pseudo-P-Well
//		pseudoWellLayers[N_TYPE].setFactoryCIFLayer("CWN");			// Pseudo-N-Well

		// The Skill names
		metalLayers[0].setFactorySkillLayer("metal1");			// Metal-1
		metalLayers[1].setFactorySkillLayer("metal2");			// Metal-2
		metalLayers[2].setFactorySkillLayer("metal3");			// Metal-3
		metalLayers[3].setFactorySkillLayer("metal4");			// Metal-4
		metalLayers[4].setFactorySkillLayer("metal5");			// Metal-5
		metalLayers[5].setFactorySkillLayer("metal6");			// Metal-6
		poly1Layer.setFactorySkillLayer("poly");				// Polysilicon-1
		poly2_lay.setFactorySkillLayer("");					// Polysilicon-2
		activeLayers[P_TYPE].setFactorySkillLayer("aa");				// P-Active
		activeLayers[N_TYPE].setFactorySkillLayer("aa");				// N-Active
		selectLayers[P_TYPE].setFactorySkillLayer("pplus");			// P-Select
		selectLayers[N_TYPE].setFactorySkillLayer("nplus");			// N-Select
		wellLayers[P_TYPE].setFactorySkillLayer("pwell");			// P-Well
		wellLayers[N_TYPE].setFactorySkillLayer("nwell");			// N-Well
		polyCutLayer.setFactorySkillLayer("pcont");			// Poly-Cut
		activeCutLayer.setFactorySkillLayer("acont");		// Active-Cut
		viaLayers[0].setFactorySkillLayer("via");				// Via-1
		viaLayers[1].setFactorySkillLayer("via2");				// Via-2
		viaLayers[2].setFactorySkillLayer("via3");				// Via-3
		viaLayers[3].setFactorySkillLayer("via4");				// Via-4
		viaLayers[4].setFactorySkillLayer("via5");				// Via-5
		passivationLayer.setFactorySkillLayer("glasscut");	// Passivation
		transistorPolyLayer.setFactorySkillLayer("poly");	// Transistor-Poly
		polyCapLayer.setFactorySkillLayer("");				// Poly-Cap
		pActiveWellLayer.setFactorySkillLayer("aa");			// P-Active-Well
		silicideBlockLayer.setFactorySkillLayer("");	    // Silicide-Block
        thickActiveLayer.setFactorySkillLayer("");			// Thick-Active
		padFrameLayer.setFactorySkillLayer("");				// Pad-Frame
//		pseudoMetal1_lay.setFactorySkillLayer("");			// Pseudo-Metal-1
//		pseudoMetal2_lay.setFactorySkillLayer("");			// Pseudo-Metal-2
//		pseudoMetal3_lay.setFactorySkillLayer("");			// Pseudo-Metal-3
//		pseudoMetal4_lay.setFactorySkillLayer("");			// Pseudo-Metal-4
//		pseudoMetal5_lay.setFactorySkillLayer("");			// Pseudo-Metal-5
//		pseudoMetal6_lay.setFactorySkillLayer("");			// Pseudo-Metal-6
//		pseudoPoly1_lay.setFactorySkillLayer("");			// Pseudo-Polysilicon-1
//		pseudoPoly2_lay.setFactorySkillLayer("");			// Pseudo-Polysilicon-2
//		pseudoActiveLayers[P_TYPE].setFactorySkillLayer("");			// Pseudo-P-Active
//		pseudoActiveLayers[N_TYPE].setFactorySkillLayer("");			// Pseudo-N-Active
//		pseudoSelectLayers[P_TYPE].setFactorySkillLayer("pplus");	// Pseudo-P-Select
//		pseudoSelectLayers[N_TYPE].setFactorySkillLayer("nplus");	// Pseudo-N-Select
//		pseudoWellLayers[P_TYPE].setFactorySkillLayer("pwell");		// Pseudo-P-Well
//		pseudoWellLayers[N_TYPE].setFactorySkillLayer("nwell");		// Pseudo-N-Well

        // Logical Effort Tech-dependent settings
        setFactoryLESettings(0.167, 0.16, 0.7);
//        setGateCapacitance(0.167);
//        setWireRatio(0.16);
//        setDiffAlpha(0.7);

		// The layer distance
		// Metal values based on 18nm technology with 200nm as grid unit.
		double BULK_LAYER = 0, WELL_THICKNESS = 13; // proposed by J. Baker.
        double SELECT_THICKNESS = 5, SELECT_DISTANCE = 8; // proposed by J. Baker. Distance from BULK_LAYER
        double ACTIVE_THICKNESS = 4, ACTIVE_DISTANCE = 9; // proposed by J. Baker. Distance from BULK_LAYER
        double THICKACT_THICKNESS = 7, THICKACT_DISTANCE = 6; // values adapted after suggestions from J. Baker.
		double ILD_LAYER = 3.5; // 0.7/0.2     convertLength()
		double IMD_LAYER = 5.65; // 1.13um/0.2
		double METAL_LAYER = 2.65; // 0.53um/0.2
		wellLayers[P_TYPE].setFactory3DInfo(WELL_THICKNESS, BULK_LAYER, "NICEST", 0.8);					// P-Well
		wellLayers[N_TYPE].setFactory3DInfo(WELL_THICKNESS, BULK_LAYER, "NICEST", 0.8);					// N-Well
		selectLayers[P_TYPE].setFactory3DInfo(SELECT_THICKNESS, SELECT_DISTANCE, "SCREEN_DOOR", 0.4);				// P-Select
		selectLayers[N_TYPE].setFactory3DInfo(SELECT_THICKNESS, SELECT_DISTANCE, "SCREEN_DOOR", 0.4);				// N-Select

        activeLayers[P_TYPE].setFactory3DInfo(ACTIVE_THICKNESS, ACTIVE_DISTANCE, "NONE", 0.0);				// P-Active old values=0.17um/0.2 =
		activeLayers[N_TYPE].setFactory3DInfo(ACTIVE_THICKNESS, ACTIVE_DISTANCE, "NONE", 0.0);				// N-Active old values=0.16um/0.2
        pActiveWellLayer.setFactory3DInfo(ACTIVE_THICKNESS, ACTIVE_DISTANCE, "NONE", 0.0);			// P-Active-Well
        thickActiveLayer.setFactory3DInfo(THICKACT_THICKNESS, THICKACT_DISTANCE, "NICEST", 0.6);			// Thick Active (between select and well)

		metalLayers[0].setFactory3DInfo(METAL_LAYER, ILD_LAYER + activeLayers[P_TYPE].getDepth(), "NONE", 0.2);					// Metal-1   0.53um/0.2
		metalLayers[1].setFactory3DInfo(METAL_LAYER, IMD_LAYER + metalLayers[0].getDistance(), "NONE", 0.2);					// Metal-2
		viaLayers[0].setFactory3DInfo(metalLayers[1].getDistance()-metalLayers[0].getDepth(), metalLayers[0].getDepth(), "NONE", 0.2);					// Via-1

		metalLayers[2].setFactory3DInfo(METAL_LAYER, IMD_LAYER + metalLayers[1].getDistance(), "NONE", 0.2);					// Metal-3
		viaLayers[1].setFactory3DInfo(metalLayers[2].getDistance()-metalLayers[1].getDepth(), metalLayers[1].getDepth(), "NONE", 0.2);					// Via-2

		metalLayers[3].setFactory3DInfo(METAL_LAYER, IMD_LAYER + metalLayers[2].getDistance(), "NONE", 0.2);					// Metal-4
        viaLayers[2].setFactory3DInfo(metalLayers[3].getDistance()-metalLayers[2].getDepth(), metalLayers[2].getDepth(), "NONE", 0.2);					// Via-3

		metalLayers[4].setFactory3DInfo(METAL_LAYER, IMD_LAYER + metalLayers[3].getDistance(), "NONE", 0.2);					// Metal-5
		viaLayers[3].setFactory3DInfo(metalLayers[4].getDistance()-metalLayers[3].getDepth(), metalLayers[3].getDepth(), "NONE", 0.2);					// Via-4

		metalLayers[5].setFactory3DInfo(4.95, IMD_LAYER + metalLayers[4].getDistance(), "NONE", 0.2);					// Metal-6 0.99um/0.2
        viaLayers[4].setFactory3DInfo(metalLayers[5].getDistance()-metalLayers[4].getDepth(), metalLayers[4].getDepth(), "NONE", 0.2);					// Via-5

		double PASS_LAYER = 5; // 1um/0.2
		double PO_LAYER = 1; // 0.2/0.2
		double FOX_LAYER = 1.75; // 0.35/0.2
        double FOX_LAYER_POLY2 = 2.85; // 15.85 - 13 requested by J. Baker.
        double TOX_LAYER = 0; // Very narrow thin oxide in gate

//		/* for displaying pins */
//		pseudoMetal1_lay.setFactory3DInfo(0, metalLayers[0].getDistance());			// Pseudo-Metal-1
//		pseudoMetal2_lay.setFactory3DInfo(0, metalLayers[1].getDistance());			// Pseudo-Metal-2
//		pseudoMetal3_lay.setFactory3DInfo(0, metalLayers[2].getDistance());			// Pseudo-Metal-3
//		pseudoMetal4_lay.setFactory3DInfo(0, metalLayers[3].getDistance());			// Pseudo-Metal-4
//		pseudoMetal5_lay.setFactory3DInfo(0, metalLayers[4].getDistance());			// Pseudo-Metal-5
//		pseudoMetal6_lay.setFactory3DInfo(0, metalLayers[5].getDistance());			// Pseudo-Metal-6

		// Poly layers
        double activeDepth = activeLayers[P_TYPE].getDepth();
        poly1Layer.setFactory3DInfo(PO_LAYER, FOX_LAYER + activeDepth, "NONE", 0.2);					// Polysilicon-1
		transistorPolyLayer.setFactory3DInfo(PO_LAYER, TOX_LAYER + activeDepth, "NONE", 0.2);			// Transistor-Poly
        poly2_lay.setFactory3DInfo(PO_LAYER, FOX_LAYER_POLY2 + activeDepth, "NONE", 0.2);					// Polysilicon-2 // on top of transistor layer?
		polyCapLayer.setFactory3DInfo(PO_LAYER, FOX_LAYER + activeDepth, "NONE", 0.2);				// Poly-Cap @TODO GVG Ask polyCap

		polyCutLayer.setFactory3DInfo(metalLayers[0].getDistance()-poly1Layer.getDepth(), poly1Layer.getDepth(), "NONE", 0.2);				// Poly-Cut between poly and metal1
		activeCutLayer.setFactory3DInfo(metalLayers[0].getDistance()-activeLayers[N_TYPE].getDepth(), activeLayers[N_TYPE].getDepth(), "NONE", 0.2);				// Active-Cut betweent active and metal1

		// Other layers
		passivationLayer.setFactory3DInfo(PASS_LAYER, metalLayers[5].getDepth(), "NONE", 0.2);			// Passivation
		silicideBlockLayer.setFactory3DInfo(0, BULK_LAYER, "NONE", 0.2);			// Silicide-Block
        padFrameLayer.setFactory3DInfo(0, passivationLayer.getDepth(), "NONE", 0.2);				// Pad-Frame

//		pseudoPoly1_lay.setFactory3DInfo(0, poly1Layer.getDistance());			// Pseudo-Polysilicon-1
//		pseudoPoly2_lay.setFactory3DInfo(0, poly2_lay.getDistance());			// Pseudo-Polysilicon-2
//		pseudoActiveLayers[P_TYPE].setFactory3DInfo(0, activeLayers[P_TYPE].getDistance());			// Pseudo-P-Active
//		pseudoActiveLayers[N_TYPE].setFactory3DInfo(0, activeLayers[N_TYPE].getDistance());			// Pseudo-N-Active
//		pseudoSelectLayers[P_TYPE].setFactory3DInfo(0, selectLayers[P_TYPE].getDistance());			// Pseudo-P-Select
//		pseudoSelectLayers[N_TYPE].setFactory3DInfo(0, selectLayers[N_TYPE].getDistance());			// Pseudo-N-Select
//		pseudoWellLayers[P_TYPE].setFactory3DInfo(0, wellLayers[P_TYPE].getDistance());				// Pseudo-P-Well
//		pseudoWellLayers[N_TYPE].setFactory3DInfo(0, wellLayers[N_TYPE].getDistance());				// Pseudo-N-Well

		// The Spice parasitics
		metalLayers[0].setFactoryParasitics(0.078, 0.1209, 0.1104);			// Metal-1
		metalLayers[1].setFactoryParasitics(0.078, 0.0843, 0.0974);			// Metal-2
		metalLayers[2].setFactoryParasitics(0.078, 0.0843, 0.0974);			// Metal-3
		metalLayers[3].setFactoryParasitics(0.078, 0.0843, 0.0974);			// Metal-4
		metalLayers[4].setFactoryParasitics(0.078, 0.0843, 0.0974);			// Metal-5
		metalLayers[5].setFactoryParasitics(0.036, 0.0423, 0.1273);			// Metal-6
		poly1Layer.setFactoryParasitics(6.2, 0.1467, 0.0608);			// Polysilicon-1
		poly2_lay.setFactoryParasitics(50.0, 1.0, 0);			// Polysilicon-2
		activeLayers[P_TYPE].setFactoryParasitics(2.5, 0.9, 0);			// P-Active
		activeLayers[N_TYPE].setFactoryParasitics(3.0, 0.9, 0);			// N-Active
		selectLayers[P_TYPE].setFactoryParasitics(0, 0, 0);				// P-Select
		selectLayers[N_TYPE].setFactoryParasitics(0, 0, 0);				// N-Select
		wellLayers[P_TYPE].setFactoryParasitics(0, 0, 0);				// P-Well
		wellLayers[N_TYPE].setFactoryParasitics(0, 0, 0);				// N-Well
		polyCutLayer.setFactoryParasitics(2.2, 0, 0);			// Poly-Cut
		activeCutLayer.setFactoryParasitics(2.5, 0, 0);			// Active-Cut
		viaLayers[0].setFactoryParasitics(1.0, 0, 0);				// Via-1
		viaLayers[1].setFactoryParasitics(0.9, 0, 0);				// Via-2
		viaLayers[2].setFactoryParasitics(0.8, 0, 0);				// Via-3
		viaLayers[3].setFactoryParasitics(0.8, 0, 0);				// Via-4
		viaLayers[4].setFactoryParasitics(0.8, 0, 0);				// Via-5
		passivationLayer.setFactoryParasitics(0, 0, 0);			// Passivation
		transistorPolyLayer.setFactoryParasitics(2.5, 0.09, 0);	// Transistor-Poly
		polyCapLayer.setFactoryParasitics(0, 0, 0);				// Poly-Cap
		pActiveWellLayer.setFactoryParasitics(0, 0, 0);			// P-Active-Well
		silicideBlockLayer.setFactoryParasitics(0, 0, 0);		// Silicide-Block
        thickActiveLayer.setFactoryParasitics(0, 0, 0);			// Thick-Active
//		pseudoMetal1_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-1
//		pseudoMetal2_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-2
//		pseudoMetal3_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-3
//		pseudoMetal4_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-4
//		pseudoMetal5_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-5
//		pseudoMetal6_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-6
//		pseudoPoly1_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Polysilicon-1
//		pseudoPoly2_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Polysilicon-2
//		pseudoActiveLayers[P_TYPE].setFactoryParasitics(0, 0, 0);		// Pseudo-P-Active
//		pseudoActiveLayers[N_TYPE].setFactoryParasitics(0, 0, 0);		// Pseudo-N-Active
//		pseudoSelectLayers[P_TYPE].setFactoryParasitics(0, 0, 0);		// Pseudo-P-Select
//		pseudoSelectLayers[N_TYPE].setFactoryParasitics(0, 0, 0);		// Pseudo-N-Select
//		pseudoWellLayers[P_TYPE].setFactoryParasitics(0, 0, 0);			// Pseudo-P-Well
//		pseudoWellLayers[N_TYPE].setFactoryParasitics(0, 0, 0);			// Pseudo-N-Well
		padFrameLayer.setFactoryParasitics(0, 0, 0);				// Pad-Frame

		setFactoryParasitics(4, 0.1);

		String [] headerLevel1 =
		{
			"*CMOS/BULK-NWELL (PRELIMINARY PARAMETERS)",
			".OPTIONS NOMOD DEFL=3UM DEFW=3UM DEFAD=70P DEFAS=70P LIMPTS=1000",
			"+ITL5=0 RELTOL=0.01 ABSTOL=500PA VNTOL=500UV LVLTIM=2",
			"+LVLCOD=1",
			".MODEL N NMOS LEVEL=1",
			"+KP=60E-6 VTO=0.7 GAMMA=0.3 LAMBDA=0.05 PHI=0.6",
			"+LD=0.4E-6 TOX=40E-9 CGSO=2.0E-10 CGDO=2.0E-10 CJ=.2MF/M^2",
			".MODEL P PMOS LEVEL=1",
			"+KP=20E-6 VTO=0.7 GAMMA=0.4 LAMBDA=0.05 PHI=0.6",
			"+LD=0.6E-6 TOX=40E-9 CGSO=3.0E-10 CGDO=3.0E-10 CJ=.2MF/M^2",
			".MODEL DIFFCAP D CJO=.2MF/M^2"
		};
		setSpiceHeaderLevel1(headerLevel1);
		String [] headerLevel2 =
		{
			"* MOSIS 3u CMOS PARAMS",
			".OPTIONS NOMOD DEFL=2UM DEFW=6UM DEFAD=100P DEFAS=100P",
			"+LIMPTS=1000 ITL5=0 ABSTOL=500PA VNTOL=500UV",
			"* Note that ITL5=0 sets ITL5 to infinity",
			".MODEL N NMOS LEVEL=2 LD=0.3943U TOX=502E-10",
			"+NSUB=1.22416E+16 VTO=0.756 KP=4.224E-05 GAMMA=0.9241",
			"+PHI=0.6 UO=623.661 UEXP=8.328627E-02 UCRIT=54015.0",
			"+DELTA=5.218409E-03 VMAX=50072.2 XJ=0.4U LAMBDA=2.975321E-02",
			"+NFS=4.909947E+12 NEFF=1.001E-02 NSS=0.0 TPG=1.0",
			"+RSH=20.37 CGDO=3.1E-10 CGSO=3.1E-10",
			"+CJ=3.205E-04 MJ=0.4579 CJSW=4.62E-10 MJSW=0.2955 PB=0.7",
			".MODEL P PMOS LEVEL=2 LD=0.2875U TOX=502E-10",
			"+NSUB=1.715148E+15 VTO=-0.7045 KP=1.686E-05 GAMMA=0.3459",
			"+PHI=0.6 UO=248.933 UEXP=1.02652 UCRIT=182055.0",
			"+DELTA=1.0E-06 VMAX=100000.0 XJ=0.4U LAMBDA=1.25919E-02",
			"+NFS=1.0E+12 NEFF=1.001E-02 NSS=0.0 TPG=-1.0",
			"+RSH=79.10 CGDO=2.89E-10 CGSO=2.89E-10",
			"+CJ=1.319E-04 MJ=0.4125 CJSW=3.421E-10 MJSW=0.198 PB=0.66",
			".TEMP 25.0"
		};
		setSpiceHeaderLevel2(headerLevel2);

		//**************************************** ARCS ****************************************

		/** metal 1 arc */
		metalArcs[0] = newArcProto("Metal-1", 0, 3.0, ArcProto.Function.METAL1,
			new Technology.ArcLayer(metalLayers[0], 0, Poly.Type.FILLED)
		);
		metalArcs[0].setFactoryFixedAngle(true);
		metalArcs[0].setWipable();
		metalArcs[0].setFactoryAngleIncrement(90);

		/** metal 2 arc */
		metalArcs[1] = newArcProto("Metal-2", 0, 3.0, ArcProto.Function.METAL2,
			new Technology.ArcLayer(metalLayers[1], 0, Poly.Type.FILLED)
		);
		metalArcs[1].setFactoryFixedAngle(true);
		metalArcs[1].setWipable();
		metalArcs[1].setFactoryAngleIncrement(90);

		/** metal 3 arc */
		metalArcs[2] = newArcProto("Metal-3", 0, 3.0, ArcProto.Function.METAL3,
			new Technology.ArcLayer(metalLayers[2], 0, Poly.Type.FILLED)
		);
		metalArcs[2].setFactoryFixedAngle(true);
		metalArcs[2].setWipable();
		metalArcs[2].setFactoryAngleIncrement(90);

		/** metal 4 arc */
		metalArcs[3] = newArcProto("Metal-4", 0, 3.0, ArcProto.Function.METAL4,
			new Technology.ArcLayer(metalLayers[3], 0, Poly.Type.FILLED)
		);
		metalArcs[3].setFactoryFixedAngle(true);
		metalArcs[3].setWipable();
		metalArcs[3].setFactoryAngleIncrement(90);

		/** metal 5 arc */
		metalArcs[4] = newArcProto("Metal-5", 0, 3.0, ArcProto.Function.METAL5,
			new Technology.ArcLayer(metalLayers[4], 0, Poly.Type.FILLED)
		);
		metalArcs[4].setFactoryFixedAngle(true);
		metalArcs[4].setWipable();
		metalArcs[4].setFactoryAngleIncrement(90);

		/** metal 6 arc */
		metalArcs[5] = newArcProto("Metal-6", 0, 5.0, ArcProto.Function.METAL6,
			new Technology.ArcLayer(metalLayers[5], 0, Poly.Type.FILLED)
		);
		metalArcs[5].setFactoryFixedAngle(true);
		metalArcs[5].setWipable();
		metalArcs[5].setFactoryAngleIncrement(90);

		/** polysilicon 1 arc */
		polyArcs[0] = newArcProto("Polysilicon-1", 0, 2.0, ArcProto.Function.POLY1,
			new Technology.ArcLayer(poly1Layer, 0, Poly.Type.FILLED)
		);
		polyArcs[0].setFactoryFixedAngle(true);
		polyArcs[0].setWipable();
		polyArcs[0].setFactoryAngleIncrement(90);

		/** polysilicon 2 arc */
		polyArcs[1] = newArcProto("Polysilicon-2", 0, 7.0, ArcProto.Function.POLY2,
			new Technology.ArcLayer(poly2_lay, 0, Poly.Type.FILLED)
		);
		polyArcs[1].setFactoryFixedAngle(true);
		polyArcs[1].setWipable();
		polyArcs[1].setFactoryAngleIncrement(90);
//		polyArcs[1].setNotUsed(true);

		/** P-active arc */
		activeArcs[P_TYPE] = newArcProto("P-Active", 12.0, 15.0, ArcProto.Function.DIFFP,
			new Technology.ArcLayer(activeLayers[P_TYPE], 12, Poly.Type.FILLED),
			new Technology.ArcLayer(wellLayers[N_TYPE], 0, Poly.Type.FILLED),
			new Technology.ArcLayer(selectLayers[P_TYPE], 8, Poly.Type.FILLED)
		);
		activeArcs[P_TYPE].setFactoryFixedAngle(true);
		activeArcs[P_TYPE].setWipable();
		activeArcs[P_TYPE].setFactoryAngleIncrement(90);

		/** N-active arc */
		activeArcs[N_TYPE] = newArcProto("N-Active", 12.0, 15.0, ArcProto.Function.DIFFN,
			new Technology.ArcLayer(activeLayers[N_TYPE], 12, Poly.Type.FILLED),
			new Technology.ArcLayer(wellLayers[P_TYPE], 0, Poly.Type.FILLED),
			new Technology.ArcLayer(selectLayers[N_TYPE], 8, Poly.Type.FILLED)
		);
		activeArcs[N_TYPE].setFactoryFixedAngle(true);
		activeArcs[N_TYPE].setWipable();
		activeArcs[N_TYPE].setFactoryAngleIncrement(90);

		/** General active arc */
		active_arc = newArcProto("Active", 0, 3.0, ArcProto.Function.DIFF,
			new Technology.ArcLayer(activeLayers[P_TYPE], 0, Poly.Type.FILLED),
			new Technology.ArcLayer(activeLayers[N_TYPE], 0, Poly.Type.FILLED)
		);
		active_arc.setFactoryFixedAngle(true);
		active_arc.setWipable();
		active_arc.setFactoryAngleIncrement(90);
//		active_arc.setNotUsed(true);

		//**************************************** NODES ****************************************

        for (int i = 0; i < metalArcs.length; i++) {
            ArcProto ap = metalArcs[i];
            metalPinNodes[i] = ap.makeWipablePin("Metal-"+(i+1)+"-Pin", "metal-"+(i+1), 0, 3.0);
        }
        polyPinNodes[0] = polyArcs[0].makeWipablePin("Polysilicon-1-Pin", "polysilicon-1", 0, 2.0);
        polyPinNodes[1] = polyArcs[1].makeWipablePin("Polysilicon-2-Pin", "polysilicon-2", 0, 3.0);
        activePinNodes[P_TYPE] = activeArcs[P_TYPE].makeWipablePin("P-Active-Pin", "p-active", 6, 15.0);
        activePinNodes[N_TYPE] = activeArcs[N_TYPE].makeWipablePin("N-Active-Pin", "n-active", 6, 15.0);
		PrimitiveNode activeGenPinNode = active_arc.makeWipablePin("Active-Pin", "active", 0, 3.0, activeArcs[P_TYPE], activeArcs[N_TYPE]);

//		/** metal-1-pin */
//		metalPinNodes[0] = PrimitiveNode.newInstance("Metal-1-Pin", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoMetal1_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalPinNodes[0].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalPinNodes[0], new ArcProto[] {metalArcs[0]}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
//			});
//		metalPinNodes[0].setFunction(PrimitiveNode.Function.PIN);
//		metalPinNodes[0].setArcsWipe();
//		metalPinNodes[0].setArcsShrink();
//
//		/** metal-2-pin */
//		metalPinNodes[1] = PrimitiveNode.newInstance("Metal-2-Pin", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoMetal2_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalPinNodes[1].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalPinNodes[1], new ArcProto[] {metalArcs[1]}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
//			});
//		metalPinNodes[1].setFunction(PrimitiveNode.Function.PIN);
//		metalPinNodes[1].setArcsWipe();
//		metalPinNodes[1].setArcsShrink();
//
//		/** metal-3-pin */
//		metalPinNodes[2] = PrimitiveNode.newInstance("Metal-3-Pin", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoMetal3_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalPinNodes[2].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalPinNodes[2], new ArcProto[] {metalArcs[2]}, "metal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
//			});
//		metalPinNodes[2].setFunction(PrimitiveNode.Function.PIN);
//		metalPinNodes[2].setArcsWipe();
//		metalPinNodes[2].setArcsShrink();
//
//		/** metal-4-pin */
//		metalPinNodes[3] = PrimitiveNode.newInstance("Metal-4-Pin", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoMetal4_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalPinNodes[3].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalPinNodes[3], new ArcProto[] {metalArcs[3]}, "metal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
//			});
//		metalPinNodes[3].setFunction(PrimitiveNode.Function.PIN);
//		metalPinNodes[3].setArcsWipe();
//		metalPinNodes[3].setArcsShrink();
//
//		/** metal-5-pin */
//		metalPinNodes[4] = PrimitiveNode.newInstance("Metal-5-Pin", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoMetal5_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalPinNodes[4].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalPinNodes[4], new ArcProto[] {metalArcs[4]}, "metal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
//			});
//		metalPinNodes[4].setFunction(PrimitiveNode.Function.PIN);
//		metalPinNodes[4].setArcsWipe();
//		metalPinNodes[4].setArcsShrink();
////		metalPinNodes[4].setNotUsed(true);
//
//		/** metal-6-pin */
//		metalPinNodes[5] = PrimitiveNode.newInstance("Metal-6-Pin", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoMetal6_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalPinNodes[5].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalPinNodes[5], new ArcProto[] {metalArcs[5]}, "metal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
//			});
//		metalPinNodes[5].setFunction(PrimitiveNode.Function.PIN);
//		metalPinNodes[5].setArcsWipe();
//		metalPinNodes[5].setArcsShrink();
////		metalPinNodes[5].setNotUsed(true);
//
//		/** polysilicon-1-pin */
//		polyPinNodes[0] = PrimitiveNode.newInstance("Polysilicon-1-Pin", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoPoly1_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		polyPinNodes[0].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, polyPinNodes[0], new ArcProto[] {polyArcs[0]}, "polysilicon-1", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
//			});
//		polyPinNodes[0].setFunction(PrimitiveNode.Function.PIN);
//		polyPinNodes[0].setArcsWipe();
//		polyPinNodes[0].setArcsShrink();
//
//		/** polysilicon-2-pin */
//		polyPinNodes[1] = PrimitiveNode.newInstance("Polysilicon-2-Pin", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoPoly2_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		polyPinNodes[1].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, polyPinNodes[1], new ArcProto[] {polyArcs[1]}, "polysilicon-2", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
//			});
//		polyPinNodes[1].setFunction(PrimitiveNode.Function.PIN);
//		polyPinNodes[1].setArcsWipe();
//		polyPinNodes[1].setArcsShrink();
////		polyPinNodes[1].setNotUsed(true);
//
//		/** P-active-pin */
//		activePinNodes[P_TYPE] = PrimitiveNode.newInstance("P-Active-Pin", this, 15.0, 15.0, new SizeOffset(6, 6, 6, 6),
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoActiveLayers[P_TYPE],  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
//				new Technology.NodeLayer(pseudoWellLayers[N_TYPE],  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
//				new Technology.NodeLayer(pseudoSelectLayers[P_TYPE], 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4))
//			});
//		activePinNodes[P_TYPE].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, activePinNodes[P_TYPE], new ArcProto[] {activeArcs[P_TYPE]}, "p-active", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
//			});
//		activePinNodes[P_TYPE].setFunction(PrimitiveNode.Function.PIN);
//		activePinNodes[P_TYPE].setArcsWipe();
//		activePinNodes[P_TYPE].setArcsShrink();
//
//		/** N-active-pin */
//		activePinNodes[N_TYPE] = PrimitiveNode.newInstance("N-Active-Pin", this, 15.0, 15.0, new SizeOffset(6, 6, 6, 6),
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoActiveLayers[N_TYPE],  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
//				new Technology.NodeLayer(pseudoWellLayers[P_TYPE],  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
//				new Technology.NodeLayer(pseudoSelectLayers[N_TYPE], 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4))
//			});
//		activePinNodes[N_TYPE].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, activePinNodes[N_TYPE], new ArcProto[] {activeArcs[N_TYPE]}, "n-active", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
//			});
//		activePinNodes[N_TYPE].setFunction(PrimitiveNode.Function.PIN);
//		activePinNodes[N_TYPE].setArcsWipe();
//		activePinNodes[N_TYPE].setArcsShrink();
//
//		/** General active-pin */
//		PrimitiveNode activeGenPinNode = PrimitiveNode.newInstance("Active-Pin", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pseudoActiveLayers[P_TYPE], 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
//				new Technology.NodeLayer(pseudoActiveLayers[N_TYPE], 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		activeGenPinNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, activeGenPinNode, new ArcProto[] {active_arc, activeArcs[P_TYPE], activeArcs[N_TYPE]}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
//			});
//		activeGenPinNode.setFunction(PrimitiveNode.Function.PIN);
//		activeGenPinNode.setArcsWipe();
//		activeGenPinNode.setArcsShrink();
////        activeGenPinNode.setNotUsed(true);

		/** metal-1-P-active-contact */
		metalActiveContactNodes[P_TYPE] = PrimitiveNode.newInstance("Metal-1-P-Active-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
				new Technology.NodeLayer(activeLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(wellLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX,Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(selectLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				Technology.NodeLayer.makeMulticut(activeCutLayer, 0, Poly.Type.FILLED, TechPoint.makeIndented(8.5), 2, 2, 3, 3)
//				new Technology.NodeLayer(activeCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
			});
		metalActiveContactNodes[P_TYPE].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalActiveContactNodes[P_TYPE], new ArcProto[] {activeArcs[P_TYPE], metalArcs[0]}, "metal-1-p-act", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metalActiveContactNodes[P_TYPE].setFunction(PrimitiveNode.Function.CONTACT);
//		metalActiveContactNodes[P_TYPE].setSpecialType(PrimitiveNode.MULTICUT);
//		metalActiveContactNodes[P_TYPE].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
		metalActiveContactNodes[P_TYPE].setMinSize(17, 17, "6.2, 7.3");

		/** metal-1-N-active-contact */
		metalActiveContactNodes[N_TYPE] = PrimitiveNode.newInstance("Metal-1-N-Active-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
				new Technology.NodeLayer(activeLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(wellLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(selectLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				Technology.NodeLayer.makeMulticut(activeCutLayer, 0, Poly.Type.FILLED, TechPoint.makeIndented(8.5), 2, 2, 3, 3)
//				new Technology.NodeLayer(activeCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
			});
		metalActiveContactNodes[N_TYPE].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalActiveContactNodes[N_TYPE], new ArcProto[] {activeArcs[N_TYPE], metalArcs[0]}, "metal-1-n-act", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metalActiveContactNodes[N_TYPE].setFunction(PrimitiveNode.Function.CONTACT);
//		metalActiveContactNodes[N_TYPE].setSpecialType(PrimitiveNode.MULTICUT);
//		metalActiveContactNodes[N_TYPE].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
		metalActiveContactNodes[N_TYPE].setMinSize(17, 17, "6.2, 7.3");

		/** metal-1-polysilicon-1-contact */
		metal1PolyContactNodes[0] = PrimitiveNode.newInstance("Metal-1-Polysilicon-1-Con", this, 5.0, 5.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.5)),
				new Technology.NodeLayer(poly1Layer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				Technology.NodeLayer.makeMulticut(polyCutLayer, 0, Poly.Type.FILLED, TechPoint.makeIndented(2.5), 2, 2, 3, 3)
//				new Technology.NodeLayer(polyCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5))
			});
		metal1PolyContactNodes[0].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1PolyContactNodes[0], new ArcProto[] {polyArcs[0], metalArcs[0]}, "metal-1-polysilicon-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		metal1PolyContactNodes[0].setFunction(PrimitiveNode.Function.CONTACT);
//		metal1PolyContactNodes[0].setSpecialType(PrimitiveNode.MULTICUT);
//		metal1PolyContactNodes[0].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
		metal1PolyContactNodes[0].setMinSize(5, 5, "5.2, 7.3");

		/** metal-1-polysilicon-2-contact */
		metal1PolyContactNodes[1] = PrimitiveNode.newInstance("Metal-1-Polysilicon-2-Con", this, 10.0, 10.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(3)),
				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				Technology.NodeLayer.makeMulticut(polyCutLayer, 0, Poly.Type.FILLED, TechPoint.makeIndented(5), 2, 2, 3, 3)
//				new Technology.NodeLayer(polyCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4))
			});
		metal1PolyContactNodes[1].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1PolyContactNodes[1], new ArcProto[] {polyArcs[1], metalArcs[0]}, "metal-1-polysilicon-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(4.5), EdgeV.fromBottom(4.5), EdgeH.fromRight(4.5), EdgeV.fromTop(4.5))
			});
		metal1PolyContactNodes[1].setFunction(PrimitiveNode.Function.CONTACT);
//		metal1PolyContactNodes[1].setSpecialType(PrimitiveNode.MULTICUT);
//		metal1PolyContactNodes[1].setSpecialValues(new double [] {2, 2, 4, 4, 3, 3});
//		metal1PolyContactNodes[1].setNotUsed(true);
		metal1PolyContactNodes[1].setMinSize(10, 10, "?");

		/** metal-1-polysilicon-1-2-contact */
		metal1PolyContactNodes[2] = PrimitiveNode.newInstance("Metal-1-Polysilicon-1-2-Con", this, 15.0, 15.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(5.5)),
				new Technology.NodeLayer(poly1Layer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(5)),
				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				Technology.NodeLayer.makeMulticut(polyCutLayer, 0, Poly.Type.FILLED, TechPoint.makeIndented(7.5), 2, 2, 3, 3)
//				new Technology.NodeLayer(polyCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5))
			});
		metal1PolyContactNodes[2].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1PolyContactNodes[2], new ArcProto[] {polyArcs[0], polyArcs[1], metalArcs[0]}, "metal-1-polysilicon-1-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(7), EdgeV.fromBottom(7), EdgeH.fromRight(7), EdgeV.fromTop(7))
			});
		metal1PolyContactNodes[2].setFunction(PrimitiveNode.Function.CONTACT);
//		metal1PolyContactNodes[2].setSpecialType(PrimitiveNode.MULTICUT);
//		metal1PolyContactNodes[2].setSpecialValues(new double [] {2, 2, 6.5, 6.5, 3, 3});
//		metal1PolyContactNodes[2].setNotUsed(true);
		metal1PolyContactNodes[2].setMinSize(15, 15, "?");

		/** P-Transistor */
        /** N-Transistor */
        String[] stdNames = {"p", "n"};
        for (int i = 0; i < 2; i++)
        {
            transistorPolyLayers[i] = new Technology.NodeLayer(transistorPolyLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2);
            transistorPolyLLayers[i] = new Technology.NodeLayer(poly1Layer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(10))}, 1, 1, 0 /*Was 2 SMR*/, 2);
            transistorPolyRLayers[i] = new Technology.NodeLayer(poly1Layer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 0 /*Was 2 SMR*/);
            transistorPolyCLayers[i] = new Technology.NodeLayer(transistorPolyLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(10))}, 1, 1, 2, 2);
            transistorActiveLayers[i] = new Technology.NodeLayer(activeLayers[i], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(7)),
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(7))}, 4, 4, 0, 0);
            transistorActiveTLayers[i] = new Technology.NodeLayer(activeLayers[i], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(10)),
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(7))}, 4, 0 /*Was 4 SMR*/, 0, 0);
            transistorActiveBLayers[i] = new Technology.NodeLayer(activeLayers[i], 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(7)),
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(10))}, 0 /*Was 4 SMR*/, 4, 0, 0);
            transistorWellLayers[i] = new Technology.NodeLayer(wellLayers[(i+1)%transistorNodes.length], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(1)),
                new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1))}, 10, 10, 6, 6);
            transistorSelectLayers[i] = new Technology.NodeLayer(selectLayers[i], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(5)),
                new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(5))}, 6, 6, 2, 2);
            transistorNodes[i] = PrimitiveNode.newInstance(stdNames[i].toUpperCase()+"-Transistor", this, 15.0, 22.0, new SizeOffset(6, 6, 10, 10),
                new Technology.NodeLayer [] {transistorActiveLayers[i], transistorPolyLayers[i], transistorWellLayers[i], transistorSelectLayers[i]});
            transistorNodes[i].setElectricalLayers(new Technology.NodeLayer [] {transistorActiveTLayers[i], transistorActiveBLayers[i],
                transistorPolyCLayers[i], transistorPolyLLayers[i], transistorPolyRLayers[i], transistorWellLayers[i], transistorSelectLayers[i]});
            transistorNodes[i].addPrimitivePorts(new PrimitivePort []
                {
                    PrimitivePort.newInstance(this, transistorNodes[i], new ArcProto[] {polyArcs[0]}, stdNames[i]+"-trans-poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(4), EdgeV.fromBottom(11), EdgeH.fromLeft(4), EdgeV.fromTop(11)),
                    PrimitivePort.newInstance(this, transistorNodes[i], new ArcProto[] {activeArcs[i]}, stdNames[i]+"-trans-diff-top", 90,90, 1, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7)),
                    PrimitivePort.newInstance(this, transistorNodes[i], new ArcProto[] {polyArcs[0]}, stdNames[i]+"-trans-poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromRight(4), EdgeV.fromBottom(11), EdgeH.fromRight(4), EdgeV.fromTop(11)),
                    PrimitivePort.newInstance(this, transistorNodes[i], new ArcProto[] {activeArcs[i]}, stdNames[i]+"-trans-diff-bottom", 270,90, 2, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(7.5), EdgeV.fromBottom(7), EdgeH.fromRight(7.5), EdgeV.fromBottom(7.5))
                });
            transistorNodes[i].setFunction((i==P_TYPE) ? PrimitiveNode.Function.TRAPMOS : PrimitiveNode.Function.TRANMOS);
            transistorNodes[i].setHoldsOutline();
            transistorNodes[i].setCanShrink();
            transistorNodes[i].setSpecialType(PrimitiveNode.SERPTRANS);
            transistorNodes[i].setSpecialValues(new double [] {7, 1.5, 2.5, 2, 1, 2});
            transistorNodes[i].setMinSize(15, 22, "2.1, 3.1");
        }

		/** Thick oxide transistors */
		String[] thickNames = {"Thick-P", "Thick-N"};
		Technology.NodeLayer[] thickActiveLayers = new Technology.NodeLayer[] {transistorActiveLayers[P_TYPE], transistorActiveLayers[N_TYPE]};
		Technology.NodeLayer[] thickPolyLayers = new Technology.NodeLayer[] {transistorPolyLayers[P_TYPE], transistorPolyLayers[N_TYPE]};
		Technology.NodeLayer[] thickWellLayers = new Technology.NodeLayer[] {transistorWellLayers[P_TYPE], transistorWellLayers[N_TYPE]};
		Technology.NodeLayer[] thickSelectLayers = new Technology.NodeLayer[] {transistorSelectLayers[P_TYPE], transistorSelectLayers[N_TYPE]};
		Technology.NodeLayer[] thickActiveTLayers = new Technology.NodeLayer[] {transistorActiveTLayers[P_TYPE], transistorActiveTLayers[N_TYPE]};
		Technology.NodeLayer[] thickActiveBLayers = new Technology.NodeLayer[] {transistorActiveBLayers[P_TYPE], transistorActiveBLayers[N_TYPE]};
        Technology.NodeLayer[] thickPolyCLayers = new Technology.NodeLayer[] {transistorPolyCLayers[P_TYPE], transistorPolyCLayers[N_TYPE]};
		Technology.NodeLayer[] thickPolyLLayers = new Technology.NodeLayer[] {transistorPolyLLayers[P_TYPE], transistorPolyLLayers[N_TYPE]};
		Technology.NodeLayer[] thickPolyRLayers = new Technology.NodeLayer[] {transistorPolyRLayers[P_TYPE], transistorPolyRLayers[N_TYPE]};
		Technology.NodeLayer[] thickLayers = new Technology.NodeLayer[2];

		for (int i = 0; i < thickLayers.length; i++)
		{
			thickLayers[i] = new Technology.NodeLayer(thickActiveLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1))}, 10, 10, 6, 6);
		}

		for (int i = 0; i < thickTransistorNodes.length; i++)
		{
			thickTransistorNodes[i] = PrimitiveNode.newInstance(thickNames[i] + "-Transistor", this, 15.0, 22.0, new SizeOffset(6, 6, 10, 10),
				new Technology.NodeLayer [] {thickActiveLayers[i], thickPolyLayers[i], thickWellLayers[i], thickSelectLayers[i], thickLayers[i]});
			thickTransistorNodes[i].setElectricalLayers(new Technology.NodeLayer [] {thickActiveTLayers[i], thickActiveBLayers[i],
				thickPolyCLayers[i], thickPolyLLayers[i], thickPolyRLayers[i], thickWellLayers[i], thickSelectLayers[i], thickLayers[i]});
			thickTransistorNodes[i].addPrimitivePorts(new PrimitivePort []
				{
					PrimitivePort.newInstance(this, thickTransistorNodes[i], new ArcProto[] {polyArcs[0]}, "poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
						EdgeH.fromLeft(4), EdgeV.fromBottom(11), EdgeH.fromLeft(4), EdgeV.fromTop(11)),
					PrimitivePort.newInstance(this, thickTransistorNodes[i], new ArcProto[] {activeArcs[i]}, "diff-top", 90,90, 1, PortCharacteristic.UNKNOWN,
						EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7)),
					PrimitivePort.newInstance(this, thickTransistorNodes[i], new ArcProto[] {polyArcs[0]}, "poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
						EdgeH.fromRight(4), EdgeV.fromBottom(11), EdgeH.fromRight(4), EdgeV.fromTop(11)),
					PrimitivePort.newInstance(this, thickTransistorNodes[i], new ArcProto[] {activeArcs[i]}, "diff-bottom", 270,90, 2, PortCharacteristic.UNKNOWN,
						EdgeH.fromLeft(7.5), EdgeV.fromBottom(7), EdgeH.fromRight(7.5), EdgeV.fromBottom(7.5))
				});
			thickTransistorNodes[i].setFunction((i==P_TYPE) ? PrimitiveNode.Function.TRAPMOS : PrimitiveNode.Function.TRANMOS);
			thickTransistorNodes[i].setHoldsOutline();
			thickTransistorNodes[i].setCanShrink();
			thickTransistorNodes[i].setSpecialType(PrimitiveNode.SERPTRANS);
			thickTransistorNodes[i].setSpecialValues(new double [] {7, 1.5, 2.5, 2, 1, 2});
			thickTransistorNodes[i].setMinSize(15, 22, "2.1, 3.1");
			thickTransistorNodes[i].setSkipSizeInPalette(); // For display purposes
            thickTransistorNodes[i].setNodeBit(PrimitiveNode.OD18BIT);
		}

        if (haveMocmosExtensions) {
             scalableTransistorNodes = new PrimitiveNode[2];
            /** Scalable-P-Transistor */
            scalableTransistorNodes[P_TYPE] = PrimitiveNode.newInstance("P-Transistor-Scalable", this, 17.0, 26.0, new SizeOffset(7, 7, 12, 12),
                new Technology.NodeLayer []
                {
                    new Technology.NodeLayer(activeLayers[P_TYPE], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(6)),
                        new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(11))}),
                    new Technology.NodeLayer(metalLayers[0], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromTop(6.5)),
                        new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(10.5))}),
                    new Technology.NodeLayer(activeLayers[P_TYPE], 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(11)),
                        new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(6))}),
                    new Technology.NodeLayer(metalLayers[0], 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(10.5)),
                        new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromBottom(6.5))}),
                    new Technology.NodeLayer(activeLayers[P_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(9)),
                        new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(9))}),
                    new Technology.NodeLayer(transistorPolyLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(12)),
                        new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(12))}),
                    new Technology.NodeLayer(wellLayers[N_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
                    new Technology.NodeLayer(selectLayers[P_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
                    new Technology.NodeLayer(activeCutLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(9.5)),
                        new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromBottom(7.5))}),
                    new Technology.NodeLayer(activeCutLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5)),
                        new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromTop(9.5))})
                });
            scalableTransistorNodes[P_TYPE].addPrimitivePorts(new PrimitivePort []
                {
                    PrimitivePort.newInstance(this, scalableTransistorNodes[P_TYPE], new ArcProto[] {polyArcs[0]}, "p-trans-sca-poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromCenter(-3.5), EdgeV.makeCenter(), EdgeH.fromCenter(-3.5), EdgeV.makeCenter()),
                    PrimitivePort.newInstance(this, scalableTransistorNodes[P_TYPE], new ArcProto[] {activeArcs[P_TYPE], metalArcs[0]}, "p-trans-sca-diff-top", 90,90, 1, PortCharacteristic.UNKNOWN,
                        EdgeH.makeCenter(), EdgeV.fromCenter(4.5), EdgeH.makeCenter(), EdgeV.fromCenter(4.5)),
                    PrimitivePort.newInstance(this, scalableTransistorNodes[P_TYPE], new ArcProto[] {polyArcs[0]}, "p-trans-sca-poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromCenter(3.5), EdgeV.makeCenter(), EdgeH.fromCenter(3.5), EdgeV.makeCenter()),
                    PrimitivePort.newInstance(this, scalableTransistorNodes[P_TYPE], new ArcProto[] {activeArcs[P_TYPE], metalArcs[0]}, "p-trans-sca-diff-bottom", 270,90, 2, PortCharacteristic.UNKNOWN,
                        EdgeH.makeCenter(), EdgeV.fromCenter(-4.5), EdgeH.makeCenter(), EdgeV.fromCenter(-4.5))
                });
            scalableTransistorNodes[P_TYPE].setFunction(PrimitiveNode.Function.TRAPMOS);
            scalableTransistorNodes[P_TYPE].setCanShrink();
            scalableTransistorNodes[P_TYPE].setMinSize(17, 26, "2.1, 3.1");

            /** Scalable-N-Transistor */
            scalableTransistorNodes[N_TYPE] = PrimitiveNode.newInstance("N-Transistor-Scalable", this, 17.0, 26.0, new SizeOffset(7, 7, 12, 12),
                new Technology.NodeLayer []
                {
                    new Technology.NodeLayer(activeLayers[N_TYPE], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(6)),
                        new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(11))}),
                    new Technology.NodeLayer(metalLayers[0], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromTop(6.5)),
                        new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(10.5))}),
                    new Technology.NodeLayer(activeLayers[N_TYPE], 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(11)),
                        new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(6))}),
                    new Technology.NodeLayer(metalLayers[0], 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(10.5)),
                        new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromBottom(6.5))}),
                    new Technology.NodeLayer(activeLayers[N_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(9)),
                        new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(9))}),
                    new Technology.NodeLayer(transistorPolyLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(12)),
                        new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(12))}),
                    new Technology.NodeLayer(wellLayers[P_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
                    new Technology.NodeLayer(selectLayers[N_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
                    new Technology.NodeLayer(activeCutLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(9.5)),
                        new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromBottom(7.5))}),
                    new Technology.NodeLayer(activeCutLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5)),
                        new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromTop(9.5))})
                });
            scalableTransistorNodes[N_TYPE].addPrimitivePorts(new PrimitivePort []
                {
                    PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {polyArcs[0]}, "n-trans-sca-poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromCenter(-3.5), EdgeV.makeCenter(), EdgeH.fromCenter(-3.5), EdgeV.makeCenter()),
                    PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {activeArcs[N_TYPE], metalArcs[0]}, "n-trans-sca-diff-top", 90,90, 1, PortCharacteristic.UNKNOWN,
                        EdgeH.makeCenter(), EdgeV.fromCenter(4.5), EdgeH.makeCenter(), EdgeV.fromCenter(4.5)),
                    PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {polyArcs[0]}, "n-trans-sca-poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromCenter(3.5), EdgeV.makeCenter(), EdgeH.fromCenter(3.5), EdgeV.makeCenter()),
                    PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {activeArcs[N_TYPE], metalArcs[0]}, "n-trans-sca-diff-bottom", 270,90, 2, PortCharacteristic.UNKNOWN,
                        EdgeH.makeCenter(), EdgeV.fromCenter(-4.5), EdgeH.makeCenter(), EdgeV.fromCenter(-4.5))
                });
            scalableTransistorNodes[N_TYPE].setFunction(PrimitiveNode.Function.TRANMOS);
            scalableTransistorNodes[N_TYPE].setCanShrink();
            scalableTransistorNodes[N_TYPE].setMinSize(17, 26, "2.1, 3.1");

            npnTransistorNode = PrimitiveNode.newInstance("NPN-Transistor", this, 56.0, 48.0, new SizeOffset(13, 13, 13, 13),
                new Technology.NodeLayer []
                {
                    // Right Collector Cuts
                    Technology.NodeLayer.makeMulticut(activeCutLayer, 2, Poly.Type.FILLED, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(8)),
                        new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(8))}, 2, 2, 2, 2/*3, 3*/),
                    // Right Collector Metal
                    new Technology.NodeLayer(metalLayers[0], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromRight(9), EdgeV.fromBottom(3)),
                        new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(3))}),
                    // Right Collector P-Active
                    new Technology.NodeLayer(activeLayers[P_TYPE], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromRight(9), EdgeV.fromBottom(3)),
                        new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(3))}),
                    // Right Collector N+ Select
                    new Technology.NodeLayer(selectLayers[N_TYPE], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromRight(11), EdgeV.fromBottom(11)),
                        new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(11))}),
                    // Left Collector Cuts
                    Technology.NodeLayer.makeMulticut(activeCutLayer, 2, Poly.Type.FILLED, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(8)),
                        new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(8))}, 2, 2, 2, 2/*3, 3*/),
                    // Left Collector Metal
                    new Technology.NodeLayer(metalLayers[0], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
                        new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.fromTop(3))}),
                    // Left Collector P-Active
                    new Technology.NodeLayer(activeLayers[P_TYPE], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
                        new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.fromTop(3))}),
                    // Left Collector N+ Select
                    new Technology.NodeLayer(selectLayers[N_TYPE], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(11)),
                        new Technology.TechPoint(EdgeH.fromLeft(11), EdgeV.fromTop(11))}),
                    // Bottom Collector Left Cuts
                    Technology.NodeLayer.makeMulticut(activeCutLayer, 2, Poly.Type.FILLED, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(11), EdgeV.fromBottom(6)),
                        new Technology.TechPoint(EdgeH.fromCenter(-13), EdgeV.fromBottom(6))}, 2, 2, 2, 2/*3, 3*/),
                    // Bottom Collector Left Metal
                    new Technology.NodeLayer(metalLayers[0], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.fromBottom(3)),
                        new Technology.TechPoint(EdgeH.fromCenter(-10), EdgeV.fromBottom(9))}),
                    // Bottom Collector Right Cuts
                    Technology.NodeLayer.makeMulticut(activeCutLayer, 2, Poly.Type.FILLED, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromCenter(13), EdgeV.fromBottom(6)),
                        new Technology.TechPoint(EdgeH.fromRight(11), EdgeV.fromBottom(6))}, 2, 2, 2, 2/*3, 3*/),
                     // Bottom Collector Right Metal
                    new Technology.NodeLayer(metalLayers[0], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromCenter(10), EdgeV.fromBottom(3)),
                        new Technology.TechPoint(EdgeH.fromRight(9), EdgeV.fromBottom(9))}),
                     // Bottom Collector P-Active
                    new Technology.NodeLayer(activeLayers[P_TYPE], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.fromBottom(3)),
                        new Technology.TechPoint(EdgeH.fromRight(9), EdgeV.fromBottom(9))}),
                    // Bottom Collector N+ Select
                    new Technology.NodeLayer(selectLayers[N_TYPE], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
                        new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromBottom(11))}),
                    // Top Collector Cuts
                    Technology.NodeLayer.makeMulticut(activeCutLayer, 2, Poly.Type.FILLED, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(11), EdgeV.fromTop(6)),
                        new Technology.TechPoint(EdgeH.fromRight(11), EdgeV.fromTop(6))}, 2, 2, 2, 2/*3, 3*/),
                     // Top Collector Metal
                    new Technology.NodeLayer(metalLayers[0], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.fromTop(9)),
                        new Technology.TechPoint(EdgeH.fromRight(9), EdgeV.fromTop(3))}),
                     // Top Collector P-Active
                    new Technology.NodeLayer(activeLayers[P_TYPE], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.fromTop(9)),
                        new Technology.TechPoint(EdgeH.fromRight(9), EdgeV.fromTop(3))}),
                    // Top Collector N+ Select
                    new Technology.NodeLayer(selectLayers[N_TYPE], 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromTop(11)),
                        new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge())}),
                    // Emitter Cuts
                    Technology.NodeLayer.makeMulticut(activeCutLayer, 1, Poly.Type.FILLED, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(22), EdgeV.fromBottom(24)),
                        new Technology.TechPoint(EdgeH.fromCenter(-6), EdgeV.fromTop(24))}, 2, 2, 2, 2/*3, 3*/),
                    // Emitter Metal
                    new Technology.NodeLayer(metalLayers[0], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(19), EdgeV.fromBottom(21)),
                        new Technology.TechPoint(EdgeH.fromCenter(-3), EdgeV.fromTop(21))}),
                    // Emitter N+ Select
                    new Technology.NodeLayer(selectLayers[N_TYPE], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromLeft(19), EdgeV.fromBottom(21)),
                        new Technology.TechPoint(EdgeH.fromCenter(-3), EdgeV.fromTop(21))}),
                    // Base Cuts
                    Technology.NodeLayer.makeMulticut(activeCutLayer, 0, Poly.Type.FILLED, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromCenter(6), EdgeV.fromBottom(24)),
                        new Technology.TechPoint(EdgeH.fromRight(22), EdgeV.fromTop(24))}, 2, 2, 2, 2/*3, 3*/),
                    // Base Metal
                    new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromCenter(3), EdgeV.fromBottom(21)),
                        new Technology.TechPoint(EdgeH.fromRight(19), EdgeV.fromTop(21))}),
                    // Base P+ Select
                    new Technology.NodeLayer(selectLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                        new Technology.TechPoint(EdgeH.fromCenter(3), EdgeV.fromBottom(21)),
                        new Technology.TechPoint(EdgeH.fromRight(19), EdgeV.fromTop(21))}),
                    // P Base
                    new Technology.NodeLayer(pBaseLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(13)),
                    // N Well
                    new Technology.NodeLayer(wellLayers[N_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
                });
            npnTransistorNode.addPrimitivePorts(new PrimitivePort []
                {
                    PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {metalArcs[0]}, "base", 180,90, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromCenter(5.5), EdgeV.fromBottom(23.5), EdgeH.fromRight(21.5), EdgeV.fromTop(23.5)),
                    PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {metalArcs[0]}, "emitter", 90,90, 1, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(21.5), EdgeV.fromBottom(23.5), EdgeH.fromCenter(-5.5), EdgeV.fromTop(23.5)),
                    PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {metalArcs[0]}, "collector", 90,90, 1, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(5.5), EdgeV.fromTop(6.5), EdgeH.fromRight(5.5), EdgeV.fromTop(5.5))
                });
            npnTransistorNode.setFunction(PrimitiveNode.Function.TRANPN);
            npnTransistorNode.setCanShrink();
            npnTransistorNode.setMinSize(56, 48, "16.*");
            npnTransistorNode.setSkipSizeInPalette(); // not used to size palette.
        }

		/** metal-1-metal-2-contact */
		metalContactNodes[0] = PrimitiveNode.newInstance("Metal-1-Metal-2-Con", this, 5.0, 5.0, new SizeOffset(0.5, 0.5, 0.5, 0.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.5)),
				new Technology.NodeLayer(metalLayers[1], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.5)),
				Technology.NodeLayer.makeMulticut(viaLayers[0], 0, Poly.Type.FILLED, TechPoint.makeIndented(2.5), 2, 2, 3, 3)
//				new Technology.NodeLayer(viaLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5))
			});
		metalContactNodes[0].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalContactNodes[0], new ArcProto[] {metalArcs[0], metalArcs[1]}, "metal-1-metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metalContactNodes[0].setFunction(PrimitiveNode.Function.CONTACT);
//		metalContactNodes[0].setSpecialType(PrimitiveNode.MULTICUT);
//		metalContactNodes[0].setSpecialValues(new double [] {2, 2, 1, 1, 3, 3});
		metalContactNodes[0].setMinSize(5, 5, "8.3, 9.3");

		/** metal-2-metal-3-contact */
		metalContactNodes[1] = PrimitiveNode.newInstance("Metal-2-Metal-3-Con", this, 6.0, 6.0, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[1], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(metalLayers[2], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				Technology.NodeLayer.makeMulticut(viaLayers[1], 0, Poly.Type.FILLED, TechPoint.makeIndented(3), 2, 2, 3, 3)
//				new Technology.NodeLayer(viaLayers[1], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2))
			});
		metalContactNodes[1].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalContactNodes[1], new ArcProto[] {metalArcs[1], metalArcs[2]}, "metal-2-metal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metalContactNodes[1].setFunction(PrimitiveNode.Function.CONTACT);
//		metalContactNodes[1].setSpecialType(PrimitiveNode.MULTICUT);
//		metalContactNodes[1].setSpecialValues(new double [] {2, 2, 1, 1, 3, 3});
		metalContactNodes[1].setMinSize(6, 6, "14.3, 15.3");

		/** metal-3-metal-4-contact */
		metalContactNodes[2] = PrimitiveNode.newInstance("Metal-3-Metal-4-Con", this, 6.0, 6.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[2], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(metalLayers[3], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				Technology.NodeLayer.makeMulticut(viaLayers[2], 0, Poly.Type.FILLED, TechPoint.makeIndented(3), 2, 2, 3, 3)
//				new Technology.NodeLayer(viaLayers[2], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2))
			});
		metalContactNodes[2].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalContactNodes[2], new ArcProto[] {metalArcs[2], metalArcs[3]}, "metal-3-metal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metalContactNodes[2].setFunction(PrimitiveNode.Function.CONTACT);
//		metalContactNodes[2].setSpecialType(PrimitiveNode.MULTICUT);
//		metalContactNodes[2].setSpecialValues(new double [] {2, 2, 2, 2, 3, 3});
		metalContactNodes[2].setMinSize(6, 6, "21.3, 22.3");

		/** metal-4-metal-5-contact */
		metalContactNodes[3] = PrimitiveNode.newInstance("Metal-4-Metal-5-Con", this, 7.0, 7.0, new SizeOffset(1.5, 1.5, 1.5, 1.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[3], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5)),
				new Technology.NodeLayer(metalLayers[4], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5)),
				Technology.NodeLayer.makeMulticut(viaLayers[3], 0, Poly.Type.FILLED, TechPoint.makeIndented(3.5), 2, 2, 3, 3)
//				new Technology.NodeLayer(viaLayers[3], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.5))
			});
		metalContactNodes[3].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalContactNodes[3], new ArcProto[] {metalArcs[3], metalArcs[4]}, "metal-4-metal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metalContactNodes[3].setFunction(PrimitiveNode.Function.CONTACT);
//		metalContactNodes[3].setSpecialType(PrimitiveNode.MULTICUT);
//		metalContactNodes[3].setSpecialValues(new double [] {2, 2, 1, 1, 3, 3});
//		metalContactNodes[3].setNotUsed(true);
		metalContactNodes[3].setMinSize(7, 7, "25.3, 26.3");

		/** metal-5-metal-6-contact */
		metalContactNodes[4] = PrimitiveNode.newInstance("Metal-5-Metal-6-Con", this, 8.0, 8.0, new SizeOffset(1.5, 1.5, 1.5, 1.5),
//		metalContactNodes[4] = PrimitiveNode.newInstance("Metal-5-Metal-6-Con", this, 8.0, 8.0, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[4], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5)),
				new Technology.NodeLayer(metalLayers[5], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5)),
				Technology.NodeLayer.makeMulticut(viaLayers[4], 0, Poly.Type.FILLED, TechPoint.makeIndented(4), 3, 3, 4, 4)
//				new Technology.NodeLayer(metalLayers[4], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
//				new Technology.NodeLayer(metalLayers[5], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
//				new Technology.NodeLayer(viaLayers[4], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(3))
			});
		metalContactNodes[4].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalContactNodes[4], new ArcProto[] {metalArcs[4], metalArcs[5]}, "metal-5-metal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metalContactNodes[4].setFunction(PrimitiveNode.Function.CONTACT);
//		metalContactNodes[4].setSpecialType(PrimitiveNode.MULTICUT);
//		metalContactNodes[4].setSpecialValues(new double [] {3, 3, 2, 2, 4, 4});
//		metalContactNodes[4].setNotUsed(true);
		metalContactNodes[4].setMinSize(8, 8, "29.3, 30.3");

        /**************************************************************************
         * Metal-1-P-Well Contact/Metal-1-N-Well Contact
        **************************************************************************/
        for (int i = 0; i < metalWellContactNodes.length; i++)
        {
            PrimitiveNode.Function func = (i==P_TYPE) ? PrimitiveNode.Function.WELL : PrimitiveNode.Function.SUBSTRATE;
            Layer active = (i==P_TYPE) ? pActiveWellLayer : activeLayers[N_TYPE];
            metalWellContactNodes[i] = PrimitiveNode.newInstance(metalLayers[0].getName()+"-"+wellLayers[i].getName()+"-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
                new Technology.NodeLayer []
                {
                    new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
                    new Technology.NodeLayer(active, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
                    new Technology.NodeLayer(wellLayers[i], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
                    new Technology.NodeLayer(selectLayers[i], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
                    Technology.NodeLayer.makeMulticut(activeCutLayer, 0, Poly.Type.FILLED, TechPoint.makeIndented(8.5), 2, 2, 3, 3)
//                    new Technology.NodeLayer(activeCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
                });
            metalWellContactNodes[i].addPrimitivePorts(new PrimitivePort []
                {
                    PrimitivePort.newInstance(this, metalWellContactNodes[i], new ArcProto[] {metalArcs[0], active_arc},
                            ((i==P_TYPE)?"metal-1-well":"metal-1-substrate"),
                            0,180, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
                });
            metalWellContactNodes[i].setFunction(func);
//            metalWellContactNodes[i].setSpecialType(PrimitiveNode.MULTICUT);
//            metalWellContactNodes[i].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
            metalWellContactNodes[i].setMinSize(17, 17, "4.2, 6.2, 7.3");
        }

        createPureLayerNodes();

		oldNodeNames.put("Metal-1-Substrate-Con", metalWellContactNodes[N_TYPE]);
		oldNodeNames.put("Metal-1-Well-Con", metalWellContactNodes[P_TYPE]);
    }

    private void createPureLayerNodes() {
//        /**************************************************************************
//         * Metal Nodes
//        **************************************************************************/
//		/** Metal-1-Node */
//		metalNodes[0] = PrimitiveNode.newInstance("Metal-1-Node", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalNodes[0].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalNodes[0], new ArcProto[] {metalArcs[0]}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		metalNodes[0].setFunction(PrimitiveNode.Function.NODE);
//		metalNodes[0].setHoldsOutline();
//		metalNodes[0].setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Metal-2-Node */
//		metalNodes[1] = PrimitiveNode.newInstance("Metal-2-Node", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(metalLayers[1], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalNodes[1].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalNodes[1], new ArcProto[] {metalArcs[1]}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		metalNodes[1].setFunction(PrimitiveNode.Function.NODE);
//		metalNodes[1].setHoldsOutline();
//		metalNodes[1].setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Metal-3-Node */
//		metalNodes[2] = PrimitiveNode.newInstance("Metal-3-Node", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(metalLayers[2], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalNodes[2].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalNodes[2], new ArcProto[] {metalArcs[2]}, "metal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		metalNodes[2].setFunction(PrimitiveNode.Function.NODE);
//		metalNodes[2].setHoldsOutline();
//		metalNodes[2].setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Metal-4-Node */
//		metalNodes[3] = PrimitiveNode.newInstance("Metal-4-Node", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(metalLayers[3], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalNodes[3].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalNodes[3], new ArcProto[] {metalArcs[3]}, "metal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		metalNodes[3].setFunction(PrimitiveNode.Function.NODE);
//		metalNodes[3].setHoldsOutline();
//		metalNodes[3].setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Metal-5-Node */
//		metalNodes[4] = PrimitiveNode.newInstance("Metal-5-Node", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(metalLayers[4], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalNodes[4].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalNodes[4], new ArcProto[] {metalArcs[4]}, "metal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		metalNodes[4].setFunction(PrimitiveNode.Function.NODE);
//		metalNodes[4].setHoldsOutline();
//		metalNodes[4].setSpecialType(PrimitiveNode.POLYGONAL);
////		metalNodes[4].setNotUsed(true);
//
//		/** Metal-6-Node */
//		metalNodes[5] = PrimitiveNode.newInstance("Metal-6-Node", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(metalLayers[5], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		metalNodes[5].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, metalNodes[5], new ArcProto[] {metalArcs[5]}, "metal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		metalNodes[5].setFunction(PrimitiveNode.Function.NODE);
//		metalNodes[5].setHoldsOutline();
//		metalNodes[5].setSpecialType(PrimitiveNode.POLYGONAL);
////		metalNodes[5].setNotUsed(true);
//
//		/** Polysilicon-1-Node */
//		polyNodes[0] = PrimitiveNode.newInstance("Polysilicon-1-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(poly1Layer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		polyNodes[0].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, polyNodes[0], new ArcProto[] {polyArcs[0]}, "polysilicon-1", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		polyNodes[0].setFunction(PrimitiveNode.Function.NODE);
//		polyNodes[0].setHoldsOutline();
//		polyNodes[0].setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Polysilicon-2-Node */
//		polyNodes[1] = PrimitiveNode.newInstance("Polysilicon-2-Node", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		polyNodes[1].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, polyNodes[1], new ArcProto[] {polyArcs[1]}, "polysilicon-2", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		polyNodes[1].setFunction(PrimitiveNode.Function.NODE);
//		polyNodes[1].setHoldsOutline();
//		polyNodes[1].setSpecialType(PrimitiveNode.POLYGONAL);
////		polyNodes[1].setNotUsed(true);
//
//        PrimitiveNode[] activeNodes = new PrimitiveNode[2];
//
//		/** P-Active-Node/N-Active-Node */
//        for (int i = 0; i < activeNodes.length; i++)
//        {
//            activeNodes[i] = PrimitiveNode.newInstance(activeLayers[i].getName()+"-Node", this, 3.0, 3.0, null,
//                new Technology.NodeLayer []
//                {
//                    new Technology.NodeLayer(activeLayers[i], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//                });
//            activeNodes[i].addPrimitivePorts(new PrimitivePort []
//                {
//                    PrimitivePort.newInstance(this, activeNodes[i], new ArcProto[] {active_arc, activeArcs[0], activeArcs[1]}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
//                        EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//                });
//            activeNodes[i].setFunction(PrimitiveNode.Function.NODE);
//            activeNodes[i].setHoldsOutline();
//            activeNodes[i].setSpecialType(PrimitiveNode.POLYGONAL);
//        }
//
//        PrimitiveNode[] selectNodes = new PrimitiveNode[2];
//
//		/** P-Select-Node/N-Select-Node */
//        for (int i = 0; i < selectNodes.length; i++)
//        {
//            selectNodes[i] = PrimitiveNode.newInstance(selectLayers[i].getName()+"-Node", this, 4.0, 4.0, null,
//                new Technology.NodeLayer []
//                {
//                    new Technology.NodeLayer(selectLayers[i], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//                });
//            selectNodes[i].addPrimitivePorts(new PrimitivePort []
//                {
//                    PrimitivePort.newInstance(this, selectNodes[i], new ArcProto[0], "select", 0,180, 0, PortCharacteristic.UNKNOWN,
//                        EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//                });
//            selectNodes[i].setFunction(PrimitiveNode.Function.NODE);
//            selectNodes[i].setHoldsOutline();
//            selectNodes[i].setSpecialType(PrimitiveNode.POLYGONAL);
//        }
//
//        PrimitiveNode[] wellNodes = new PrimitiveNode[2];
//
//		/** P-Well-Node */
//		wellNodes[P_TYPE] = PrimitiveNode.newInstance("P-Well-Node", this, 12.0, 12.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(wellLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		wellNodes[P_TYPE].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, wellNodes[P_TYPE], new ArcProto[] {activeArcs[P_TYPE]}, "well", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		wellNodes[P_TYPE].setFunction(PrimitiveNode.Function.NODE);
//		wellNodes[P_TYPE].setHoldsOutline();
//		wellNodes[P_TYPE].setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** N-Well-Node */
//		wellNodes[N_TYPE] = PrimitiveNode.newInstance("N-Well-Node", this, 12.0, 12.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(wellLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		wellNodes[N_TYPE].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, wellNodes[N_TYPE], new ArcProto[] {activeArcs[P_TYPE]}, "well", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		wellNodes[N_TYPE].setFunction(PrimitiveNode.Function.NODE);
//		wellNodes[N_TYPE].setHoldsOutline();
//		wellNodes[N_TYPE].setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** PolyCut-Node */
//		PrimitiveNode polyCutNode = PrimitiveNode.newInstance("Poly-Cut-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(polyCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		polyCutNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, polyCutNode, new ArcProto[0], "polycut", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		polyCutNode.setFunction(PrimitiveNode.Function.NODE);
//		polyCutNode.setHoldsOutline();
//		polyCutNode.setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** ActiveCut-Node */
//		PrimitiveNode activeCutNode = PrimitiveNode.newInstance("Active-Cut-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(activeCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		activeCutNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, activeCutNode, new ArcProto[0], "activecut", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		activeCutNode.setFunction(PrimitiveNode.Function.NODE);
//		activeCutNode.setHoldsOutline();
//		activeCutNode.setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Via-1-Node */
//		viaNodes[0] = PrimitiveNode.newInstance("Via-1-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(viaLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		viaNodes[0].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, viaNodes[0], new ArcProto[0], "via-1", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		viaNodes[0].setFunction(PrimitiveNode.Function.NODE);
//		viaNodes[0].setHoldsOutline();
//		viaNodes[0].setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Via-2-Node */
//		viaNodes[1] = PrimitiveNode.newInstance("Via-2-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(viaLayers[1], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		viaNodes[1].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, viaNodes[1], new ArcProto[0], "via-2", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		viaNodes[1].setFunction(PrimitiveNode.Function.NODE);
//		viaNodes[1].setHoldsOutline();
//		viaNodes[1].setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Via-3-Node */
//		viaNodes[2] = PrimitiveNode.newInstance("Via-3-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(viaLayers[2], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		viaNodes[2].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, viaNodes[2], new ArcProto[0], "via-3", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		viaNodes[2].setFunction(PrimitiveNode.Function.NODE);
//		viaNodes[2].setHoldsOutline();
//		viaNodes[2].setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Via-4-Node */
//		viaNodes[3] = PrimitiveNode.newInstance("Via-4-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(viaLayers[3], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		viaNodes[3].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, viaNodes[3], new ArcProto[0], "via-4", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		viaNodes[3].setFunction(PrimitiveNode.Function.NODE);
//		viaNodes[3].setHoldsOutline();
//		viaNodes[3].setSpecialType(PrimitiveNode.POLYGONAL);
////		viaNodes[3].setNotUsed(true);
//
//		/** Via-5-Node */
//		viaNodes[4] = PrimitiveNode.newInstance("Via-5-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(viaLayers[4], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		viaNodes[4].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, viaNodes[4], new ArcProto[0], "via-5", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		viaNodes[4].setFunction(PrimitiveNode.Function.NODE);
//		viaNodes[4].setHoldsOutline();
//		viaNodes[4].setSpecialType(PrimitiveNode.POLYGONAL);
////		viaNodes[4].setNotUsed(true);
//
//		/** Passivation-Node */
//		PrimitiveNode passivationNode = PrimitiveNode.newInstance("Passivation-Node", this, 8.0, 8.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(passivationLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		passivationNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, passivationNode, new ArcProto[0], "passivation", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		passivationNode.setFunction(PrimitiveNode.Function.NODE);
//		passivationNode.setHoldsOutline();
//		passivationNode.setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Polysilicon-1-Transistor-Node */
//		PrimitiveNode polyTransistorNode = PrimitiveNode.newInstance("Transistor-Poly-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(transistorPolyLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		polyTransistorNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, polyTransistorNode, new ArcProto[] {polyArcs[0]}, "trans-poly-1", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		polyTransistorNode.setFunction(PrimitiveNode.Function.NODE);
//		polyTransistorNode.setHoldsOutline();
//		polyTransistorNode.setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Poly-Cap-Node */
//		PrimitiveNode polyCapNode = PrimitiveNode.newInstance("Poly-Cap-Node", this, 8.0, 8.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(polyCapLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		polyCapNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, polyCapNode, new ArcProto[0], "poly-cap", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		polyCapNode.setFunction(PrimitiveNode.Function.NODE);
//		polyCapNode.setHoldsOutline();
//		polyCapNode.setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** P-Active-Well-Node */
//		PrimitiveNode pActiveWellNode = PrimitiveNode.newInstance("P-Active-Well-Node", this, 8.0, 8.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(pActiveWellLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		pActiveWellNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, pActiveWellNode, new ArcProto[0], "p-active-well", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		pActiveWellNode.setFunction(PrimitiveNode.Function.NODE);
//		pActiveWellNode.setHoldsOutline();
//		pActiveWellNode.setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Silicide-Block-Node */
//		PrimitiveNode silicideBlockNode = PrimitiveNode.newInstance("Silicide-Block-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(silicideBlockLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		silicideBlockNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, silicideBlockNode, new ArcProto[] {polyArcs[0]}, "silicide-block", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		silicideBlockNode.setFunction(PrimitiveNode.Function.NODE);
//		silicideBlockNode.setHoldsOutline();
//		silicideBlockNode.setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Thick-Active-Node */
//		PrimitiveNode thickActiveNode = PrimitiveNode.newInstance("Thick-Active-Node", this, 4.0, 4.0, null, // 4.0 is given by rule 24.1
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(thickActiveLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		thickActiveNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, thickActiveNode, new ArcProto[] {polyArcs[0]}, "thick-active", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		thickActiveNode.setFunction(PrimitiveNode.Function.NODE);
//		thickActiveNode.setHoldsOutline();
//		thickActiveNode.setSpecialType(PrimitiveNode.POLYGONAL);
//
//		/** Pad-Frame-Node */
//		PrimitiveNode padFrameNode = PrimitiveNode.newInstance("Pad-Frame-Node", this, 8.0, 8.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(padFrameLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		padFrameNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, padFrameNode, new ArcProto[0], "pad-frame", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		padFrameNode.setFunction(PrimitiveNode.Function.NODE);
//		padFrameNode.setHoldsOutline();
//		padFrameNode.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
        for(int i = 0; i < metalLayers.length; i++)
    		metalLayers[i].makePureLayerNode(metalLayers[i].getName() + "-Node", 3.0, Poly.Type.FILLED, "metal-"+(i+1), metalArcs[i]);
		poly1Layer.makePureLayerNode("Polysilicon-1-Node", 2.0, Poly.Type.FILLED, "polysilicon-1", polyArcs[0]);
		poly2_lay.makePureLayerNode("Polysilicon-2-Node", 3.0, Poly.Type.FILLED, "polysilicon-2", polyArcs[1]);
        for (int i = 0; i < activeLayers.length; i++)
            activeLayers[i].makePureLayerNode(activeLayers[i].getName()+"-Node", 3.0, Poly.Type.FILLED, "active", active_arc, activeArcs[0], activeArcs[1]);
        for (int i = 0; i < selectLayers.length; i++)
            selectLayers[i].makePureLayerNode(selectLayers[i].getName()+"-Node", 4.0, Poly.Type.FILLED, "select");
		for (int i = 0; i < wellLayers.length; i++)
            wellLayers[i].makePureLayerNode(wellLayers[i].getName()+"-Node", 12.0, Poly.Type.FILLED, "well", activeArcs[P_TYPE]);
		polyCutLayer.makePureLayerNode("Poly-Cut-Node", 2.0, Poly.Type.FILLED, "polycut");
		activeCutLayer.makePureLayerNode("Active-Cut-Node", 2.0, Poly.Type.FILLED, "activecut");
        for (int i = 0; i < viaLayers.length; i++)
            viaLayers[i].makePureLayerNode("Via-"+(i+1)+"-Node", 2.0, Poly.Type.FILLED, "via-"+(i+1));
        passivationLayer.makePureLayerNode("Passivation-Node", 8.0, Poly.Type.FILLED, "passivation");
		transistorPolyLayer.makePureLayerNode("Transistor-Poly-Node", 2.0, Poly.Type.FILLED, "trans-poly-1", polyArcs[0]);
		polyCapLayer.makePureLayerNode("Poly-Cap-Node", 8.0, Poly.Type.FILLED, "poly-cap");
		pActiveWellLayer.makePureLayerNode("P-Active-Well-Node", 8.0, Poly.Type.FILLED, "p-active-well");
		silicideBlockLayer.makePureLayerNode("Silicide-Block-Node", 2.0, Poly.Type.FILLED, "silicide-block", polyArcs[0]);
		thickActiveLayer.makePureLayerNode("Thick-Active-Node", 4.0, Poly.Type.FILLED, "thick-active"/*, polyArcs[0]*/); // 4.0 is given by rule 24.1
		padFrameLayer.makePureLayerNode("Pad-Frame-Node", 8.0, Poly.Type.FILLED, "pad-frame");
        if (pBaseLayer != null)
            pBaseLayer.makePureLayerNode("P-Base-Node", 22.0, Poly.Type.FILLED, "p-base");

//        for(int i = 0; i < metalLayers.length; i++)
//		    metalLayers[i].setPureLayerNode(metalNodes[i]);					// Metal-1->6
//		poly1Layer.setPureLayerNode(polyNodes[0]);						// Polysilicon-1
//		poly2_lay.setPureLayerNode(polyNodes[1]);						// Polysilicon-2
//        for (int i = 0; i < activeLayers.length; i++)
//		    activeLayers[i].setPureLayerNode(activeNodes[i]);					// P-Active/N-Active
//        for (int i = 0; i < selectLayers.length; i++)
//		    selectLayers[i].setPureLayerNode(selectNodes[i]);					// P-Select/N-Select
//        for (int i = 0; i < wellLayers.length; i++)
//		    wellLayers[i].setPureLayerNode(wellNodes[i]);						// P-Well/N-Well
//		polyCutLayer.setPureLayerNode(polyCutNode);					// Poly-Cut
//		activeCutLayer.setPureLayerNode(activeCutNode);				// Active-Cut
//		viaLayers[0].setPureLayerNode(viaNodes[0]);						// Via-1
//		viaLayers[1].setPureLayerNode(viaNodes[1]);						// Via-2
//		viaLayers[2].setPureLayerNode(viaNodes[2]);						// Via-3
//		viaLayers[3].setPureLayerNode(viaNodes[3]);						// Via-4
//		viaLayers[4].setPureLayerNode(viaNodes[4]);						// Via-5
//		passivationLayer.setPureLayerNode(passivationNode);			// Passivation
//		transistorPolyLayer.setPureLayerNode(polyTransistorNode);	// Transistor-Poly
//		polyCapLayer.setPureLayerNode(polyCapNode);					// Poly-Cap
//		pActiveWellLayer.setPureLayerNode(pActiveWellNode);			// P-Active-Well
//		silicideBlockLayer.setPureLayerNode(silicideBlockNode);		// Silicide-Block
//		thickActiveLayer.setPureLayerNode(thickActiveNode);			// Thick-Active
//		padFrameLayer.setPureLayerNode(padFrameNode);				// Pad-Frame
    }

    private void createExtraLayers() {
		pBaseLayer = Layer.newInstance(this, "P-Base",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 107,226, 96,/*240,221,181,*/ 0.8,true,
			new int[] { 0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x0888,   //     X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x0888}));//     X   X   X
    }

    private void initFoundryMOSIS() {
        newFoundry(Foundry.Type.MOSIS, MoCMOS.class.getResource("utils/Mosis180DRCDeck.xml"),
                // The GDS names for MOSIS
                "Metal-1 49, 80p, 80t",
                "Metal-2 51, 82p, 82t",
                "Metal-3 62, 93p, 93t",
                "Metal-4 31, 63p, 63t",
                "Metal-5 33, 64p, 64t",
                "Metal-6 37, 68p, 68t",
                "Polysilicon-1 46, 77p, 77t",
                "Transistor-Poly 46",
                "Polysilicon-2 56",
                "P-Active 43",
                "N-Active 43",
                "P-Active-Well 43",
                "P-Select 44",
                "N-Select 45",
                "P-Well 41",
                "N-Well 42",
                "Poly-Cut 25",
                "Active-Cut 25",
                "Via1 50",
                "Via2 61",
                "Via3 30",
                "Via4 32",
                "Via5 36",

                "Passivation 52",
                "Poly-Cap 28",
                "Silicide-Block 29",
                "Thick-Active 60",
                "Pad-Frame 26");
//            mosis.setFactoryGDSLayer(metalLayers[0], "49, 80p, 80t"); // Metal-1
//            mosis.setFactoryGDSLayer(metalLayers[1], "51, 82p, 82t"); // Metal-2
//            mosis.setFactoryGDSLayer(metalLayers[2], "62, 93p, 93t"); // Metal-3
//            mosis.setFactoryGDSLayer(metalLayers[3], "31, 63p, 63t"); // Metal-4
//            mosis.setFactoryGDSLayer(metalLayers[4], "33, 64p, 64t"); // Metal-5
//            mosis.setFactoryGDSLayer(metalLayers[5], "37, 68p, 68t"); // Metal-6
//            mosis.setFactoryGDSLayer(poly1Layer, "46, 77p, 77t"); // Polysilicon-1
//            mosis.setFactoryGDSLayer(transistorPolyLayer, "46"); // Transistor-Poly
//            mosis.setFactoryGDSLayer(poly2_lay, "56"); // Polysilicon-2
//            mosis.setFactoryGDSLayer(activeLayers[P_TYPE], "43"); // P-Active
//            mosis.setFactoryGDSLayer(activeLayers[N_TYPE], "43"); // N-Active
//            mosis.setFactoryGDSLayer(pActiveWellLayer, "43"); // P-Active-Well
//            mosis.setFactoryGDSLayer(selectLayers[P_TYPE], "44"); // P-Select
//            mosis.setFactoryGDSLayer(selectLayers[N_TYPE], "45"); // N-Select
//            mosis.setFactoryGDSLayer(wellLayers[P_TYPE], "41"); // P-Well
//            mosis.setFactoryGDSLayer(wellLayers[N_TYPE], "42"); // N-Well
//            mosis.setFactoryGDSLayer(polyCutLayer, "25"); // Poly-Cut
//            mosis.setFactoryGDSLayer(activeCutLayer, "25"); // Active-Cut
//            mosis.setFactoryGDSLayer(viaLayers[0], "50"); // Via-1
//            mosis.setFactoryGDSLayer(viaLayers[1], "61"); // Via-2
//            mosis.setFactoryGDSLayer(viaLayers[2], "30"); // Via-3
//            mosis.setFactoryGDSLayer(viaLayers[3], "32"); // Via-4
//            mosis.setFactoryGDSLayer(viaLayers[4], "36"); // Via-5
//
//            mosis.setFactoryGDSLayer(passivationLayer, "52"); // Passivation
//            mosis.setFactoryGDSLayer(polyCapLayer, "28"); // Poly-Cap
//            mosis.setFactoryGDSLayer(silicideBlockLayer, "29"); // Silicide-Block
//            mosis.setFactoryGDSLayer(thickActiveLayer, "60"); // Thick-Active
//            mosis.setFactoryGDSLayer(padFrameLayer, "26"); // Pad-Frame
    }

    /**
     * Method to load primitive nodes in the palette after rules have been loaded
     */
    private void buildTechPalette()
    {
        // Information for palette
        int maxY = metalArcs.length + activeArcs.length + 2 /* poly*/ + 1 /* trans */ + 1 /*misc*/ + 1 /* well */;
        nodeGroups = new Object[maxY][3];
        int count = 0;
        List<NodeInst> tmp;
        String[] stdNames = {"p", "n"};

        // Transistor nodes first
        nodeGroups[count][0] = npnTransistorNode;
        for (int i = 0; i < transistorNodes.length; i++)
        {
            tmp = new ArrayList<NodeInst>(2);
            String tmpVar = stdNames[i]+"Mos";
            tmp.add(makeNodeInst(transistorNodes[i], transistorNodes[i].getFunction(), 0, true, tmpVar, 9));
            tmp.add(makeNodeInst(thickTransistorNodes[i], thickTransistorNodes[i].getFunction(), 0, true, tmpVar, 9));
            if (scalableTransistorNodes != null)
                tmp.add(makeNodeInst(scalableTransistorNodes[i], scalableTransistorNodes[i].getFunction(), 0, true, tmpVar, 9));
            nodeGroups[count][i+1] = tmp;
        }

        // Well second
        count++;
        for (int i = 0; i < metalWellContactNodes.length; i++)
        {
            String tmpVar = stdNames[i]+"Well";
            nodeGroups[count][i+1] = makeNodeInst(metalWellContactNodes[i], metalWellContactNodes[i].getFunction(),
                    0, true, tmpVar, 5.5);
        }

        // Active/Well first
        for (int i = 0; i < activeArcs.length; i++)
        {
            nodeGroups[++count][0] = activeArcs[i];
            nodeGroups[count][1] = activePinNodes[i];
            String tmpVar = stdNames[i]+"Act";
            nodeGroups[count][2] = makeNodeInst(metalActiveContactNodes[i], metalActiveContactNodes[i].getFunction(),
                    0, true, tmpVar, 5.55);
        }

        // Poly-related node insts
        nodeGroups[++count][0] = polyArcs[0];
        nodeGroups[count][1] = polyPinNodes[0];
        nodeGroups[count][2] = metal1PolyContactNodes[0];

        nodeGroups[++count][0] = polyArcs[1];
        nodeGroups[count][1] = polyPinNodes[1];
        tmp = new ArrayList<NodeInst>();
        tmp.add(makeNodeInst(metal1PolyContactNodes[1], metal1PolyContactNodes[1].getFunction(), 0, true, null, 5.5));
        tmp.add(makeNodeInst(metal1PolyContactNodes[2], metal1PolyContactNodes[2].getFunction(), 0, true, null, 5.5));
        nodeGroups[count][2] = tmp;

        // MXMY contacts
        for (int i = 0; i < metalArcs.length; i++)
        {
            nodeGroups[++count][0] = metalArcs[i];
            nodeGroups[count][1] = metalPinNodes[i];
            nodeGroups[count][2] = (i < metalArcs.length - 1) ? metalContactNodes[i] : null;
        }

        // On the side
        nodeGroups[++count][0] = "Pure";
        nodeGroups[count][1] = "Misc.";
        nodeGroups[count][2] = "Cell";
	}

	/******************** SUPPORT METHODS ********************/

    /**
	 * Method to set the technology to state "newstate", which encodes the number of metal
	 * layers, whether it is a deep process, and other rules.
     */
    @Override
	public void setState()
	{
        EDatabase.theDatabase.checkChanging();
        setNotUsed(isSecondPolysilicon() ? 2 : 1);
        findNodeProto("Active-Pin").setNotUsed(true);
		findArcProto("Active").setNotUsed(true);
        if (npnTransistorNode != null)
            npnTransistorNode.setNotUsed(!isAnalog());
//        if (pBaseLayer != null)
//            pBaseLayer.getPureLayerNode().setNotUsed(!isAnalog());

		// set rules
        cachedRules = getFactoryDesignRules();

        // now rewrite the description
		setTechDesc(describeState());
	}

	/**
	 * Method to describe the technology when it is in state "state".
	 */
	private String describeState()
	{
		int numMetals = getNumMetals();
		String rules = "";
		switch (getRuleSet())
		{
			case SCMOSRULES: rules = "now standard";    break;
			case DEEPRULES:  rules = "now deep";        break;
			case SUBMRULES:  rules = "now submicron";   break;
		}
		int numPolys = 1;
		if (isSecondPolysilicon()) numPolys = 2;
		String description = "MOSIS CMOS (2-6 metals [now " + numMetals + "], 1-2 polys [now " +
			numPolys + "], flex rules [" + rules + "]";
		if (isDisallowStackedVias()) description += ", stacked vias disallowed";
		if (isAlternateActivePolyRules()) description += ", alternate contact rules";
		return description + ")";
	}

    @Override
    protected void dumpExtraProjectSettings(PrintWriter out) {
        printlnSetting(out, getRuleSetSetting());
        printlnSetting(out, getSecondPolysiliconSetting());
        printlnSetting(out, getDisallowStackedViasSetting());
        printlnSetting(out, getAlternateActivePolyRulesSetting());
        printlnSetting(out, getAnalogSetting());
    }

	/******************** SCALABLE TRANSISTOR DESCRIPTION ********************/

	private static final int SCALABLE_ACTIVE_TOP = 0;
	private static final int SCALABLE_METAL_TOP  = 1;
	private static final int SCALABLE_ACTIVE_BOT = 2;
	private static final int SCALABLE_METAL_BOT  = 3;
	private static final int SCALABLE_ACTIVE_CTR = 4;
	private static final int SCALABLE_POLY       = 5;
	private static final int SCALABLE_WELL       = 6;
	private static final int SCALABLE_SUBSTRATE  = 7;
	private static final int SCALABLE_TOTAL      = 8;

	/**
	 * Method to return a list of Polys that describe a given NodeInst.
	 * This method overrides the general one in the Technology object
	 * because of the unusual primitives in this Technology.
	 * @param ni the NodeInst to describe.
	 * @param electrical true to get the "electrical" layers.
	 * This makes no sense for Schematics primitives.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * This makes no sense for Schematics primitives.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * @return an array of Poly objects.
	 */
    @Override
	protected Poly [] getShapeOfNode(NodeInst ni, boolean electrical, boolean reasonable, Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		NodeProto prototype = ni.getProto();
		if (scalableTransistorNodes != null && (prototype == scalableTransistorNodes[P_TYPE] || prototype == scalableTransistorNodes[N_TYPE]))
            return getShapeOfNodeScalable(ni, null, reasonable);

        // Default
        return super.getShapeOfNode(ni, electrical, reasonable, primLayers, layerOverride);
    }

    /**
     * Special getShapeOfNode function for scalable transistors
     * @param ni
     * @param context
     * @param reasonable
     * @return Array of Poly containing layers representing a Scalable Transistor
     */
    private Poly [] getShapeOfNodeScalable(NodeInst ni, VarContext context, boolean reasonable)
    {
		// determine special configurations (number of active contacts, inset of active contacts)
		int numContacts = 2;
		boolean insetContacts = false;
		Variable var = ni.getVar(TRANS_CONTACT, String.class);
		if (var != null)
		{
			String pt = (String)var.getObject();
			for(int i=0; i<pt.length(); i++)
			{
				char chr = pt.charAt(i);
				if (chr == '0' || chr == '1' || chr == '2')
				{
					numContacts = chr - '0';
				} else if (chr == 'i' || chr == 'I') insetContacts = true;
			}
		}
		int boxOffset = 4 - numContacts * 2;

		// determine width
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		double nodeWid = ni.getXSize();
		double activeWid = nodeWid - 14;
		int extraInset = 0;
		var = ni.getVar(Schematics.ATTR_WIDTH);
		if (var != null)
		{
			VarContext evalContext = context;
			if (evalContext == null) evalContext = VarContext.globalContext;
			String extra = var.describe(evalContext, ni);
			Object o = evalContext.evalVar(var, ni);
			if (o != null) extra = o.toString();
			double requestedWid = TextUtils.atof(extra);
			if (requestedWid > activeWid)
			{
				System.out.println("Warning: " + ni.getParent() + ", " +
					ni + " requests width of " + requestedWid + " but is only " + activeWid + " wide");
			}
			if (requestedWid < activeWid && requestedWid > 0)
			{
				extraInset = (int)((activeWid - requestedWid) / 2);
				activeWid = requestedWid;
			}
		}
		double actInset = (nodeWid-activeWid) / 2;
        double gateOverhang = getTransistorExtension(np, true, cachedRules);
        double polyInset = actInset - gateOverhang; // cachedRules.getPolyOverhang();
		double actContInset = 7 + extraInset;

		// contacts must be 5 wide at a minimum
		if (activeWid < 5) actContInset -= (5-activeWid)/2;
		double metContInset = actContInset + 0.5;

		// determine the multicut information
        NodeLayer activeMulticut = metalActiveContactNodes[P_TYPE].findMulticut();
        NodeLayer activeSurround = metalActiveContactNodes[P_TYPE].getLayers()[1];
        assert activeSurround.getLayer().getFunction().isDiff();
        double cutSize = activeMulticut.getMulticutSizeX();
        assert cutSize == activeMulticut.getMulticutSizeY();
        double cutIndent = activeMulticut.getLeftEdge().getAdder() - activeSurround.getLeftEdge().getAdder();
        double cutSep = activeMulticut.getMulticutSep1D();
        assert cutSep == activeMulticut.getMulticutSep2D();
//		double [] specialValues = metalActiveContactNodes[P_TYPE].getSpecialValues();
//		double cutSize = specialValues[0];
//		double cutIndent = specialValues[2];   // or specialValues[3]
//		double cutSep = specialValues[4];      // or specialValues[5]
		int numCuts = (int)((activeWid-cutIndent*2+cutSep) / (cutSize+cutSep));
		if (numCuts <= 0) numCuts = 1;
		double cutBase = 0;
		if (numCuts != 1)
			cutBase = (activeWid-cutIndent*2 - cutSize*numCuts -
				cutSep*(numCuts-1)) / 2 + (nodeWid-activeWid)/2 + cutIndent;

		// now compute the number of polygons
		int extraCuts = numCuts*2 - (2-numContacts) * numCuts;
		Technology.NodeLayer [] layers = np.getLayers();
		int count = SCALABLE_TOTAL + extraCuts - boxOffset;
		Technology.NodeLayer [] newNodeLayers = new Technology.NodeLayer[count];

		// load the basic layers
		int fillIndex = 0;
		for(int box = boxOffset; box < SCALABLE_TOTAL; box++)
		{
			TechPoint [] oldPoints = layers[box].getPoints();
			TechPoint [] points = new TechPoint[oldPoints.length];
			for(int i=0; i<oldPoints.length; i++) points[i] = oldPoints[i].duplicate();
			switch (box)
			{
				case SCALABLE_ACTIVE_CTR:		// active that passes through gate
					points[0].getX().setAdder(actInset);
					points[0].getX().setAdder(actInset);
					points[1].getX().setAdder(-actInset);
					break;
				case SCALABLE_ACTIVE_TOP:		// active surrounding contacts
				case SCALABLE_ACTIVE_BOT:
					points[0].getX().setAdder(actContInset);
					points[1].getX().setAdder(-actContInset);
					if (insetContacts)
					{
						double shift = 0.5;
						if (points[0].getY().getAdder() < 0) shift = -0.5;
						points[0].getY().setAdder(points[0].getY().getAdder() + shift);
						points[1].getY().setAdder(points[1].getY().getAdder() + shift);
					}
					break;
				case SCALABLE_POLY:				// poly
					points[0].getX().setAdder(polyInset);
					points[1].getX().setAdder(-polyInset);
					break;
				case SCALABLE_METAL_TOP:		// metal surrounding contacts
				case SCALABLE_METAL_BOT:
					points[0].getX().setAdder(metContInset);
					points[1].getX().setAdder(-metContInset);
					if (insetContacts)
					{
						double shift = 0.5;
						if (points[0].getY().getAdder() < 0) shift = -0.5;
						points[0].getY().setAdder(points[0].getY().getAdder() + shift);
						points[1].getY().setAdder(points[1].getY().getAdder() + shift);
					}
					break;
				case SCALABLE_WELL:				// well and select
				case SCALABLE_SUBSTRATE:
					if (insetContacts)
					{
						points[0].getY().setAdder(points[0].getY().getAdder() + 0.5);
						points[1].getY().setAdder(points[1].getY().getAdder() - 0.5);
					}
					break;
			}
			newNodeLayers[fillIndex] = new Technology.NodeLayer(layers[box].getLayer(), layers[box].getPortNum(),
				layers[box].getStyle(), layers[box].getRepresentation(), points);
			fillIndex++;
		}

		// load the contact cuts
		for(int box = 0; box < extraCuts; box++)
		{
			int oldIndex = SCALABLE_TOTAL;
			if (box >= numCuts) oldIndex++;

			// make a new description of this layer
			TechPoint [] oldPoints = layers[oldIndex].getPoints();
			TechPoint [] points = new TechPoint[oldPoints.length];
			for(int i=0; i<oldPoints.length; i++) points[i] = oldPoints[i].duplicate();
			if (numCuts == 1)
			{
				points[0].getX().setAdder(ni.getXSize() / 2 - cutSize/2);
				points[1].getX().setAdder(ni.getXSize() / 2 + cutSize/2);
			} else
			{
				int cut = box % numCuts;
				double base = cutBase + cut * (cutSize + cutSep);
				points[0].getX().setAdder(base);
				points[1].getX().setAdder(base + cutSize);
			}
			if (insetContacts)
			{
				double shift = 0.5;
				if (points[0].getY().getAdder() < 0) shift = -0.5;
				points[0].getY().setAdder(points[0].getY().getAdder() + shift);
				points[1].getY().setAdder(points[1].getY().getAdder() + shift);
			}
			newNodeLayers[fillIndex] = new Technology.NodeLayer(layers[oldIndex].getLayer(), layers[oldIndex].getPortNum(),
				layers[oldIndex].getStyle(), layers[oldIndex].getRepresentation(), points);
			fillIndex++;
		}

		// now let the superclass convert it to Polys
		return super.getShapeOfNode(ni, false, reasonable, newNodeLayers, null);
	}

	/******************** PARAMETERIZABLE DESIGN RULES ********************/

	/**
	 * Method to build "factory" design rules, given the current technology settings.
	 * @return the "factory" design rules for this Technology.
	 * Returns null if there is an error loading the rules.
     */
    @Override
	public XMLRules getFactoryDesignRules()
	{
        Foundry foundry = getSelectedFoundry();
        List<DRCTemplate> theRules = foundry.getRules();
        XMLRules rules = new XMLRules(this);

        assert(foundry != null);

        // Resize primitives according to the foundry
//        resizeNodes(foundry.equals(Foundry.Type.TSMC.name());

		// load the DRC tables from the explanation table
        int numMetals = getNumMetals();
        int rulesMode = getRuleSet();

		for(int pass=0; pass<2; pass++)
		{
			for(DRCTemplate rule : theRules)
			{
				// see if the rule applies
				if (pass == 0)
				{
					if (rule.ruleType == DRCTemplate.DRCRuleType.NODSIZ) continue;
				} else
				{
					if (rule.ruleType != DRCTemplate.DRCRuleType.NODSIZ) continue;
				}

				int when = rule.when;
                boolean goodrule = true;
				if ((when&(DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode())) != 0)
				{
					switch (rulesMode)
					{
						case DEEPRULES:  if ((when&DRCTemplate.DRCMode.DE.mode()) == 0) goodrule = false;   break;
						case SUBMRULES:  if ((when&DRCTemplate.DRCMode.SU.mode()) == 0) goodrule = false;   break;
						case SCMOSRULES: if ((when&DRCTemplate.DRCMode.SC.mode()) == 0) goodrule = false;   break;
					}
					if (!goodrule) continue;
				}
				if ((when&(DRCTemplate.DRCMode.M2.mode()|DRCTemplate.DRCMode.M3.mode()|DRCTemplate.DRCMode.M4.mode()|DRCTemplate.DRCMode.M5.mode()|DRCTemplate.DRCMode.M6.mode())) != 0)
				{
					switch (numMetals)
					{
						case 2:  if ((when&DRCTemplate.DRCMode.M2.mode()) == 0) goodrule = false;   break;
						case 3:  if ((when&DRCTemplate.DRCMode.M3.mode()) == 0) goodrule = false;   break;
						case 4:  if ((when&DRCTemplate.DRCMode.M4.mode()) == 0) goodrule = false;   break;
						case 5:  if ((when&DRCTemplate.DRCMode.M5.mode()) == 0) goodrule = false;   break;
						case 6:  if ((when&DRCTemplate.DRCMode.M6.mode()) == 0) goodrule = false;   break;
					}
					if (!goodrule) continue;
				}
				if ((when&DRCTemplate.DRCMode.AC.mode()) != 0)
				{
					if (!isAlternateActivePolyRules()) continue;
				}
				if ((when&DRCTemplate.DRCMode.NAC.mode()) != 0)
				{
					if (isAlternateActivePolyRules()) continue;
				}
				if ((when&DRCTemplate.DRCMode.SV.mode()) != 0)
				{
					if (isDisallowStackedVias()) continue;
				}
				if ((when&DRCTemplate.DRCMode.NSV.mode()) != 0)
				{
					if (!isDisallowStackedVias()) continue;
				}
				if ((when&DRCTemplate.DRCMode.AN.mode()) != 0)
				{
					if (!isAnalog()) continue;
				}

				// get more information about the rule
				String proc = "";
				if ((when&(DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode())) != 0)
				{
					switch (rulesMode)
					{
						case DEEPRULES:  proc = "DEEP";   break;
						case SUBMRULES:  proc = "SUBM";   break;
						case SCMOSRULES: proc = "SCMOS";  break;
					}
				}
				String metal = "";
				if ((when&(DRCTemplate.DRCMode.M2.mode()|DRCTemplate.DRCMode.M3.mode()|DRCTemplate.DRCMode.M4.mode()|DRCTemplate.DRCMode.M5.mode()|DRCTemplate.DRCMode.M6.mode())) != 0)
				{
					switch (getNumMetals())
					{
						case 2:  metal = "2m";   break;
						case 3:  metal = "3m";   break;
						case 4:  metal = "4m";   break;
						case 5:  metal = "5m";   break;
						case 6:  metal = "6m";   break;
					}
					if (!goodrule) continue;
				}
				String ruleName = rule.ruleName;
				String extraString = metal + proc;
                if (extraString.length() > 0 && ruleName.indexOf(extraString) == -1) {
                    rule = new DRCTemplate(rule);
                    rule.ruleName +=  ", " +  extraString;
                }

                rules.loadDRCRules(this, foundry, rule);
			}
		}

        // Resize primitives according to the foundry and existing rules.
        rules.resizeMetalContacts(metalContactNodes, numMetals);

        // Active contacts
        rules.resizeContactsWithActive(metalActiveContactNodes, null, false, false, false);

        // Well contacts
        rules.resizeContactsWithActive(metalWellContactNodes, null, false, false, false);

        // Poly contact
        rules.resizePolyContact(metal1PolyContactNodes[0]);

        // Standard transistors
        DRCTemplate polyWid = null;
        for (PrimitiveNode primNode : transistorNodes)
        {
            // Not very elegant here
            Technology.NodeLayer activeNode = primNode.getLayers()[0]; // active
            Technology.NodeLayer activeTNode = primNode.getElectricalLayers()[0]; // active Top or Left
            Technology.NodeLayer activeBNode = primNode.getElectricalLayers()[1]; // active Bottom or Right
            Technology.NodeLayer polyNode = primNode.getLayers()[1]; // poly
            Technology.NodeLayer polyCNode = primNode.getElectricalLayers()[2]; // poly center
            Technology.NodeLayer polyLNode = primNode.getElectricalLayers()[3]; // poly left or Top
            Technology.NodeLayer polyRNode = primNode.getElectricalLayers()[4]; // poly right or bottom
            Technology.NodeLayer wellNode = primNode.getLayers()[2]; // well
            Technology.NodeLayer selNode = primNode.getLayers()[3]; // select

            // setting well-active actSurround
            int index = rules.getRuleIndex(activeNode.getLayer().getIndex(), wellNode.getLayer().getIndex());
            DRCTemplate actSurround = rules.getRule(index, DRCTemplate.DRCRuleType.SURROUND, primNode.getName());
            double length = primNode.getDefHeight();
            if (polyWid == null)
                polyWid = rules.getRule(polyNode.getLayer().getIndex(), DRCTemplate.DRCRuleType.MINWID); // gate size
            // active from poly
            double actOverhang = getTransistorExtension(primNode, false, rules);
            double lenValMax = DBMath.round(length /2 - (polyWid.getValue(0)/2));   // Y if poly gate is horizontal, X if poly is vertical
            double lenValMin = DBMath.round(lenValMax - actOverhang);
            // Active layer
            activeNode.getBottomEdge().setAdder(lenValMin); activeNode.getTopEdge().setAdder(-lenValMin);
            activeBNode.getBottomEdge().setAdder(lenValMin); activeBNode.getTopEdge().setAdder(lenValMax);
            activeTNode.getTopEdge().setAdder(-lenValMin); activeTNode.getBottomEdge().setAdder(-lenValMax);

            // poly from active
            double gateOverhang = getTransistorExtension(primNode, true, rules);
            double polyExten = actSurround.getValue(0) - gateOverhang;
            double gateEdge = polyExten + gateOverhang;

            polyNode.getBottomEdge().setAdder(lenValMax);  polyNode.getTopEdge().setAdder(-lenValMax);
            polyNode.getLeftEdge().setAdder(polyExten);    polyNode.getRightEdge().setAdder(-polyExten);
            polyLNode.getBottomEdge().setAdder(lenValMax); polyLNode.getTopEdge().setAdder(-lenValMax);
            polyLNode.getLeftEdge().setAdder(polyExten);   polyLNode.getRightEdge().setAdder(gateEdge);
            polyRNode.getBottomEdge().setAdder(lenValMax); polyRNode.getTopEdge().setAdder(-lenValMax);
            polyRNode.getLeftEdge().setAdder(-gateEdge);   polyRNode.getRightEdge().setAdder(-polyExten);
            polyCNode.getBottomEdge().setAdder(lenValMax); polyCNode.getTopEdge().setAdder(-lenValMax);
            polyCNode.getLeftEdge().setAdder(gateEdge);    polyCNode.getRightEdge().setAdder(-gateEdge);

            // select
            index = rules.getRuleIndex(activeNode.getLayer().getIndex(), selNode.getLayer().getIndex());
            DRCTemplate selSurround = rules.getRule(index, DRCTemplate.DRCRuleType.SURROUND, primNode.getName());
            index = rules.getRuleIndex(polyNode.getLayer().getIndex(), selNode.getLayer().getIndex());
            DRCTemplate selPolySurround = rules.getRule(index, DRCTemplate.DRCRuleType.SURROUND, primNode.getName());
            double selExtenOppLen = actSurround.getValue(0) - selPolySurround.getValue(0);
            double selExtenAlongLen = lenValMin - selSurround.getValue(0);// only valid on active extension (Y axis in 180nm)

            selNode.getLeftEdge().setAdder(selExtenOppLen); selNode.getRightEdge().setAdder(-selExtenOppLen);
            selNode.getBottomEdge().setAdder(selExtenAlongLen); selNode.getTopEdge().setAdder(-selExtenAlongLen);

            // setting serpentine factors
            double serpPolyWid = polyWid.getValue(0)/2;
            double serpActiveWid = serpPolyWid + actOverhang;
            double serpSelectWid = serpActiveWid + selSurround.getValue(0);
            double serpWellWid = serpActiveWid + actSurround.getValue(0);
            double serpSelectExt = selPolySurround.getValue(0);
            double serpWellExt = actSurround.getValue(0);
            polyNode.setSerpentineLWidth(serpPolyWid);
            polyNode.setSerpentineRWidth(serpPolyWid);
            polyNode.setSerpentineExtentT(gateOverhang);
            polyNode.setSerpentineExtentB(gateOverhang);
            polyCNode.setSerpentineLWidth(serpPolyWid);
            polyCNode.setSerpentineRWidth(serpPolyWid);
            polyLNode.setSerpentineLWidth(serpPolyWid);
            polyLNode.setSerpentineRWidth(serpPolyWid);
            polyLNode.setSerpentineExtentB(gateOverhang);
            polyRNode.setSerpentineLWidth(serpPolyWid);
            polyRNode.setSerpentineRWidth(serpPolyWid);
            polyRNode.setSerpentineExtentT(gateOverhang);
            activeNode.setSerpentineLWidth(serpActiveWid);
            activeNode.setSerpentineRWidth(serpActiveWid);
            activeTNode.setSerpentineLWidth(serpActiveWid);
            activeBNode.setSerpentineRWidth(serpActiveWid);
            selNode.setSerpentineLWidth(serpSelectWid);
            selNode.setSerpentineRWidth(serpSelectWid);
            selNode.setSerpentineExtentT(serpSelectExt);
            selNode.setSerpentineExtentB(serpSelectExt);
            wellNode.setSerpentineLWidth(serpWellWid);
            wellNode.setSerpentineRWidth(serpWellWid);
            wellNode.setSerpentineExtentT(serpWellExt);
            wellNode.setSerpentineExtentB(serpWellExt);

            primNode.setSizeOffset(new SizeOffset(actSurround.getValue(0), actSurround.getValue(0),
                    lenValMax, lenValMax));
        }

        // arcs
        for (Iterator<ArcProto> it = getArcs(); it.hasNext(); ) {
            ArcProto ap = it.next();
            DRCTemplate wid = rules.getRule(ap.getLayer(0).getIndex(), DRCTemplate.DRCRuleType.MINWID); // size
            if (wid == null) continue;
            double width = DBMath.round(wid.getValue(0));
            double half = DBMath.round(width/2);
            int baseExtend = (int)DBMath.lambdaToGrid(width*0.5);
            ap.setExtends(baseExtend, baseExtend + ap.getGridFullExtend() - ap.getGridBaseExtend());
        }

//        // poly arcs
//        double width = DBMath.round(polyWid.getValue(0));
//        double half = DBMath.round(width/2);
//        polyArcs[0].setDefaultLambdaBaseWidth(width);
////        polyArcs[0].setDefaultLambdaFullWidth(width);
//        polyPinNodes[0].setDefSize(width, width);
//        PrimitivePort polyPort = polyPinNodes[0].getPort(0);
//        polyPort.getLeft().setAdder(half); polyPort.getBottom().setAdder(half);
//        polyPort.getRight().setAdder(-half); polyPort.getTop().setAdder(-half);

        // resizing all pure layer nodes
//        for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
//        {
//            PrimitiveNode pnp = it.next();
//            if (pnp.isNotUsed()) continue;
//            if (pnp.getFunction() != PrimitiveNode.Function.NODE) continue;
//            rules.getRule(pnp.getElectricalLayers()[0].getLayer().getIndex(), DRCTemplate.DRCRuleType.MINWID); // gate size
//        }
        // Information for palette
        buildTechPalette();

        return rules;
    }

    @Override
    public SizeCorrector getSizeCorrector(Version version, Map<Setting,Object> projectSettings, boolean isJelib, boolean keepExtendOverMin) {
        SizeCorrector sc = super.getSizeCorrector(version, projectSettings, isJelib, keepExtendOverMin);
        if (!keepExtendOverMin) return sc;
        boolean newDefaults = version.compareTo(Version.parseVersion("8.04u")) >= 0;
        int numMetals = newDefaults ? 6 : 4;
        boolean isSecondPolysilicon = newDefaults ? true : false;
        int ruleSet = SUBMRULES;

        Object numMetalsValue = projectSettings.get(getNumMetalsSetting());
        if (numMetalsValue instanceof Integer)
            numMetals = ((Integer)numMetalsValue).intValue();

        Object secondPolysiliconValue = projectSettings.get(getSecondPolysiliconSetting());
        if (secondPolysiliconValue instanceof Boolean)
            isSecondPolysilicon = ((Boolean)secondPolysiliconValue).booleanValue();
        else if (secondPolysiliconValue instanceof Integer)
            isSecondPolysilicon = ((Integer)secondPolysiliconValue).intValue() != 0;

        Object ruleSetValue = projectSettings.get(getRuleSetSetting());
        if (ruleSetValue instanceof Integer)
            ruleSet = ((Integer)ruleSetValue).intValue();

        if (numMetals == getNumMetals() && isSecondPolysilicon == isSecondPolysilicon() && ruleSet == getRuleSet() && version.compareTo(changeOfMetal6) >= 0)
            return sc;

        setArcCorrection(sc, "Polysilicon-2", ruleSet == SCMOSRULES ? 3 : 7);
        setArcCorrection(sc, "Metal-3", numMetals <= 3 ? (ruleSet == SCMOSRULES ? 6 : 5) : 3);
        setArcCorrection(sc, "Metal-4", numMetals <= 4 ? 6 : 3);
        setArcCorrection(sc, "Metal-5", numMetals <= 5 ? 4 : 3);
        if (version.compareTo(changeOfMetal6) < 0) // Fix of bug #357
            setArcCorrection(sc, "Metal-6", 4);

        return sc;
    }

//    /**
//	 * Method to compare a Rules set with the "factory" set and construct an override string.
//	 * @param origDRCRules
//	 * @param newDRCRules
//	 * @return a StringBuffer that describes any overrides.  Returns "" if there are none.
//	 */
//	public static StringBuffer getRuleDifferences(DRCRules origDRCRules, DRCRules newDRCRules)
//	{
//		StringBuffer changes = new StringBuffer();
//		MOSRules origRules = (MOSRules)origDRCRules;
//		MOSRules newRules = (MOSRules)newDRCRules;
//
//		// include differences in the wide-rule limit
//		if (!newRules.wideLimit.equals(origRules.wideLimit))
//		{
//			changes.append("w:"+newRules.wideLimit+";");
//		}
//
//		// include differences in layer spacings
//		for(int l1=0; l1<tech.getNumLayers(); l1++)
//			for(int l2=0; l2<=l1; l2++)
//		{
//			int i = newRules.getRuleIndex(l2, l1);
//			if (!newRules.conList[i].equals(origRules.conList[i]))
//			{
//				changes.append("c:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conList[i]+";");
//			}
//			if (!newRules.conListRules[i].equals(origRules.conListRules[i]))
//			{
//				changes.append("cr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListRules[i]+";");
//			}
//			if (!newRules.unConList[i].equals(origRules.unConList[i]))
//			{
//				changes.append("u:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConList[i]+";");
//			}
//			if (!newRules.unConListRules[i].equals(origRules.unConListRules[i]))
//			{
//				changes.append("ur:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListRules[i]+";");
//			}
//
//			if (!newRules.conListWide[i].equals(origRules.conListWide[i]))
//			{
//				changes.append("cw:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListWide[i]+";");
//			}
//			if (!newRules.conListWideRules[i].equals(origRules.conListWideRules[i]))
//			{
//				changes.append("cwr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListWideRules[i]+";");
//			}
//			if (!newRules.unConListWide[i].equals(origRules.unConListWide[i]))
//			{
//				changes.append("uw:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListWide[i]+";");
//			}
//			if (!newRules.unConListWideRules[i].equals(origRules.unConListWideRules[i]))
//			{
//				changes.append("uwr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListWideRules[i]+";");
//			}
//
//			if (!newRules.conListMulti[i].equals(origRules.conListMulti[i]))
//			{
//				changes.append("cm:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListMulti[i]+";");
//			}
//			if (!newRules.conListMultiRules[i].equals(origRules.conListMultiRules[i]))
//			{
//				changes.append("cmr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListMultiRules[i]+";");
//			}
//			if (!newRules.unConListMulti[i].equals(origRules.unConListMulti[i]))
//			{
//				changes.append("um:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListMulti[i]+";");
//			}
//			if (!newRules.unConListMultiRules[i].equals(origRules.unConListMultiRules[i]))
//			{
//				changes.append("umr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListMultiRules[i]+";");
//			}
//
//			if (!newRules.edgeList[i].equals(origRules.edgeList[i]))
//			{
//				changes.append("e:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.edgeList[i]+";");
//			}
//			if (!newRules.edgeListRules[i].equals(origRules.edgeListRules[i]))
//			{
//				changes.append("er:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.edgeListRules[i]+";");
//			}
//		}
//
//		// include differences in minimum layer widths
//		for(int i=0; i<newRules.numLayers; i++)
//		{
//			if (!newRules.minWidth[i].equals(origRules.minWidth[i]))
//			{
//				changes.append("m:"+tech.getLayer(i).getName()+"="+newRules.minWidth[i]+";");
//			}
//			if (!newRules.minWidthRules[i].equals(origRules.minWidthRules[i]))
//			{
//				changes.append("mr:"+tech.getLayer(i).getName()+"="+newRules.minWidthRules[i]+";");
//			}
//		}
//
//		// include differences in minimum node sizes
//		int j = 0;
//		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
//		{
//			PrimitiveNode np = it.next();
//			if (!newRules.minNodeSize[j*2].equals(origRules.minNodeSize[j*2]) ||
//				!newRules.minNodeSize[j*2+1].equals(origRules.minNodeSize[j*2+1]))
//			{
//				changes.append("n:"+np.getName()+"="+newRules.minNodeSize[j*2]+"/"+newRules.minNodeSize[j*2+1]+";");
//			}
//			if (!newRules.minNodeSizeRules[j].equals(origRules.minNodeSizeRules[j]))
//			{
//				changes.append("nr:"+np.getName()+"="+newRules.minNodeSizeRules[j]+";");
//			}
//			j++;
//		}
//		return changes;
//	}

	/**
	 * Method to be called from DRC:setRules
	 * @param newDRCRules
	 */
	public void setRuleVariables(DRCRules newDRCRules)
	{
		MOSRules newRules = (MOSRules)newDRCRules;

		// update variables on the technology
// 		Variable var = newVar(DRCRules.WIDE_LIMIT, newRules.wideLimit);
// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES, newRules.conList);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES_RULE, newRules.conListRules);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES, newRules.unConList);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES_RULE, newRules.unConListRules);
// 		if (var != null) var.setDontSave();

// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES_WIDE, newRules.conListWide);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES_WIDE_RULE, newRules.conListWideRules);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES_WIDE, newRules.unConListWide);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES_WIDE_RULE, newRules.unConListWideRules);
// 		if (var != null) var.setDontSave();

// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES_MULTI, newRules.conListMulti);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES_MULTI_RULE, newRules.conListMultiRules);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES_MULTI, newRules.unConListMulti);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES_MULTI_RULE, newRules.unConListMultiRules);
// 		if (var != null) var.setDontSave();

// 		var = newVar(DRCRules.MIN_EDGE_DISTANCES, newRules.edgeList);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_EDGE_DISTANCES_RULE, newRules.edgeListRules);
// 		if (var != null) var.setDontSave();

// 		var = newVar(DRCRules.MIN_WIDTH, newRules.minWidth);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_WIDTH_RULE, newRules.minWidthRules);
// 		if (var != null) var.setDontSave();

		// update per-node information
		int j = 0;
		for(Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			np.setMinSize(newRules.minNodeSize[j*2].doubleValue(), newRules.minNodeSize[j*2+1].doubleValue(),
				newRules.minNodeSizeRules[j]);
			j++;
		}
	}

	/**
	 * Method to implement rule 3.4 which specifies the amount of active overhang
	 * on a transistor.
	 */
//	private void setTransistorActiveOverhang(double overhang)
//	{
//		TechPoint [] pActivePoints = transistorActiveLayers[P_TYPE].getPoints();
//		TechPoint [] nActivePoints = transistorActiveLayers[N_TYPE].getPoints();
//		TechPoint [] pActiveTPoints = transistorActiveTLayers[P_TYPE].getPoints();
//		TechPoint [] nActiveTPoints = transistorActiveTLayers[N_TYPE].getPoints();
//		TechPoint [] pActiveBPoints = transistorActiveBLayers[P_TYPE].getPoints();
//		TechPoint [] nActiveBPoints = transistorActiveBLayers[N_TYPE].getPoints();
//		TechPoint [] pWellPoints = transistorWellLayers[P_TYPE].getPoints();
//		TechPoint [] nWellPoints = transistorWellLayers[N_TYPE].getPoints();
//		TechPoint [] pSelectPoints = transistorSelectLayers[P_TYPE].getPoints();
//		TechPoint [] nSelectPoints = transistorSelectLayers[N_TYPE].getPoints();
//
//		// pickup extension of well about active (2.3)
//		EdgeH pActiveLeft = pActivePoints[0].getX();
//		EdgeH pWellLeft = pWellPoints[0].getX();
//		double wellOverhang = pActiveLeft.getAdder() - pWellLeft.getAdder();
//
//		// define the active box in terms of the central transistor box
//		EdgeV pActiveBottom = pActivePoints[0].getY();
//		EdgeV pActiveTop = pActivePoints[1].getY();
//		pActiveBottom.setAdder(10-overhang);
//		pActiveTop.setAdder(-10+overhang);
//		EdgeV nActiveBottom = nActivePoints[0].getY();
//		EdgeV nActiveTop = nActivePoints[1].getY();
//		nActiveBottom.setAdder(10-overhang);
//		nActiveTop.setAdder(-10+overhang);
//
//		// for the electrical rule versions with split active
//		EdgeV pActiveBBottom = pActiveBPoints[0].getY();
//		EdgeV pActiveTTop = pActiveTPoints[1].getY();
//		pActiveBBottom.setAdder(10-overhang);
//		pActiveTTop.setAdder(-10+overhang);
//		EdgeV nActiveBBottom = nActiveBPoints[0].getY();
//		EdgeV nActiveTTop = nActiveTPoints[1].getY();
//		nActiveBBottom.setAdder(10-overhang);
//		nActiveTTop.setAdder(-10+overhang);
//
//		// extension of select about active = 2 (4.2)
//		EdgeV pSelectBottom = pSelectPoints[0].getY();
//		EdgeV pSelectTop = pSelectPoints[1].getY();
//		pSelectBottom.setAdder(pActiveBottom.getAdder()-2);
//		pSelectTop.setAdder(pActiveTop.getAdder()+2);
//		EdgeV nSelectBottom = nSelectPoints[0].getY();
//		EdgeV nSelectTop = nSelectPoints[1].getY();
//		nSelectBottom.setAdder(nActiveBottom.getAdder()-2);
//		nSelectTop.setAdder(nActiveTop.getAdder()+2);
//
//		// extension of well about active (2.3)
//		EdgeV pWellBottom = pWellPoints[0].getY();
//		EdgeV pWellTop = pWellPoints[1].getY();
//		pWellBottom.setAdder(pActiveBottom.getAdder()-wellOverhang);
//		pWellTop.setAdder(pActiveTop.getAdder()+wellOverhang);
//		EdgeV nWellBottom = nWellPoints[0].getY();
//		EdgeV nWellTop = nWellPoints[1].getY();
//		nWellBottom.setAdder(nActiveBottom.getAdder()-wellOverhang);
//		nWellTop.setAdder(nActiveTop.getAdder()+wellOverhang);
//
//		// the serpentine active overhang
//		SizeOffset so = transistorNodes[P_TYPE].getProtoSizeOffset();
//		double halfPolyWidth = (transistorNodes[P_TYPE].getDefHeight() - so.getHighYOffset() - so.getLowYOffset()) / 2;
//		transistorActiveLayers[P_TYPE].setSerpentineLWidth(halfPolyWidth+overhang);
//		transistorActiveLayers[P_TYPE].setSerpentineRWidth(halfPolyWidth+overhang);
//		transistorActiveTLayers[P_TYPE].setSerpentineRWidth(halfPolyWidth+overhang);
//		transistorActiveBLayers[P_TYPE].setSerpentineLWidth(halfPolyWidth+overhang);
//		transistorActiveLayers[N_TYPE].setSerpentineLWidth(halfPolyWidth+overhang);
//		transistorActiveLayers[N_TYPE].setSerpentineRWidth(halfPolyWidth+overhang);
//		transistorActiveTLayers[N_TYPE].setSerpentineRWidth(halfPolyWidth+overhang);
//		transistorActiveBLayers[N_TYPE].setSerpentineLWidth(halfPolyWidth+overhang);
//
//		transistorSelectLayers[P_TYPE].setSerpentineLWidth(halfPolyWidth+overhang+2);
//		transistorSelectLayers[P_TYPE].setSerpentineRWidth(halfPolyWidth+overhang+2);
//		transistorSelectLayers[N_TYPE].setSerpentineLWidth(halfPolyWidth+overhang+2);
//		transistorSelectLayers[N_TYPE].setSerpentineRWidth(halfPolyWidth+overhang+2);
//
//		transistorWellLayers[P_TYPE].setSerpentineLWidth(halfPolyWidth+overhang+wellOverhang);
//		transistorWellLayers[P_TYPE].setSerpentineRWidth(halfPolyWidth+overhang+wellOverhang);
//		transistorWellLayers[N_TYPE].setSerpentineLWidth(halfPolyWidth+overhang+wellOverhang);
//		transistorWellLayers[N_TYPE].setSerpentineRWidth(halfPolyWidth+overhang+wellOverhang);
//	}

	/******************** OPTIONS ********************/

    private final Setting cacheRuleSet = TechSetting.makeIntSetting(this, "MoCMOSRuleSet", "Technology tab", "MOSIS CMOS rule set",
        getProjectSettings(), "MOCMOS Rule Set", 1);
    {
    	cacheRuleSet.setTrueMeaning(new String[] {"SCMOS", "Submicron", "Deep"});
	}
	/**
	 * Method to tell the current rule set for this Technology if Mosis is the foundry.
	 * @return the current rule set for this Technology:<BR>
	 * 0: SCMOS rules<BR>
	 * 1: Submicron rules (the default)<BR>
	 * 2: Deep rules
	 */
    public int getRuleSet() { return cacheRuleSet.getInt(); }

//    private static DRCTemplate.DRCMode getRuleMode()
//    {
//        switch (getRuleSet())
//        {
//            case DEEPRULES: return DRCTemplate.DRCMode.DE;
//            case SUBMRULES: return DRCTemplate.DRCMode.SU;
//            case SCMOSRULES: return DRCTemplate.DRCMode.SC;
//        }
//        return null;
//    }

    /**
	 * Method to set the rule set for this Technology.
	 * @return the new rule setting for this Technology, with values:<BR>
	 * 0: SCMOS rules<BR>
	 * 1: Submicron rules<BR>
	 * 2: Deep rules
	 */
	public Setting getRuleSetSetting() { return cacheRuleSet; }

	private final Setting cacheSecondPolysilicon = TechSetting.makeBooleanSetting(this, getTechName() + "SecondPolysilicon", "Technology tab", getTechName().toUpperCase() + " CMOS: Second Polysilicon Layer",
		getProjectSettings(), "UseSecondPolysilicon", true);
	/**
	 * Method to tell the number of polysilicon layers in this Technology.
	 * The default is false.
	 * @return true if there are 2 polysilicon layers in this Technology.
	 * If false, there is only 1 polysilicon layer.
	 */
	public boolean isSecondPolysilicon() { return cacheSecondPolysilicon.getBoolean(); }
	/**
	 * Returns project Setting to tell a second polysilicon layer in this Technology.
	 * @return project Setting to tell a second polysilicon layer in this Technology.
	 */
	public Setting getSecondPolysiliconSetting() { return cacheSecondPolysilicon; }

	private final Setting cacheDisallowStackedVias = TechSetting.makeBooleanSetting(this, "MoCMOSDisallowStackedVias", "Technology tab", "MOSIS CMOS: Disallow Stacked Vias",
        getProjectSettings(), "DisallowStackedVias", false);
	/**
	 * Method to determine whether this Technology disallows stacked vias.
	 * The default is false (they are allowed).
	 * @return true if the MOCMOS technology disallows stacked vias.
	 */
	public boolean isDisallowStackedVias() { return cacheDisallowStackedVias.getBoolean(); }
	/**
	 * Returns project Setting to tell whether this Technology disallows stacked vias.
	 * @return project Setting to tell whether this Technology disallows stacked vias.
	 */
	public Setting getDisallowStackedViasSetting() { return cacheDisallowStackedVias; }

	private final Setting cacheAlternateActivePolyRules = TechSetting.makeBooleanSetting(this, "MoCMOSAlternateActivePolyRules", "Technology tab", "MOSIS CMOS: Alternate Active and Poly Contact Rules",
		getProjectSettings(), "UseAlternativeActivePolyRules", false);
	/**
	 * Method to determine whether this Technology is using alternate Active and Poly contact rules.
	 * The default is false.
	 * @return true if the MOCMOS technology is using alternate Active and Poly contact rules.
	 */
	public boolean isAlternateActivePolyRules() { return cacheAlternateActivePolyRules.getBoolean(); }
	/**
	 * Returns project Setting to tell whether this Technology is using alternate Active and Poly contact rules.
	 * @return project Setting to tell whether this Technology is using alternate Active and Poly contact rules.
	 */
	public Setting getAlternateActivePolyRulesSetting() { return cacheAlternateActivePolyRules; }

	private final Setting cacheAnalog = TechSetting.makeBooleanSetting(this, getTechName() + "Analog", "Technology tab", "MOSIS CMOS: Vertical NPN transistor pbase",
		getProjectSettings(), "Analog", false);
	/**
	 * Method to tell whether this technology has layers for vertical NPN transistor pbase.
	 * The default is false.
	 * @return true if this Technology has layers for vertical NPN transistor pbase.
	 */
	public boolean isAnalog() { return cacheAnalog.getBoolean(); }
	/**
	 * Returns project Setting to tell whether this technology has layers for vertical NPN transistor pbase.
	 * @return project Setting to tell whether this technology has layers for vertical NPN transistor pbase.
	 */
	public Setting getAnalogSetting() { return cacheAnalog; }

    /** set if no stacked vias allowed */			private static final int MOCMOSNOSTACKEDVIAS =   01;
//	/** set for stick-figure display */				private static final int MOCMOSSTICKFIGURE =     02;
	/** number of metal layers */					private static final int MOCMOSMETALS =         034;
	/**   2-metal rules */							private static final int MOCMOS2METAL =           0;
	/**   3-metal rules */							private static final int MOCMOS3METAL =          04;
	/**   4-metal rules */							private static final int MOCMOS4METAL =         010;
	/**   5-metal rules */							private static final int MOCMOS5METAL =         014;
	/**   6-metal rules */							private static final int MOCMOS6METAL =         020;
	/** type of rules */							private static final int MOCMOSRULESET =       0140;
	/**   set if submicron rules in use */			private static final int MOCMOSSUBMRULES =        0;
	/**   set if deep rules in use */				private static final int MOCMOSDEEPRULES =      040;
	/**   set if standard SCMOS rules in use */		private static final int MOCMOSSCMOSRULES =    0100;
	/** set to use alternate active/poly rules */	private static final int MOCMOSALTAPRULES =    0200;
	/** set to use second polysilicon layer */		private static final int MOCMOSTWOPOLY =       0400;
//	/** set to show special transistors */			private static final int MOCMOSSPECIALTRAN =  01000;

	/**
	 * Method to convert any old-style state information to the new options.
	 */
	/**
	 * Method to convert any old-style variable information to the new options.
	 * May be overrideen in subclasses.
	 * @param varName name of variable
	 * @param value value of variable
	 * @return true if variable was converted
	 */
    @Override
	public Map<Setting,Object> convertOldVariable(String varName, Object value)
	{
        if (varName.equals("MoCMOSNumberOfMetalLayers") || varName.equals("MOCMOSNumberOfMetalLayers"))
            return Collections.singletonMap(getNumMetalsSetting(), value);
        if (varName.equals("MoCMOSSecondPolysilicon"))
            return Collections.singletonMap(getSecondPolysiliconSetting(), value);
        
        if (!varName.equalsIgnoreCase(TECH_LAST_STATE.getName())) return null;
		if (!(value instanceof Integer)) return null;
		int oldBits = ((Integer)value).intValue();

        HashMap<Setting,Object> settings = new HashMap<Setting,Object>();

		boolean oldNoStackedVias = (oldBits&MOCMOSNOSTACKEDVIAS) != 0;
		settings.put(getDisallowStackedViasSetting(), new Integer(oldNoStackedVias?1:0));

		int numMetals = 0;
		switch (oldBits&MOCMOSMETALS)
		{
			case MOCMOS2METAL: numMetals = 2;   break;
			case MOCMOS3METAL: numMetals = 3;   break;
			case MOCMOS4METAL: numMetals = 4;   break;
			case MOCMOS5METAL: numMetals = 5;   break;
			case MOCMOS6METAL: numMetals = 6;   break;
		}
		settings.put(getNumMetalsSetting(), new Integer(numMetals));

		int ruleSet = 0;
		switch (oldBits&MOCMOSRULESET)
		{
			case MOCMOSSUBMRULES:  ruleSet = SUBMRULES;   break;
			case MOCMOSDEEPRULES:  ruleSet = DEEPRULES;   break;
			case MOCMOSSCMOSRULES: ruleSet = SCMOSRULES;  break;
		}
		settings.put(getRuleSetSetting(), new Integer(ruleSet));

		boolean alternateContactRules = (oldBits&MOCMOSALTAPRULES) != 0;
		settings.put(getAlternateActivePolyRulesSetting(), new Integer(alternateContactRules?1:0));

		boolean secondPoly = (oldBits&MOCMOSTWOPOLY) != 0;
		settings.put(getSecondPolysiliconSetting(), new Integer(secondPoly?1:0));

		return settings;
	}
/******************** OVERRIDES ********************/
    /**
     * Method to set the size of a transistor NodeInst in this Technology.
     * Override because for MOCMOS sense of "width" and "length" are
     * different for resistors and transistors.
     * @param ni the NodeInst
     * @param width the new width (positive values only)
     * @param length the new length (positive values only)
     */
    @Override
    public void setPrimitiveNodeSize(NodeInst ni, double width, double length)
    {
        if (ni.getFunction().isResistor()) {
        	super.setPrimitiveNodeSize(ni, length, width);
        } else {
        	super.setPrimitiveNodeSize(ni, width, length);
        }
    }

    /**
     * Method to calculate extension of the poly gate from active layer or of the active from the poly gate.
     * @param primNode
     * @param poly true to calculate the poly extension
     * @param rules
     * @return value of the extension
     */
    private double getTransistorExtension(PrimitiveNode primNode, boolean poly, DRCRules rules)
    {
        if (!primNode.getFunction().isTransistor()) return 0.0;

        Technology.NodeLayer activeNode = primNode.getLayers()[0]; // active
        Technology.NodeLayer polyCNode;

        if (scalableTransistorNodes != null && (primNode == scalableTransistorNodes[P_TYPE] || primNode == scalableTransistorNodes[N_TYPE]))
        {
            polyCNode = primNode.getLayers()[5]; // poly center
        }
        else
        {
            // Standard transistors
            polyCNode = primNode.getElectricalLayers()[2]; // poly center
        }
        DRCTemplate overhang = (poly) ?
                rules.getExtensionRule(polyCNode.getLayer(), activeNode.getLayer(), false) :
                rules.getExtensionRule(activeNode.getLayer(), polyCNode.getLayer(), false);
        return (overhang != null ? overhang.getValue(0) : 0.0);
    }
}

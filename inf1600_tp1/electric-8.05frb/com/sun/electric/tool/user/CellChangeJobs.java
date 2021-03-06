/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellChangeJobs.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.IdMapper;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for Jobs that make changes to the cells.
 */
public class CellChangeJobs
{
	// constructor, never used
	private CellChangeJobs() {}

	/****************************** DELETE A CELL ******************************/

	/**
	 * Class to delete a cell in a new thread.
	 */
	public static class DeleteCell extends Job
	{
		Cell cell;

		public DeleteCell(Cell cell)
		{
			super("Delete " + cell, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// check cell usage once more
			if (cell.isInUse("delete", false)) return false;
			cell.kill();
			return true;
		}
	}

	/**
	 * This class implement the command to delete a list of cells.
	 */
	public static class DeleteManyCells extends Job
	{
		private List<Cell> cellsToDelete;

		public DeleteManyCells(List<Cell> cellsToDelete)
		{
			super("Delete Multiple Cells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cellsToDelete = cellsToDelete;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			for(Cell cell : cellsToDelete)
			{
				System.out.println("Deleting " + cell);
				cell.kill();
			}
			return true;
		}

		public void terminateOK()
		{
			System.out.println("Deleted " + cellsToDelete.size() + " cells");
			EditWindow.repaintAll();
		}
	}

	/****************************** RENAME CELLS ******************************/

	/**
	 * Class to rename a cell in a new thread.
	 */
	public static class RenameCell extends Job
	{
		private Cell cell;
		private String newName;
		private String newGroupCell;
		private IdMapper idMapper;

		public RenameCell(Cell cell, String newName, String newGroupCell)
		{
			super("Rename " + cell, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newName = newName;
			this.newGroupCell = newGroupCell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			idMapper = cell.rename(newName, newGroupCell);
			fieldVariableChanged("idMapper");
			return true;
		}

		public void terminateOK()
		{
			User.fixStaleCellReferences(idMapper);
		}
	}

	/**
	 * Class to rename a cell in a new thread.
	 */
	public static class RenameCellGroup extends Job
	{
		Cell cellInGroup;
		String newName;

		public RenameCellGroup(Cell cellInGroup, String newName)
		{
			super("Rename Cell Group", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cellInGroup = cellInGroup;
			this.newName = newName;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// see if all cells in the group have the same name
			boolean allSameName = true;
			String lastName = null;
			for(Iterator<Cell> it = cellInGroup.getCellGroup().getCells(); it.hasNext(); )
			{
				String cellName = it.next().getName();
				if (lastName != null && !lastName.equals(cellName))
				{
					allSameName = false;
					break;
				}
				lastName = cellName;
			}

			ArrayList<Cell> cells = new ArrayList<Cell>();
			for(Iterator<Cell> it = cellInGroup.getCellGroup().getCells(); it.hasNext(); )
				cells.add(it.next());
			String newGroupCell = null;
			for(Cell cell : cells)
			{
				if (allSameName)
				{
					cell.rename(newName, newName);
				} else
				{
					if (newGroupCell == null)
						newGroupCell = newName + cell.getName();
					cell.rename(newName+cell.getName(), newGroupCell);
				}
			}
			return true;
		}
	}

	/****************************** SHOW CELLS GRAPHICALLY ******************************/

	/**
	 * This class implement the command to make a graph of the cells.
	 */
	public static class GraphCells extends Job
	{
		private static final double TEXTHEIGHT = 2;

		private Cell top;
		private Cell graphCell;

		private static class CellGraphNode
		{
			int			depth;
			int			clock;
			double		 x;
			double		 y;
			double		 yoff;
			NodeInst	   pin;
			NodeInst	   topPin;
			NodeInst	   botPin;
			CellGraphNode  main;
		}

		public GraphCells(Cell top)
		{
			super("Graph Cells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.top = top;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// create the graph cell
			graphCell = Cell.newInstance(Library.getCurrent(), "CellStructure");
			fieldVariableChanged("graphCell");
			if (graphCell == null) return false;
			if (graphCell.getNumVersions() > 1)
				System.out.println("Creating new version of cell: CellStructure"); else
					System.out.println("Creating cell: CellStructure");

			// create CellGraphNodes for every cell and initialize the depth to -1
			HashMap<Cell,CellGraphNode> cellGraphNodes = new HashMap<Cell,CellGraphNode>();
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					CellGraphNode cgn = new CellGraphNode();
					cgn.depth = -1;
					cellGraphNodes.put(cell, cgn);
				}
			}

			// find all top-level cells
			if (top != null)
			{
				CellGraphNode cgn = cellGraphNodes.get(top);
				cgn.depth = 0;
			} else
			{
				for(Iterator<Cell> cIt = Library.getCurrent().getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					if (cell.getNumUsagesIn() == 0)
					{
						CellGraphNode cgn = cellGraphNodes.get(cell);
						cgn.depth = 0;
					}
				}
			}

			// now place all cells at their proper depth
			int maxDepth = 0;
			boolean more = true;
			while (more)
			{
				more = false;
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = it.next();
					if (lib.isHidden()) continue;
					for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						CellGraphNode cgn = cellGraphNodes.get(cell);
						if (cgn.depth == -1) continue;
						for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
						{
							NodeInst ni = nIt.next();
							if (!ni.isCellInstance()) continue;
							Cell sub = (Cell)ni.getProto();

							// ignore recursive references (showing icon in contents)
							if (ni.isIconOfParent()) continue;

							CellGraphNode subCgn = cellGraphNodes.get(sub);
							if (subCgn.depth <= cgn.depth)
							{
								subCgn.depth = cgn.depth + 1;
								if (subCgn.depth > maxDepth) maxDepth = subCgn.depth;
								more = true;
							}
							Cell trueCell = sub.contentsView();
							if (trueCell == null) continue;
							CellGraphNode trueCgn = cellGraphNodes.get(trueCell);
							if (trueCgn.depth <= cgn.depth)
							{
								trueCgn.depth = cgn.depth + 1;
								if (trueCgn.depth > maxDepth) maxDepth = trueCgn.depth;
								more = true;
							}
						}
					}
				}

				// add in any cells referenced from other libraries
				if (!more && top == null)
				{
					for(Iterator<Cell> cIt = Library.getCurrent().getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						CellGraphNode cgn = cellGraphNodes.get(cell);
						if (cgn.depth >= 0) continue;
						cgn.depth = 0;
						more = true;
					}
				}
			}

			// now assign X coordinates to each cell
			maxDepth++;
			double maxWidth = 0;
			double [] xval = new double[maxDepth];
			double [] yoff = new double[maxDepth];
			for(int i=0; i<maxDepth; i++) xval[i] = yoff[i] = 0;
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					CellGraphNode cgn = cellGraphNodes.get(cell);

					// ignore icon cells from the graph (merge with contents)
					if (cgn.depth == -1) continue;

					// ignore associated cells for now
					Cell trueCell = graphMainView(cell);
					if (trueCell != null &&
						(cell.getNumUsagesIn() == 0 || cell.isIcon() ||
							cell.getView() == View.LAYOUTSKEL))
					{
						cgn.depth = -1;
						continue;
					}

					cgn.x = xval[cgn.depth];
					xval[cgn.depth] += cell.describe(false).length();
					if (xval[cgn.depth] > maxWidth) maxWidth = xval[cgn.depth];
					cgn.y = cgn.depth;
					cgn.yoff = 0;
				}
			}

			// now center each row
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					CellGraphNode cgn = cellGraphNodes.get(cell);
					if (cgn.depth == -1) continue;
					if (xval[(int)cgn.y] < maxWidth)
					{
						double spread = maxWidth / xval[(int)cgn.y];
						cgn.x = cgn.x * spread;
					}
				}
			}

			// generate accurate X/Y coordinates
			double xScale = 2.0 / 3.0;
			double yScale = 20;
			double yOffset = TEXTHEIGHT * 1.25;
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					CellGraphNode cgn = cellGraphNodes.get(cell);
					if (cgn.depth == -1) continue;
					double x = cgn.x;   double y = cgn.y;
					x = x * xScale;
					y = -y * yScale + ((yoff[(int)cgn.y]++)%3) * yOffset;
					cgn.x = x;   cgn.y = y;
				}
			}

			// make unattached cells sit with their contents view
			if (top == null)
			{
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = it.next();
					if (lib.isHidden()) continue;
					for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						CellGraphNode cgn = cellGraphNodes.get(cell);
						if (cgn.depth != -1) continue;

						if (cell.getNumUsagesIn() != 0 && !cell.isIcon() &&
							cell.getView() != View.LAYOUTSKEL) continue;
						Cell trueCell = graphMainView(cell);
						if (trueCell == null) continue;
						CellGraphNode trueCgn = cellGraphNodes.get(trueCell);
						if (trueCgn.depth == -1) continue;

						cgn.pin = cgn.topPin = cgn.botPin = null;
						cgn.main = trueCgn;
						cgn.yoff += yOffset*2;
						cgn.x = trueCgn.x;
						cgn.y = trueCgn.y + trueCgn.yoff;
					}
				}
			}

			// write the header message
			double xsc = maxWidth * xScale / 2;
			NodeInst titleNi = NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(xsc, yScale), 0, 0, graphCell);
			if (titleNi == null) return false;
			StringBuffer infstr = new StringBuffer();
			if (top != null)
			{
				infstr.append("Structure below " + top);
			} else
			{
				infstr.append("Structure of library " + Library.getCurrent().getName());
			}
			TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withRelSize(6);
			titleNi.newVar(Artwork.ART_MESSAGE, infstr.toString(), td);

			// place the components
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					if (cell == graphCell) continue;
					CellGraphNode cgn = cellGraphNodes.get(cell);
					if (cgn.depth == -1) continue;

					double x = cgn.x;   double y = cgn.y;
					cgn.pin = NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(x, y), 0, 0, graphCell);
					if (cgn.pin == null) return false;
					cgn.topPin = NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(x, y+TEXTHEIGHT/2), 0, 0, graphCell);
					if (cgn.topPin == null) return false;
					cgn.botPin = NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(x, y-TEXTHEIGHT/2), 0, 0, graphCell);
					if (cgn.botPin == null) return false;
					PortInst pinPi = cgn.pin.getOnlyPortInst();
					PortInst toppinPi = cgn.botPin.getOnlyPortInst();
					PortInst botPinPi = cgn.topPin.getOnlyPortInst();
					ArcInst link1 = ArcInst.makeInstanceBase(Generic.tech.invisible_arc, 0, toppinPi, pinPi);
					ArcInst link2 = ArcInst.makeInstanceBase(Generic.tech.invisible_arc, 0, pinPi, botPinPi);
					link1.setRigid(true);
					link2.setRigid(true);
					link1.setHardSelect(true);
					link2.setHardSelect(true);
					cgn.topPin.setHardSelect();
					cgn.botPin.setHardSelect();

					// write the cell name in the node
					TextDescriptor ctd = TextDescriptor.getNodeTextDescriptor().withRelSize(2);
					cgn.pin.newVar(Artwork.ART_MESSAGE, cell.describe(false), ctd);
				}
			}

			// attach related components with rigid arcs
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					if (cell == graphCell) continue;
					CellGraphNode cgn = cellGraphNodes.get(cell);
					if (cgn.depth == -1) continue;
					if (cgn.main == null) continue;

					PortInst firstPi = cgn.pin.getOnlyPortInst();
					ArcInst ai = ArcInst.makeInstanceBase(Artwork.tech.solidArc, 0, firstPi, firstPi);
					if (ai == null) return false;
					ai.setRigid(true);
					ai.setHardSelect(true);

					// set an invisible color on the arc
					ai.newVar(Artwork.ART_COLOR, new Integer(0));
				}
			}

			// build wires between the hierarchical levels
			int clock = 0;
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					if (cell == graphCell) continue;

					// always use the contents cell, not the icon
					Cell trueCell = cell.contentsView();
					if (trueCell == null) trueCell = cell;
					CellGraphNode trueCgn = cellGraphNodes.get(trueCell);
					if (trueCgn.depth == -1) continue;

					clock++;
					for(Iterator<NodeInst> nIt = trueCell.getNodes(); nIt.hasNext(); )
					{
						NodeInst ni = nIt.next();
						if (!ni.isCellInstance()) continue;

						// ignore recursive references (showing icon in contents)
						if (ni.isIconOfParent()) continue;
						Cell sub = (Cell)ni.getProto();

						Cell truesubnp = sub.contentsView();
						if (truesubnp == null) truesubnp = sub;

						CellGraphNode trueSubCgn = cellGraphNodes.get(truesubnp);
						if (trueSubCgn.clock == clock) continue;
						trueSubCgn.clock = clock;

						// draw a line from cell "trueCell" to cell "truesubnp"
						if (trueSubCgn.depth == -1) continue;
						PortInst toppinPi = trueCgn.botPin.getOnlyPortInst();
						PortInst niBotPi = trueSubCgn.topPin.getOnlyPortInst();
						ArcInst ai = ArcInst.makeInstance(Artwork.tech.solidArc, toppinPi, niBotPi);
						if (ai == null) return false;
						ai.setRigid(false);
						ai.setFixedAngle(false);
						ai.setSlidable(false);
						ai.setHardSelect(true);

						// set an appropriate color on the arc (red for jumps of more than 1 level of depth)
						int color = EGraphics.BLUE;
						if (trueCgn.y - trueSubCgn.y > yScale+yOffset+yOffset) color = EGraphics.RED;
						ai.newVar(Artwork.ART_COLOR, new Integer(color));
					}
				}
			}
			return true;
		}

		public void terminateOK()
		{
			// to redraw the new cell
			UserInterface ui = Job.getUserInterface();
			ui.displayCell(graphCell);
		}
	}

	/**
	 * Method to find the main cell that "np" is associated with in the graph.  This code is
	 * essentially the same as "contentscell()" except that any original type is allowed.
	 * Returns NONODEPROTO if the cell is not associated.
	 */
	private static Cell graphMainView(Cell cell)
	{
		// first check to see if there is a schematics link
		Cell mainSchem = cell.getCellGroup().getMainSchematics();
		if (mainSchem != null) return mainSchem;
// 		for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
// 		{
// 			Cell cellInGroup = it.next();
// 			if (cellInGroup.getView() == View.SCHEMATIC) return cellInGroup;
// 			if (cellInGroup.getView().isMultiPageView()) return cellInGroup;
// 		}

		// now check to see if there is any layout link
		for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = it.next();
			if (cellInGroup.getView() == View.LAYOUT) return cellInGroup;
		}

		// finally check to see if there is any "unknown" link
		for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
		{
			Cell cellInGroup = it.next();
			if (cellInGroup.getView() == View.UNKNOWN) return cellInGroup;
		}

		// no contents found
		return null;
	}

	/****************************** EXTRACT CELL INSTANCES ******************************/

	/**
	 * This class implement the command to delete unused old versions of cells.
	 */
	public static class PackageCell extends Job
	{
		Cell curCell;
		ERectangle bounds;
		String newCellName;

		public PackageCell(Cell curCell, Rectangle2D bounds, String newCellName)
		{
			super("Package Cell", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = curCell;
			this.bounds = ERectangle.fromLambda(bounds);
			this.newCellName = newCellName;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// create the new cell
			Cell cell = Cell.makeInstance(Library.getCurrent(), newCellName);
			if (cell == null) return false;

			// copy the nodes into the new cell
			HashMap<NodeInst,NodeInst> newNodes = new HashMap<NodeInst,NodeInst>();
			for(Iterator<RTBounds> sIt = curCell.searchIterator(bounds); sIt.hasNext(); )
			{
				RTBounds look = sIt.next();
				if (!(look instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)look;

				String name = null;
				Name oldName = ni.getNameKey();
				if (!oldName.isTempname()) name = oldName.toString();
				NodeInst newNi = NodeInst.makeInstance(ni.getProto(), new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY()),
					ni.getXSize(), ni.getYSize(), cell, ni.getOrient(), name, 0);
				if (newNi == null) return false;
				newNodes.put(ni, newNi);
				newNi.copyStateBits(ni);
				newNi.copyVarsFrom(ni);
				newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_NAME);

				// make ports where this nodeinst has them
				for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
				{
					Export pp = it.next();
					PortInst pi = newNi.findPortInstFromProto(pp.getOriginalPort().getPortProto());
					Export newPp = Export.newInstance(cell, pi, pp.getName());
					if (newPp != null)
					{
						newPp.setCharacteristic(pp.getCharacteristic());
						newPp.copyTextDescriptorFrom(pp, Export.EXPORT_NAME);
						newPp.copyVarsFrom(pp);
					}
				}
			}

			// copy the arcs into the new cell
			for(Iterator<RTBounds> sIt = curCell.searchIterator(bounds); sIt.hasNext(); )
			{
				RTBounds look = sIt.next();
				if (!(look instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)look;
				NodeInst niTail = newNodes.get(ai.getTailPortInst().getNodeInst());
				NodeInst niHead = newNodes.get(ai.getHeadPortInst().getNodeInst());
				if (niTail == null || niHead == null) continue;
				PortInst piTail = niTail.findPortInstFromProto(ai.getTailPortInst().getPortProto());
				PortInst piHead = niHead.findPortInstFromProto(ai.getHeadPortInst().getPortProto());

				String name = null;
				Name oldName = ai.getNameKey();
				if (!oldName.isTempname()) name = oldName.toString();
				ArcInst newAi = ArcInst.makeInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(), piHead, piTail, ai.getHeadLocation(),
					ai.getTailLocation(), name);
				if (newAi == null) return false;
				newAi.copyPropertiesFrom(ai);
			}
			System.out.println("Cell " + cell.describe(true) + " created");
			return true;
		}
	}

	/**
	 * This class implement the command to extract the contents of cell instances.
	 */
	public static class ExtractCellInstances extends Job
	{
		private Cell cell;
		private List<NodeInst> nodes;
		private boolean copyExports;
		private int depth;

		public ExtractCellInstances(Cell cell, List<NodeInst> highlighted, int depth, boolean copyExports, boolean startNow)
		{
			super("Extract Cell Instances", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.nodes = highlighted;
			this.copyExports = copyExports;
			this.depth = depth;
			if (!startNow)
				startJob();
			else
			{
				try {doIt(); } catch (Exception e) {e.printStackTrace();}
			}
		}

		public boolean doIt() throws JobException
		{
//			if (depth != 1)
//			{
				doArbitraryExtraction(cell, nodes, copyExports, depth);
				return true;
//			}
//			Job.getUserInterface().startProgressDialog("Extracting " + nodes.size() + " cells", null);
//			HashMap<NodeInst,HashMap<NodeInst,NodeInst>> newNodes = new HashMap<NodeInst,HashMap<NodeInst,NodeInst>>();
//			int done = 0;
//			for(NodeInst ni : nodes)
//			{
//				if (!ni.isCellInstance()) continue;
//				newNodes.put(ni, extractOneNode(ni, copyExports));
//				done++;
//				if ((done%10) == 0)
//				{
//					Job.getUserInterface().setProgressValue(done * 100 / nodes.size());
//				}
//			}
//
//			// replace arcs to the cell and exports on the cell
//			replaceArcsAndExports(cell, newNodes);
//
//			Job.getUserInterface().stopProgressDialog();
//			return true;
		}
	}

	private static void doArbitraryExtraction(Cell cell, List<NodeInst> nodes, boolean copyExports, int depth)
	{
		Job.getUserInterface().startProgressDialog("Extracting " + nodes.size() + " cells", null);
		Map<NodeInst,Map<PortInst,PortInst>> newNodes = new HashMap<NodeInst,Map<PortInst,PortInst>>();
		int done = 0;
		Set<NodeInst> nodesToKill = new HashSet<NodeInst>();
		List<Export> exportsToCopy = new ArrayList<Export>();
		for(NodeInst ni : nodes)
		{
			if (!ni.isCellInstance()) continue;
			HashMap<PortInst,PortInst> portMap = new HashMap<PortInst,PortInst>();
			extractOneLevel(cell, ni, GenMath.MATID, portMap, 1, depth);
			newNodes.put(ni, portMap);
			for (Iterator<Export> it = ni.getExports(); it.hasNext(); )
				exportsToCopy.add(it.next());
			done++;
			Job.getUserInterface().setProgressValue(done * 100 / nodes.size());
			nodesToKill.add(ni);
		}

		// replace arcs to the cell and exports on the cell
		Job.getUserInterface().setProgressNote("Replacing top-level arcs and exports");
		replaceExtractedArcs(cell, newNodes, GenMath.MATID);

		// replace the exports if needed
		if (copyExports)
		{
			for(Export pp : exportsToCopy)
			{
				PortInst oldPi = pp.getOriginalPort();
				Map<PortInst,PortInst> nodePortMap = newNodes.get(oldPi.getNodeInst());
				if (nodePortMap == null) continue;
				PortInst newPi = nodePortMap.get(oldPi);
				if (newPi == null)
				{
					pp.kill();
					continue;
				}
				pp.move(newPi);
			}
		}

		// delete original nodes
		cell.killNodes(nodesToKill);
		Job.getUserInterface().stopProgressDialog();
	}

	private static void extractOneLevel(Cell cell, NodeInst topno, AffineTransform prevTrans,
		Map<PortInst,PortInst> portMap, int curDepth, int totDepth)
	{
		Map<NodeInst,Map<PortInst,PortInst>> newNodes = new HashMap<NodeInst,Map<PortInst,PortInst>>();

		// make transformation matrix for this cell
		Cell subCell = (Cell)topno.getProto();
		AffineTransform localTrans = topno.translateOut(topno.rotateOut());
		localTrans.preConcatenate(prevTrans);

		for(Iterator<NodeInst> it = subCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			Map<PortInst,PortInst> subPortMap = new HashMap<PortInst,PortInst>();
			newNodes.put(ni, subPortMap);

			// do not extract "cell center" or "essential bounds" primitives
			NodeProto np = ni.getProto();
			if (np == Generic.tech.cellCenterNode || np == Generic.tech.essentialBoundsNode) continue;

			boolean extractCell = false;
			if (ni.isCellInstance() && curDepth < totDepth) extractCell = true;
			if (extractCell)
			{
				extractOneLevel(cell, ni, localTrans, subPortMap, curDepth+1, totDepth);

				// add to the portmap
				for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					PortInst fromPi = topno.findPortInstFromProto(e);
					PortInst toPi = subPortMap.get(e.getOriginalPort());
					portMap.put(fromPi, toPi);
				}
			} else
			{
				String name = null;
				if (ni.isUsernamed())
					name = ElectricObject.uniqueObjectName(ni.getName(), cell, NodeInst.class, false);
				Orientation orient = topno.getOrient().concatenate(ni.getOrient());
				Point2D pt = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
				AffineTransform instTrans = ni.rotateOut(localTrans);
				instTrans.transform(pt, pt);
				NodeInst newNi = NodeInst.makeInstance(np, pt, ni.getXSize(), ni.getYSize(), cell, orient, name, 0);
				if (newNi == null) continue;
				newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_NAME);
				newNi.copyStateBits(ni);
				newNi.copyVarsFrom(ni);

				// add ports to the new node's portmap
				for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst oldPi = pIt.next();
					PortInst newPi = newNi.findPortInstFromProto(oldPi.getPortProto());
					subPortMap.put(oldPi, newPi);
				}

				// add exports to the parent portmap
				for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					PortInst fromPi = topno.findPortInstFromProto(e);
					PortInst toPi = newNi.findPortInstFromProto(e.getOriginalPort().getPortProto());
					portMap.put(fromPi, toPi);
				}
			}
		}

		replaceExtractedArcs(subCell, newNodes, localTrans);
	}

	private static void replaceExtractedArcs(Cell cell, Map<NodeInst,Map<PortInst,PortInst>> nodeMaps,
		AffineTransform trans)
	{
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			PortInst oldHeadPi = ai.getHeadPortInst();
			NodeInst headNi = oldHeadPi.getNodeInst();
			Map<PortInst,PortInst> headMap = nodeMaps.get(headNi);
			PortInst newHeadPi = oldHeadPi;
			if (headMap != null)
			{
				newHeadPi = headMap.get(oldHeadPi);
				if (newHeadPi == null)
				{
					System.out.println("Warning: arc " + ai.describe(false) + " in cell " + cell.describe(false) +
						" is missing head connectivity information");
					continue;
				}
			}

			PortInst oldTailPi = ai.getTailPortInst();
			NodeInst tailNi = oldTailPi.getNodeInst();
			Map<PortInst,PortInst> tailMap = nodeMaps.get(tailNi);
			PortInst newTailPi = oldTailPi;
			if (tailMap != null)
			{
				newTailPi = tailMap.get(oldTailPi);
				if (newTailPi == null)
				{
					System.out.println("Warning: arc " + ai.describe(false) + " in cell " + cell.describe(false) +
						" is missing tail connectivity information");
					continue;
				}
			}

			if (newHeadPi == null || newTailPi == null)
			{
				System.out.println("Warning: cannot reconnect arc in cell " + cell.describe(false) +
					" from " + oldHeadPi + " to " + oldTailPi);
				continue;
			}
			Point2D headLoc = new Point2D.Double(ai.getHeadLocation().getX(), ai.getHeadLocation().getY());
			trans.transform(headLoc, headLoc);
			Point2D tailLoc = new Point2D.Double(ai.getTailLocation().getX(), ai.getTailLocation().getY());
			trans.transform(tailLoc, tailLoc);

			ArcProto ap = ai.getProto();
			double wid = ai.getLambdaBaseWidth();
			String name = null;
			if (ai.isUsernamed())
				name = ElectricObject.uniqueObjectName(ai.getName(), cell, ArcInst.class, false);

			ArcInst newAi = ArcInst.makeInstanceBase(ap, wid, newHeadPi, newTailPi, headLoc, tailLoc, name);
			if (newAi == null)
			{
				System.out.println("Error: arc " + ai.describe(false) + " in cell " + cell.describe(false) +
					" was not extracted");
				continue;
			}
			newAi.copyPropertiesFrom(ai);
		}
	}

//	private static HashMap<NodeInst,NodeInst> extractOneNode(NodeInst topno, boolean copyExports)
//	{
//		// make transformation matrix for this cell
//		Cell cell = topno.getParent();
//		Cell subCell = (Cell)topno.getProto();
//		AffineTransform localTrans = topno.translateOut();
//		localTrans.preConcatenate(topno.rotateOut());
//
//		// build a list of nodes to copy
//		HashMap<NodeInst,NodeInst> newNodes = new HashMap<NodeInst,NodeInst>();
//		for(Iterator<NodeInst> it = subCell.getNodes(); it.hasNext(); )
//			newNodes.put(it.next(), null);
//
//		// copy the nodes
//		for (Map.Entry<NodeInst,NodeInst> e: newNodes.entrySet())
//		{
//			NodeInst ni = e.getKey();
//			assert e.getValue() == null;
//
//			// do not extract "cell center" or "essential bounds" primitives
//			NodeProto np = ni.getProto();
//			if (np == Generic.tech.cellCenterNode || np == Generic.tech.essentialBoundsNode) continue;
//
//			Point2D pt = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
//			localTrans.transform(pt, pt);
//
//			String name = null;
//			if (ni.isUsernamed())
//				name = ElectricObject.uniqueObjectName(ni.getName(), cell, NodeInst.class, false);
//			Orientation orient = topno.getOrient().concatenate(ni.getOrient());
//			NodeInst newNi = NodeInst.makeInstance(np, pt, ni.getXSize(), ni.getYSize(), cell, orient, name, 0);
//			if (newNi == null) continue;
//
//			e.setValue(newNi);
//			newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_NAME);
//			newNi.copyStateBits(ni);
//			newNi.copyVarsFrom(ni);
//		}
//
//		// make a list of arcs to extract
//		List<ArcInst> arcs = new ArrayList<ArcInst>();
//		for(Iterator<ArcInst> it = subCell.getArcs(); it.hasNext(); )
//			arcs.add(it.next());
//
//		// extract the arcs
//		for(ArcInst ai : arcs)
//		{
//			// ignore arcs connected to nodes that didn't get yanked
//			NodeInst niTail = newNodes.get(ai.getTailPortInst().getNodeInst());
//			NodeInst niHead = newNodes.get(ai.getHeadPortInst().getNodeInst());
//			if (niTail == null || niHead == null) continue;
//			PortInst piTail = niTail.findPortInstFromProto(ai.getTailPortInst().getPortProto());
//			PortInst piHead = niHead.findPortInstFromProto(ai.getHeadPortInst().getPortProto());
//
//			Point2D ptTail = new Point2D.Double();
//			localTrans.transform(ai.getTailLocation(), ptTail);
//			Point2D ptHead = new Point2D.Double();
//			localTrans.transform(ai.getHeadLocation(), ptHead);
//
//			// make sure the head end fits in the port
//			Poly polyHead = piHead.getPoly();
//			if (!polyHead.isInside(ptHead))
//			{
//				ptHead.setLocation(polyHead.getCenterX(), polyHead.getCenterY());
//			}
//
//			// make sure the tail end fits in the port
//			Poly polyTail = piTail.getPoly();
//			if (!polyTail.isInside(ptTail))
//			{
//				ptTail.setLocation(polyTail.getCenterX(), polyTail.getCenterY());
//			}
//
//			String name = null;
//			if (ai.isUsernamed())
//				name = ElectricObject.uniqueObjectName(ai.getName(), cell, ArcInst.class, false);
//			ArcInst newAi = ArcInst.makeInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(), piHead, piTail, ptHead, ptTail, name);
//			if (newAi != null)
//				newAi.copyPropertiesFrom(ai);
//		}
//
//		// copy the exports if requested
//		if (copyExports)
//		{
//			// initialize for queueing creation of new exports
//			for(Iterator<Export> it = subCell.getExports(); it.hasNext(); )
//			{
//				Export pp = it.next();
//				NodeInst subNi = pp.getOriginalPort().getNodeInst();
//				NodeInst newNi = newNodes.get(subNi);
//				if (newNi == null) continue;
//				PortInst pi = newNi.findPortInstFromProto(pp.getOriginalPort().getPortProto());
//
//				// don't copy if the port is already exported
//				boolean alreadyDone = false;
//				for(Iterator<Export> eIt = newNi.getExports(); eIt.hasNext(); )
//				{
//					Export oPp = eIt.next();
//					if (oPp.getOriginalPort() == pi)
//					{
//						alreadyDone = true;
//						break;
//					}
//				}
//				if (alreadyDone) continue;
//
//				// copy the port
//				String portName = ElectricObject.uniqueObjectName(pp.getName(), cell, PortProto.class, false);
//				Export newPp = Export.newInstance(cell, pi, portName);
//				if (newPp != null)
//				{
//					newPp.setCharacteristic(pp.getCharacteristic());
//					newPp.copyTextDescriptorFrom(pp, Export.EXPORT_NAME);
//					newPp.copyVarsFrom(pp);
//				}
//			}
//		}
//
//		return newNodes;
//	}
//
//	private static void replaceArcsAndExports(Cell cell, HashMap<NodeInst,HashMap<NodeInst,NodeInst>> newNodes) {
//		// replace arcs to expanded subCells
//		ArrayList<ArcInst> arcsCopy = new ArrayList<ArcInst>();
//		for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
//			arcsCopy.add(it.next());
//
//		PortInst[] pis = new PortInst[2];
//		Point2D [] pts = new Point2D[2];
//		for(ArcInst ai : arcsCopy)
//		{
//			boolean needToCopy = false;
//			for (int i = 0; i < 2; i++) {
//				pts[i] = ai.getLocation(i);
//				PortInst pi = ai.getPortInst(i);
//				HashMap<NodeInst,NodeInst> newNodesForSubcell = newNodes.get(pi.getNodeInst());
//				if (newNodesForSubcell != null) {
//					Export pp = (Export)pi.getPortProto();
//					NodeInst subNi = pp.getOriginalPort().getNodeInst();
//					NodeInst newNi = newNodesForSubcell.get(subNi);
//					pi = newNi != null ? newNi.findPortInstFromProto(pp.getOriginalPort().getPortProto()) : null;
//					needToCopy = true;
//				}
//				pis[i] = pi;
//			}
//			if (!needToCopy) continue;
//
//			ArcProto ap = ai.getProto();
//			double wid = ai.getLambdaBaseWidth();
//			String name = null;
//			if (ai.isUsernamed())
//				name = ElectricObject.uniqueObjectName(ai.getName(), cell, ArcInst.class, false);
//
//			ai.kill();
//			if (pis[0] == null || pis[1] == null) continue;
//			ArcInst newAi = ArcInst.makeInstanceBase(ap, wid, pis[0], pis[1], pts[0], pts[1], name);
//			if (newAi != null)
//				newAi.copyPropertiesFrom(ai);
//		}
//
//		// replace the exports
//		ArrayList<Export> exportsCopy = new ArrayList<Export>();
//		for (Iterator<Export> it = cell.getExports(); it.hasNext(); )
//			exportsCopy.add(it.next());
//
//		for(Export pp : exportsCopy) {
//			PortInst oldPi = pp.getOriginalPort();
//			HashMap<NodeInst,NodeInst> newNodesForSubcell = newNodes.get(oldPi.getNodeInst());
//			if (newNodesForSubcell == null) continue;
//			Export subPp = (Export)oldPi.getPortProto();
//			NodeInst subNi = subPp.getOriginalPort().getNodeInst();
//			NodeInst newNi = newNodesForSubcell.get(subNi);
//			if (newNi == null) {
//				pp.kill();
//				continue;
//			}
//			PortInst newPi = newNi.findPortInstFromProto(subPp.getOriginalPort().getPortProto());
//			pp.move(newPi);
//		}
//
//		// delete the cell instance
//		cell.killNodes(newNodes.keySet());
//	}

	/****************************** MAKE A NEW VERSION OF A CELL ******************************/

	/**
	 * This class implement the command to make a new version of a cell.
	 */
	public static class NewCellVersion extends Job
	{
		private Cell cell;
		private Cell newVersion;

		public NewCellVersion(Cell cell)
		{
			super("Create new Version of " + cell, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			newVersion = cell.makeNewVersion();
			if (newVersion == null) return false;
			fieldVariableChanged("newVersion");
			return true;
		}

		public void terminateOK()
		{
			if (newVersion == null) return;

			// change the display of old versions to the new one
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				WindowContent content = wf.getContent();
				if (content == null) continue;
				if (content.getCell() == cell)
					content.setCell(newVersion, VarContext.globalContext, null);
			}

			EditWindow.repaintAll();
			System.out.println("Created new version: "+newVersion+", old version renamed to "+cell);
		}
	}

	/****************************** MAKE A COPY OF A CELL ******************************/

	/**
	 * This class implement the command to duplicate a cell.
	 */
	public static class DuplicateCell extends Job
	{
		private Cell cell;
		private String newName;
		private boolean entireGroup;
		private Cell dupCell;

		public DuplicateCell(Cell cell, String newName, boolean entireGroup)
		{
			super("Duplicate " + cell, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newName = newName;
			this.entireGroup = entireGroup;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Map<Cell,Cell> newCells = new HashMap<Cell,Cell>();
			String newCellName = newName + cell.getView().getAbbreviationExtension();
			dupCell = Cell.copyNodeProto(cell, cell.getLibrary(), newCellName, false);
			if (dupCell == null) {
				System.out.println("Could not duplicate "+cell);
				return false;
			}
			newCells.put(cell, dupCell);
			fieldVariableChanged("dupCell");

			System.out.println("Duplicated cell "+cell+".  New cell is "+dupCell+".");

			// examine all other cells in the group
			Cell.CellGroup group = cell.getCellGroup();
			View thisView = cell.getView();
			for(Iterator<Cell> it = group.getCells(); it.hasNext(); )
			{
				Cell otherCell = it.next();
				if (otherCell == cell) continue;
				// Only when copy an schematic, we should copy the icon if entireGroup == false
				if (!entireGroup && !(thisView == View.SCHEMATIC && otherCell.getView() == View.ICON)) continue;
				Cell copyCell = Cell.copyNodeProto(otherCell, otherCell.getLibrary(),
					newName + otherCell.getView().getAbbreviationExtension(), false);
				if (copyCell == null)
				{
					System.out.println("Could not duplicate cell "+otherCell);
					break;
				}
				newCells.put(otherCell, copyCell);
				System.out.println("  Also duplicated cell "+otherCell+".  New cell is "+copyCell+".");
			}

			// if icon of cell is present, replace old icon with new icon in new schematics cell
			for(Cell oldCell : newCells.keySet())
			{
				Cell newCell = newCells.get(oldCell);
				if (newCell.getView() != View.SCHEMATIC) continue;
				List<NodeInst> replaceThese = new ArrayList<NodeInst>();
				for (Iterator<NodeInst> it = newCell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					Cell replaceCell = newCells.get(ni.getProto());
					if (replaceCell != null) replaceThese.add(ni);
				}
				for(NodeInst ni : replaceThese)
				{
					// replace old icon(s) in duplicated cell
					Cell replaceCell = newCells.get(ni.getProto());
					ni.replace(replaceCell, true, true);
				}
			}
			return true;
		}

		public void terminateOK()
		{
			// change the display of old cell to the new one
			boolean found = false;
			WindowFrame curWf = WindowFrame.getCurrentWindowFrame();
			if (curWf != null)
			{
				WindowContent content = curWf.getContent();
				if (content != null && content.getCell() == cell)
				{
					content.setCell(dupCell, VarContext.globalContext, null);
					content.repaint();
					found = true;
				}
			}

			// current cell was not duplicated: see if any displayed cell is
			if (!found)
			{
				for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
				{
					WindowFrame wf = it.next();
					WindowContent content = wf.getContent();
					if (content == null) continue;
					if (content.getCell() == cell)
					{
						content.setCell(dupCell, VarContext.globalContext, null);
						content.repaint();
						break;
					}
				}
			}
		}
	}

	/****************************** COPY CELLS ******************************/

	/**
	 * Method to recursively copy cells between libraries.
	 * @param fromCells the original cells being copied.
	 * @param toLib the destination library to copy the cell.
	 * @param verbose true to display extra information.
	 * @param move true to move instead of copy.
	 * @param allRelatedViews true to copy all related views (schematic cell with layout, etc.)
	 * If false, only schematic/icon relations are copied.
	 * @param copySubCells true to recursively copy sub-cells.  If true, "useExisting" must be true.
	 * @param useExisting true to use any existing cells in the destination library
	 * instead of creating a cross-library reference.  False to copy everything needed.
	 * @return address of a copied cell (null on failure).
	 */
	public static IdMapper copyRecursively(List<Cell> fromCells, Library toLib, boolean verbose, boolean move,
		boolean allRelatedViews, boolean copySubCells, boolean useExisting)
	{
		IdMapper idMapper = new IdMapper();
		Cell.setAllowCircularLibraryDependences(true);
		try {
			HashSet<Cell> existing = new HashSet<Cell>();
			for(Cell fromCell : fromCells)
			{
				Cell copiedCell = copyRecursively(fromCell, toLib, verbose, move, "", true,
					allRelatedViews, allRelatedViews, copySubCells, useExisting, existing, idMapper);
				if (copiedCell == null) break;
			}
		} finally {
			Cell.setAllowCircularLibraryDependences(false);
		}
		return idMapper;
	}

	/**
	 * Method to recursively copy cells between libraries.
	 * @param fromCell the original cell being copied.
	 * @param toLib the destination library to copy the cell.
	 * @param verbose true to display extra information.
	 * @param move true to move instead of copy.
	 * @param subDescript a String describing the nature of this copy (empty string initially).
	 * @param schematicRelatedView true to copy a schematic related view.  Typically this is true,
	 * meaning that if copying an icon, also copy the schematic.  If already copying the example icon,
	 * this is set to false so that we don't get into a loop.
	 * @param allRelatedViews true to copy all related views (schematic cell with layout, etc.)
	 * If false, only schematic/icon relations are copied.
	 * @param allRelatedViewsThisLevel true to copy related views for this
	 * level of invocation only (but further recursion will use "allRelatedViews").
	 * @param copySubCells true to recursively copy sub-cells.  If true, "useExisting" must be true.
	 * @param useExisting true to use any existing cells in the destination library
	 * instead of creating a cross-library reference.  False to copy everything needed.
	 * @param existing a Set of Cells that have already been copied to the desitnation library
	 * and need not be copied again.
	 */
	private static Cell copyRecursively(Cell fromCell, Library toLib,
		boolean verbose, boolean move, String subDescript, boolean schematicRelatedView, boolean allRelatedViews,
		boolean allRelatedViewsThisLevel, boolean copySubCells, boolean useExisting, HashSet<Cell> existing, IdMapper idMapper)
	{
		// check for sensibility
		if (copySubCells && !useExisting)
			System.out.println("Cross-library copy warning: It makes no sense to copy subcells but not use them");

		// see if the cell is already there
		String toName = fromCell.getName();
		View toView = fromCell.getView();
		Cell copiedCell = inDestLib(fromCell, existing);
		if (copiedCell != null)
			return copiedCell;

		// copy subcells
		if (copySubCells || fromCell.isSchematic())
		{
			boolean found = true;
			while (found)
			{
				found = false;
				for(Iterator<NodeInst> it = fromCell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (!copySubCells && !ni.isIconOfParent()) continue;
					if (!ni.isCellInstance()) continue;
					Cell cell = (Cell)ni.getProto();

					// allow cross-library references to stay
					if (cell.getLibrary() == toLib) continue;

					// see if the cell is already there
					if (inDestLib(cell, existing) != null) continue;

					// do not copy subcell if it exists already (and was not copied by this operation)
					if (useExisting)
					{
						if (toLib.findNodeProto(cell.noLibDescribe()) != null) continue;
					}

					// copy subcell if not already there
					boolean doCopySchematicView = true;
					if (ni.isIconOfParent()) doCopySchematicView = false;
					Cell oNp = copyRecursively(cell, toLib, verbose,
						move, "subcell ", doCopySchematicView, allRelatedViews, allRelatedViewsThisLevel,
						copySubCells, useExisting, existing, idMapper);
					if (oNp == null)
					{
						if (move) System.out.println("Move of sub" + cell + " failed"); else
							System.out.println("Copy of sub" + cell + " failed");
						return null;
					}
					found = true;
					break;
				}
			}
		}

		// see if copying related views
		if (!allRelatedViewsThisLevel)
		{
			// not copying related views: just copy schematic if this was icon
			if (toView == View.ICON && schematicRelatedView /*&& move*/ )
			{
				// now copy the schematics
				boolean found = true;
				while (found)
				{
					found = false;
					for(Iterator<Cell> it = fromCell.getCellGroup().getCells(); it.hasNext(); )
					{
						Cell np = it.next();
						if (np.getView() != View.SCHEMATIC) continue;

						// see if the cell is already there
						if (inDestLib(np, existing) != null) continue;

						// copy equivalent view if not already there
						Cell oNp = copyRecursively(np, toLib, verbose,
							move, "schematic view ", true, allRelatedViews, false, copySubCells, useExisting, existing, idMapper);
						if (oNp == null)
						{
							if (move) System.out.println("Move of schematic view " + np + " failed"); else
								System.out.println("Copy of schematic view " + np + " failed");
							return null;
						}
						found = true;
						break;
					}
				}
			}
		} else
		{
			// first copy the icons
			boolean found = true;
			Cell fromCellWalk = fromCell;
			while (found)
			{
				found = false;
				for(Iterator<Cell> it = fromCellWalk.getCellGroup().getCells(); it.hasNext(); )
				{
					Cell np = it.next();
					if (!np.isIcon()) continue;

					// see if the cell is already there
					if (inDestLib(np, existing) != null) continue;

					// copy equivalent view if not already there
					Cell oNp = copyRecursively(np, toLib, verbose,
						move, "alternate view ", true, allRelatedViews, false, copySubCells, useExisting, existing, idMapper);
					if (oNp == null)
					{
						if (move) System.out.println("Move of alternate view " + np + " failed"); else
							System.out.println("Copy of alternate view " + np + " failed");
						return null;
					}
					found = true;
					break;
				}
			}

			// now copy the rest
			found = true;
			while (found)
			{
				found = false;
				for(Iterator<Cell> it = fromCellWalk.getCellGroup().getCells(); it.hasNext(); )
				{
					Cell np = it.next();
					if (np.isIcon()) continue;

					// see if the cell is already there
					if (inDestLib(np, existing) != null) continue;

					// copy equivalent view if not already there
					Cell oNp = copyRecursively(np, toLib, verbose,
						move, "alternate view ", true, allRelatedViews, false, copySubCells, useExisting, existing, idMapper);
					if (oNp == null)
					{
						if (move) System.out.println("Move of alternate view " + np + " failed"); else
							System.out.println("Copy of alternate view " + np + " failed");
						return null;
					}
					found = true;
					break;
				}
			}
		}

		// see if the cell is NOW there
		copiedCell = inDestLib(fromCell, existing);
		if (copiedCell != null)
			return copiedCell;

		// copy the cell
		String newName = toName;
		if (toView.getAbbreviation().length() > 0)
		{
			newName = toName + toView.getAbbreviationExtension();
		}
		Cell newFromCell;
//		if (move) {
//			fromCell.move(toLib);
//			if (useExisting)
//				fromCell.replaceSubcellsByExisting();
//			newFromCell = fromCell;
//		} else {
			newFromCell = Cell.copyNodeProto(fromCell, toLib, newName, useExisting);
			if (newFromCell == null) {
				System.out.println("Copy of " + subDescript + fromCell + " failed");
				return null;
			}
//		}

		// remember that this cell was copied
		existing.add(newFromCell);

		// Message before the delete!!
		if (verbose)
		{
			if (fromCell.getLibrary() != toLib)
			{
				String msg = "";
				if (move) msg += "Moved "; else
					 msg += "Copied ";
				msg += subDescript + fromCell.libDescribe() + " to " + toLib;
				System.out.println(msg);
			} else
			{
				System.out.println("Copied " + subDescript + newFromCell);
			}
		}

		// if moving, adjust pointers and kill original cell
		if (move)
		{
			// clear highlighting if the current node is being replaced
//			list = us_gethighlighted(WANTNODEINST, 0, 0);
//			for(i=0; list[i] != NOGEOM; i++)
//			{
//				if (!list[i]->entryisnode) continue;
//				ni = list[i]->entryaddr.ni;
//				if (ni->proto == fromCell) break;
//			}
//			if (list[i] != NOGEOM) us_clearhighlightcount();

			// now replace old instances with the moved one
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell np = cIt.next();
					boolean found = true;
					while (found)
					{
						found = false;
						for(Iterator<NodeInst> nIt = np.getNodes(); nIt.hasNext(); )
						{
							NodeInst ni = nIt.next();
							if (ni.getProto() == fromCell)
							{
								NodeInst replacedNi = ni.replace(newFromCell, false, false);
								if (replacedNi == null)
								{
									System.out.println("Error moving " + ni + " in " + np);
									found = false;
								}
								else
									found = true;
								break;
							}
						}
					}
				}
			}
			idMapper.moveCell(fromCell.backup(), newFromCell.getId());
//			if (deletedCells != null) deletedCells.add(fromCell);
			fromCell.kill();
		}
		return newFromCell;
	}

	/**
	 * Method to return a cell from a ser "existing" with similar name and same view "cell".
	 * @param cell a pattern cell
	 * @param existing a set where to find
	 * @return a cell from a set with proper name and view.
	 */
	private static Cell inDestLib(Cell cell, HashSet<Cell> existing)
	{
		for(Cell copiedCell : existing)
		{
			if (copiedCell.getName().equalsIgnoreCase(cell.getName()) && copiedCell.getView() == cell.getView())
				return copiedCell;
		}
		return null;
	}

}

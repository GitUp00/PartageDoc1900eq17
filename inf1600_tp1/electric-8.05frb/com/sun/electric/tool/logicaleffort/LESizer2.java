/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LESizer2.java
 * Written by: Jonathan Gainsley, Sun Microsystems.
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
 *
 * Created on November 11, 2003, 4:42 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LESizer sizes an LENetlist. The LENetlist is generated by LENetlister from
 * the Electric database, or perhaps read in from a Spice file(?)
 *
 * NOTE: the only 'Electric' objects used are in LENetlister,
 * any objects referenced in this file are from the logicaleffort
 * package, although their names may imply otherwise.  Their names
 * are as such because their names match PNP's naming scheme.
 *
 * @author  gainsley
 */
public class LESizer2 {

    /** which algorithm to use */                   private LESizer.Alg optimizationAlg;
    /** Where to direct output */                   private PrintStream out;
    /** What job we are part of */                  private Job job;
    /** Netlist */                                  private LENetlister2 netlist;
    /** error logger */                             private ErrorLogger errorLogger;

    /** Creates a new instance of LESizer */
    protected LESizer2(LESizer.Alg alg, LENetlister2 netlist, Job job, ErrorLogger errorLogger) {
        optimizationAlg = alg;
        this.netlist = netlist;
        this.job = job;
        this.errorLogger = errorLogger;

        out = new PrintStream(System.out);
    }


    // ============================ Sizing For Equal Gate Delays ==========================

    /**
     * Optimize using loop algorithm;
     * @param maxDeltaX maximum tolerance allowed in X
     * @param N maximum number of loops
     * @param verbose print out size information for each optimization loop
     * @return true if succeeded, false otherwise
     *
     * Optimization will stop when the difference in sizes (X) is
     * less than maxDeltaX, or when N iterations have occurred.
     */
    protected boolean optimizeLoops(float maxDeltaX, int N, boolean verbose,
                                    float alpha, float keeperRatio)
    {
        // iterate through all the instances, updating sizes

        float currentLoopDeltaX = maxDeltaX + 1;	// force at least one iteration
        float lastLoopDeltaX = currentLoopDeltaX;
        int divergingIters = 0;                     // count number if iterations sizing is diverging
        long startTime;
        int loopcount = 0;

        while ((currentLoopDeltaX > maxDeltaX) && (loopcount < N)) {

            // check for aborted state of job
            if (((LETool.AnalyzeCell)job).checkAbort(null)) return false;

            currentLoopDeltaX = 0;
            startTime = System.currentTimeMillis();
            System.out.print("  Iteration "+loopcount);
            if (verbose) System.out.println(":");

            // iterate through each instance
            for (Iterator<LENodable> lit = netlist.getSizeableNodables(); lit.hasNext(); ) {
                LENodable leno = lit.next();
                // ignore it if not a sizeable gate
                if (!leno.isLeGate()) continue;
                LENetwork outputNet = leno.outputNetwork;

                // find all drivers in same group, of same type (LEGATE or LEKEEPER)
                List<LENodable> drivers = new ArrayList<LENodable>();
                List<LENodable> arrayedDrivers = new ArrayList<LENodable>();
                for (LEPin pin : outputNet.getAllPins()) {
                    // only interested in drivers
                    if (pin.getDir() != LEPin.Dir.OUTPUT) continue;
                    LENodable loopLeno = pin.getInstance();
                    if (leno.getType() == loopLeno.getType()) {
                        if (leno.parallelGroup == loopLeno.parallelGroup) {
                            // add the instance. Note this adds the current instance at some point as well
                            drivers.add(loopLeno);
                            // error check
                            if (leno.parallelGroup > 0 && loopcount == 0 && leno.su != loopLeno.su) {
                                String msg = "\nError: LEGATE \""+leno.getName()+"\" drives in parallel with \""+loopLeno.getName()+
                                        "\" but has a different step-up";
                                System.out.println(msg);
                                NodeInst ni = leno.getNodable().getNodeInst();
                                if (ni != null) {
                                    errorLogger.logError(msg, ni, ni.getParent(), leno.context, 0);
                                }
                            }
                        }
                    }
                    if ((loopLeno.getNodable().getNodeInst() == leno.getNodable().getNodeInst()) &&
                        (loopLeno.context.getInstPath(".").equals(leno.context.getInstPath(".")))) {
                        // this must be an arrayed driver: not this also adds current instance at some point as well
                        arrayedDrivers.add(loopLeno);
                    }
                }

                // this will be the new size.
                float newX = 0;

                // if this is an LEKEEPER, we need to find smallest gate (or group)
                // that also drives this net, it is assumed that will have to overpower this keeper
                if (leno.getType() == LENodable.Type.LEKEEPER) {
                    Map<String,List<LENodable>> drivingGroups = new HashMap<String,List<LENodable>>();

                    float smallestX = 0;

                    // iterate over all drivers on net
                    for (LEPin pin : outputNet.getAllPins()) {
                        // only interested in drivers
                        if (pin.getDir() != LEPin.Dir.OUTPUT) continue;
                        LENodable loopLeno = pin.getInstance();
                        if (loopLeno.getType() == LENodable.Type.LEGATE || loopLeno.getType() == LENodable.Type.STATICGATE) {
                            // organize by groups
                            int i = loopLeno.parallelGroup;
                            Integer integer = new Integer(i);
                            if (i <= 0) {
                                // this gate drives independently, check size
                                if (smallestX == 0) smallestX = loopLeno.leX;
                                if (loopLeno.leX < smallestX) smallestX = loopLeno.leX;
                            }
                            // add to group to sum up drive strength later
                            List<LENodable> groupList = drivingGroups.get(integer.toString());
                            if (groupList == null) {
                                groupList = new ArrayList<LENodable>();
                                drivingGroups.put(integer.toString(), groupList);
                            }
                            groupList.add(loopLeno);
                        }
                    }

                    // find smallest total size of groups
                    Set<String> keys = drivingGroups.keySet();
                    for (String str : keys) {
                        List<LENodable> groupList = drivingGroups.get(str);
                        if (groupList == null) continue;            // skip empty groups
                        // get size
                        float sizeX = 0;
                        for (LENodable loopLeno : groupList) {
                            sizeX += loopLeno.leX;
                        }
                        // check size of group
                        if (smallestX == 0) smallestX = sizeX;
                        if (sizeX < smallestX) smallestX = sizeX;
                    }

                    // if no drivers found, issue warning
                    if (!keys.iterator().hasNext() && loopcount == 0) {
                        String msg = "\nError: LEKEEPER \""+leno.getName()+"\" does not fight against any drivers";
                        System.out.println(msg);
                        NodeInst ni = leno.getNodable().getNodeInst();
                        if (ni != null) {
                            errorLogger.logError(msg, ni, ni.getParent(), leno.context, 0);
                        }
                    }

                    // For now, split effort equally amongst all drivers
                    if (leno.parallelGroup <= 0) {
                        newX = smallestX * netlist.getKeeperRatio() / arrayedDrivers.size();
                    } else {
                        newX = smallestX * netlist.getKeeperRatio() / drivers.size();
                    }
                }

                // If this is an LEGATE, simply sum all capacitances on the Net
                if (leno.getType() == LENodable.Type.LEGATE) {

                    // compute total le*X (totalcap)
                    float totalcap = 0;
                    int numLoads = 0;
                    //System.out.println("LENode "+leno.getName()+" drives: ");
                    //outputNet.print();
                    for (LEPin pin : outputNet.getAllPins()) {
                        LENodable loopLeno = pin.getInstance();

                        float load = loopLeno.leX * pin.getLE() * loopLeno.getMfactor();
                        if (pin.getDir() == LEPin.Dir.OUTPUT) load *= alpha;
                        totalcap += load;
                        // check to see if gate is only driving itself
                        if (loopLeno != leno)
                            numLoads++;
                    }

                    // create error if no loads only on first iteration
                    if (numLoads == 0 && loopcount == 0) {
                        String msg = "\nError: LEGATE \""+leno.getName()+"\" has no loads: will be ignored";
                        System.out.println(msg);
                        NodeInst ni = leno.getNodable().getNodeInst();
                        if (ni != null) {
                            errorLogger.logError(msg, ni, ni.getParent(), leno.context, 1);
                        }
                    }
                    // ignore if no loads, on all iterations
                    if (numLoads == 0)
                        continue;

                    // For now, split effort equally amongst all drivers
                    // Group 0 drives individually
                    if (leno.parallelGroup <= 0)
                        newX = totalcap / leno.su / arrayedDrivers.size();
                    else {
                        newX = totalcap / leno.su / drivers.size();
                    }
                    // also take into account mfactor of driver
                    newX = newX / (float)leno.getMfactor();
                }

                // determine change in size
                float currentX = leno.leX;
                float deltaX;
                if (currentX == 0 && newX == 0) {
                    // if before and after are 0, delta is 0
                    deltaX = 0f;
                } else {
                    // account for divide by 0
                    if (currentX == 0) currentX = 0.001f;
                    deltaX = Math.abs( (newX-currentX)/currentX);
                }
                currentLoopDeltaX = (deltaX > currentLoopDeltaX) ? deltaX : currentLoopDeltaX;

                if (verbose) {
                    out.println("Optimized "+leno.getName()+": size:  "+
                            TextUtils.formatDouble(leno.leX, 3)+
                            "x ==> "+TextUtils.formatDouble(newX, 3)+"x");
                }
                leno.leX = newX;
            }



            // All done, print some statistics about this iteration
            String elapsed = TextUtils.getElapsedTime(System.currentTimeMillis()-startTime);
            System.out.println("  ...done ("+elapsed+"), delta: "+currentLoopDeltaX);            
            if (verbose) System.out.println("-----------------------------------");
            loopcount++;

            // check to see if we're diverging or not converging
            if (currentLoopDeltaX >= lastLoopDeltaX) {
                if (divergingIters > 2) {
                    System.out.println("  Sizing diverging, aborting");
                    return false;
                }
                divergingIters++;
            }
            lastLoopDeltaX = currentLoopDeltaX;

        } // while (currentLoopDeltaX ... )

        return true;
    }



    // ========================== Sizing for Path Optimization =====================


    // =============================== Statistics ==================================


    // ============================== Design Printing ===============================


}

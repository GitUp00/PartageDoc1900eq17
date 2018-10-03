/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserCom.java
 * Asynchronous Logic Simulator user functions
 * Original C Code written by Brent Serbin and Peter J. Gallant
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.als;

import com.sun.electric.tool.simulation.Stimuli;

import java.io.*;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Scanner;

/**
 * Class to hold user-defined functions for the ALS Simulator.
 */
public class UserCom
{
	/**
	 * Class to model a bidirectional p-channel transistor.  If the
	 * routine is called, a new node summing calculation is performed and the effects
	 * of the gate output on the node are ignored.  This algorithm is used in the
	 * XEROX Aquarius simulation engine.
	 *
	 * Calling Arguments:
	 *	primHead = pointer to a structure containing the calling arguments for
	 *		 the user defined function.  The nodes are stored in the list
	 *		 in the following order: ctl, sidea, sideb
	 */
	static class PMOSTran extends ALS.UserProc
	{
		PMOSTran(ALS als) { nameMe(als, "PMOStran"); }

		void simulate(ALS.Model primHead)
		{
			ALS.ALSExport ctl = primHead.exList.get(0);
			ALS.ALSExport [] side = new ALS.ALSExport[2];
			side[0] = primHead.exList.get(1);
			side[1] = primHead.exList.get(2);

			if (ctl.nodePtr.sumState == Stimuli.LOGIC_HIGH)
			{
				scheduleNodeUpdate(primHead, side[0], '=',
					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
				scheduleNodeUpdate(primHead, side[1], '=',
					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
				return;
			}
			calculateBidirOutputs(primHead, side, Stimuli.GATE_STRENGTH);
		}
	}

	static class PMOSTranWeak extends ALS.UserProc
	{
		PMOSTranWeak(ALS als) { nameMe(als, "pMOStranWeak"); }

		void simulate(ALS.Model primHead)
		{
			ALS.ALSExport ctl = primHead.exList.get(0);
			ALS.ALSExport [] side = new ALS.ALSExport[2];
			side[0] = primHead.exList.get(1);
			side[1] = primHead.exList.get(2);

			if (ctl.nodePtr.sumState == Stimuli.LOGIC_HIGH)
			{
				scheduleNodeUpdate(primHead, side[0], '=',
					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
				scheduleNodeUpdate(primHead, side[1], '=',
					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
				return;
			}
			calculateBidirOutputs(primHead, side, Stimuli.NODE_STRENGTH);
		}
	}

	/**
	 * Class to model a bidirectional n-channel transistor.  If the
	 * routine is called, a new node summing calculation is performed and the effects
	 * of the gate output on the node are ignored.  This algorithm is used in the
	 * XEROX Aquarius simulation engine.
	 *
	 * Calling Arguments:
	 *	primHead = pointer to a structure containing the calling arguments for
	 *		 the user defined function.  The nodes are stored in the list
	 *		 in the following order: ctl, sidea, sideb
	 */
	static class NMOSTran extends ALS.UserProc
	{
		NMOSTran(ALS als) { nameMe(als, "nMOStran"); }

		void simulate(ALS.Model primHead)
		{
			ALS.ALSExport ctl = primHead.exList.get(0);
			ALS.ALSExport [] side = new ALS.ALSExport[2];
			side[0] = primHead.exList.get(1);
			side[1] = primHead.exList.get(2);

			if (ctl.nodePtr.sumState == Stimuli.LOGIC_LOW)
			{
				scheduleNodeUpdate(primHead, side[0], '=',
					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
				scheduleNodeUpdate(primHead, side[1], '=',
					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
				return;
			}
			calculateBidirOutputs(primHead, side, Stimuli.GATE_STRENGTH);
		}
	}

	static class NMOSTranWeak extends ALS.UserProc
	{
		NMOSTranWeak(ALS als) { nameMe(als, "nMOStranWeak"); }

		void simulate(ALS.Model primHead)
		{
			ALS.ALSExport ctl = primHead.exList.get(0);
			ALS.ALSExport [] side = new ALS.ALSExport[2];
			side[0] = primHead.exList.get(1);
			side[1] = primHead.exList.get(2);

			if (ctl.nodePtr.sumState == Stimuli.LOGIC_LOW)
			{
				scheduleNodeUpdate(primHead, side[0], '=',
					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
				scheduleNodeUpdate(primHead, side[1], '=',
					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
				return;
			}
			calculateBidirOutputs(primHead, side, Stimuli.NODE_STRENGTH);
		}
	}

	/**
	 * Class to fake out the function of a JK flipflop.
	 *
	 * Calling Arguments:
	 *	primHead = pointer to a structure containing the calling arguments for
	 *		 the user defined function.  The nodes are stored in the list
	 *		 in the following order: ck, j, k, q, qbar
	 */
	static class JKFlop extends ALS.UserProc
	{
		JKFlop(ALS als) { nameMe(als, "JKFFLOP"); }

		void simulate(ALS.Model primHead)
		{
			ALS.ALSExport argPtr = primHead.exList.get(0);
			int ck = argPtr.nodePtr.sumState;
			if (ck != Stimuli.LOGIC_LOW) return;

			argPtr = primHead.exList.get(1);
			int j = argPtr.nodePtr.sumState;
			argPtr = primHead.exList.get(2);
			int k = argPtr.nodePtr.sumState;
			argPtr = primHead.exList.get(3);
			ALS.ALSExport argPtrBar = primHead.exList.get(4);

			if (j == Stimuli.LOGIC_LOW)
			{
				if (k == Stimuli.LOGIC_LOW) return;
				scheduleNodeUpdate(primHead, argPtr, '=',
					new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs);
				scheduleNodeUpdate(primHead, argPtrBar, '=',
					new Integer(Stimuli.LOGIC_HIGH), Stimuli.GATE_STRENGTH, als.timeAbs);
				return;
			}
			if (k == Stimuli.LOGIC_LOW)
			{
				scheduleNodeUpdate(primHead, argPtr, '=',
					new Integer(Stimuli.LOGIC_HIGH), Stimuli.GATE_STRENGTH, als.timeAbs);
				scheduleNodeUpdate(primHead, argPtrBar, '=',
					new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs);
				return;
			}

			int out = argPtr.nodePtr.sumState;
			if (out == Stimuli.LOGIC_HIGH)
			{
				scheduleNodeUpdate(primHead, argPtr, '=',
					new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs);
				scheduleNodeUpdate(primHead, argPtrBar, '=',
					new Integer(Stimuli.LOGIC_HIGH), Stimuli.GATE_STRENGTH, als.timeAbs);
			} else
			{
				scheduleNodeUpdate(primHead, argPtr, '=',
					new Integer(Stimuli.LOGIC_HIGH), Stimuli.GATE_STRENGTH, als.timeAbs);
				scheduleNodeUpdate(primHead, argPtrBar, '=',
					new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs);
			}
		}
	}

	/**
	 * Class to mimic the function of a D flip flop.
	 *
	 * Arguments:
	 *	primHead = pointer to a structure containing the
	 *		 calling arguments for the user function.
	 *		The nodes are expected to appear in the
	 *		 parameter list in the following order:
	 *
	 *		 dataIn, clk, q
	 */
	static class DFFlop extends ALS.UserProc
	{
		DFFlop(ALS als) { nameMe(als, "DFFLOP"); }

		void simulate(ALS.Model primHead)
		{
			ALS.ALSExport argPtr = primHead.exList.get(0);

			// dataIn signal now selected
			int dIn = argPtr.nodePtr.sumState;
			argPtr = primHead.exList.get(1);

			// clk signal now selected
			int clk = argPtr.nodePtr.sumState;

			// do nothing if not a +ve clock edge
			if (clk != Stimuli.LOGIC_LOW) return;

			/* If this part of the procedure has been reached, the
				dataOut signal should be updated since clk is high.
				Therefore, the value of dataOut should be set to
				the value of dIn (one of LOGIC_LOW, LOGIC_HIGH,
				or LOGIC_X). */

			// select dataOut signal
			argPtr = primHead.exList.get(2);
			int q = dIn;
			scheduleNodeUpdate(primHead, argPtr, '=',
				new Integer(q), Stimuli.GATE_STRENGTH, als.timeAbs);
		}
	}

	/**
	 * Class to convert the value of 8 input bits into a state
	 * representation in the range 0x00-0xff.  This function can be called for
	 * the compact representation of the state of a bus structure in hexadecimal
	 * format.
	 *
	 * Calling Arguments:
	 *	primHead = pointer to a structure containing the calling arguments for
	 *		 the user defined function.  The nodes are ordered b7, b6, b5,
	 *		 b4, b3, b2, b1, b0, output in this list.
	 */
	static class BusToState extends ALS.UserProc
	{
		BusToState(ALS als) { nameMe(als, "BUS_TO_STATE"); }

		void simulate(ALS.Model primHead)
		{
			Iterator<ALS.ALSExport> it = primHead.exList.iterator();
			ALS.ALSExport argPtr = it.next();
			int state = 0;
			for (int i = 7; i > -1; --i)
			{
				int bit = argPtr.nodePtr.sumState;
				if (bit == Stimuli.LOGIC_HIGH) state += (0x01 << i);
				argPtr = it.next();
			}
			scheduleNodeUpdate(primHead, argPtr, '=',
				new Integer(state), Stimuli.VDD_STRENGTH, als.timeAbs);
		}
	}

	/**
	 * Class to convert a value in the range of 0x00-0xff into an 8 bit
	 * logic representation (logic L = -1 and H = -3).  This function can be called to
	 * provide an easy means of representing bus values with a single integer entry.
	 *
	 * Calling Arguments:
	 *	primHead = pointer to a structure containing the calling arguments for
	 *		 the user defined function.  The nodes are stored in the list
	 *		 in the following order: input, b7, b6, b5, b4, b3, b2, b1, b0
	 */
	static class StateToBus extends ALS.UserProc
	{
		StateToBus(ALS als) { nameMe(als, "STATE_TO_BUS"); }

		void simulate(ALS.Model primHead)
		{
			Iterator<ALS.ALSExport> it = primHead.exList.iterator();
			ALS.ALSExport argPtr = it.next();
			int input = argPtr.nodePtr.sumState;

			for (int i = 7; i > -1; --i)
			{
				argPtr = it.next();

				int mask = (0x01 << i);
				if ((input & mask) != 0)
				{
					scheduleNodeUpdate(primHead, argPtr, '=',
						new Integer(Stimuli.LOGIC_HIGH), Stimuli.VDD_STRENGTH, als.timeAbs);
				} else
				{
					scheduleNodeUpdate(primHead, argPtr, '=',
						new Integer(Stimuli.LOGIC_LOW), Stimuli.VDD_STRENGTH, als.timeAbs);
				}
			}
		}
	}

	/************************** ROUTINES FOR CPU ARCHITECTURE SIMULATION **************************/
        
	/**
	 * Class for a RAM system accessed in 32bit, addressed in bytes.
         * The data bus is tristate.
	 *
         * ALS function names are: RAM, RAM0, RAM1 ... RAM9  (so different RAMs can have different parameters)
	 * ALS function arguments: addr31..addr0, cs, rw , data31..data0, debug_dump, reset
         *  cs = chip select is 1 when the memory access has to be done
         *  rw = 1 for writing, rw = 0 for reading
         *  debug_dump = 1 will dump memory content to the file specified as second "param" (use "\n" between input filename and output filname)
         *  reset = 1 rereads the file from disk
         * Uses the "t: delay" as delay between control change and output change.
         * Uses "param" for specifying input filename for initial RAM content
	 */
        static class RAM_
	{
		byte[] mem;
		int last_cs;
                boolean isZ;
                double last_time;
                long last_addr;
	}
	static class RAM extends ALS.UserProc
	{
                static final int cs_i = 32;
                static final int rw_i = 33;
                static final int debug_dump_i = 66;
                static final int reset_i = 67;

		RAM(ALS als) {
                    nameMe(als, "RAM");
                    for(int i=0;i<10;++i)
                        nameMe(als, "RAM"+Integer.toString(i));
                }
                private String hex(int val, int length)
                {
                    String s = "0000000"+Long.toString((val<0)?val+0x8000000000000000l:val,16);
                    return s.substring(s.length()-length);
                }

		void simulate(ALS.Model primHead)
		{
			ALS.Func funcHead = (ALS.Func) primHead.ptr;
                        RAM_ ram_ = (RAM_)funcHead.userPtr;
                        if(ram_ == null)
                        {
                            funcHead.userPtr = ram_ = new RAM_();
                            ram_.last_time = Double.MAX_VALUE;
                        }
                        if(als.timeAbs < ram_.last_time) // if going back in time, we reset last values (this happens when simulator restarts)
                        {
                            ram_.last_cs = Stimuli.LOGIC_LOW;
                            ram_.isZ = true;
                        }
                        ram_.last_time = als.timeAbs;
			byte[] mem = ram_.mem;

                        // -------- Read RAM content from a file --------
			if (mem == null || primHead.exList.get(reset_i).nodePtr.sumState == Stimuli.LOGIC_HIGH) // is reset high?
			{
                                if(mem == null) ram_.mem = mem = new byte[65536];
                                if(funcHead.param != null) // we have a parameter, use it as a filename and read it in memory
                                {
                                    String[] param = funcHead.param.split("\n"); // first param is input file, second is output file
                                    File file = new File(param[0]);
                                    if(file == null)
                                    {
                                        System.out.println("WARNING: Unable to open file '" + param[0] + "' specified for RAM content");
                                    } else 
                                    {
                                        try {
                                        Scanner lines = new Scanner(file);
                                        while(lines.hasNextLine())
                                        {
                                            String line = lines.nextLine().replace('\t',' '); // tabs to space
                                            Scanner tokens = new Scanner(line);
                                            tokens.useDelimiter(" ");
                                            if(tokens.hasNext())
                                            {
                                                String token = tokens.next();
                                                if(token.startsWith("#")) // comment
                                                    continue;
                                                int addr = Integer.parseInt(token,16);
                                                if(addr>=0) while(tokens.hasNext() && addr<mem.length)
                                                {
                                                    token = tokens.next();
                                                    if(token==null || token.equals("")) continue;
                                                    if(token.startsWith("#")) break; // comment
                                                    mem[addr] = (byte)Integer.parseInt(token,16);
                                                    ++addr;
                                                }
                                            }
                                        }
                                        lines.close();
                                        }
                                        catch (IOException ex){
                                            System.out.println("WARNING: Unable to read file '" + param[0] + "' specified for RAM content; "+ex.getMessage());
                                        }

                                    }
                                }
			}

                        // -------- Write RAM content to a file --------
                        if(primHead.exList.get(debug_dump_i).nodePtr.sumState == Stimuli.LOGIC_HIGH) // is debug_dump high?
                        {
                                if(funcHead.param != null) // we have a parameter, use it as a filename and read it in memory
                                {
                                    String[] param = funcHead.param.split("\n"); // first param is input file, second is output file
                                    if(param.length>=2 && param[1]!=null)
                                    {
                                        try {
                                            BufferedWriter fout = new BufferedWriter(new FileWriter(param[1]));
                                            int usedMem;
                                            for(usedMem=65535;usedMem>=0 && mem[usedMem]==0;--usedMem); //find the last non zero byte
                                            ++usedMem;
                                            for(int i=0;i<usedMem;i++)
                                            {
                                                if((i&0xF) == 0)
                                                {
                                                    fout.newLine();
                                                    fout.write(hex(i,4)+" ");
                                                }
                                                fout.write(" "+hex(mem[i],2));
                                            }
                                            fout.newLine();
                                            fout.close();
                                        } catch (IOException ex){//Catch exception if any
                                            System.out.println("Error writing RAM to file: " + ex.getMessage());
                                        }
                                    }
                                }
                        }

                        // -------- Get address and control signals --------
			Iterator<ALS.ALSExport> it = primHead.exList.iterator();
                        // iterator is on addr31
                        long addr = 0;
                        long data = 0;
                        for (int i=32; i>0; --i)
                                addr = (addr<<1)|((it.next().nodePtr.sumState == Stimuli.LOGIC_HIGH)?1:0);
                        addr &= 0xFFFF; // we use only the 16 lsb of the address
                        int cs = it.next().nodePtr.sumState; // get cs
                        int rw = it.next().nodePtr.sumState; // get rw
                        // iterator is now on data31
                        
                        // -------- RAM not driving bus:  writing to RAM or idle --------
                        if(cs != Stimuli.LOGIC_HIGH || rw == Stimuli.LOGIC_HIGH) // the bus is not driven by the RAM (not selected, or writing to RAM)
                        {
                                if(cs != Stimuli.LOGIC_HIGH && ram_.isZ) // if chip not selected and bus already at Z, nothing to do
                                    return;
                                for (int i=32; i>0; --i)
                                {
                                    ALS.ALSExport argPtr = it.next();
                                    if(!ram_.isZ)
                                        scheduleNodeUpdate(primHead, argPtr, '=',
                                            new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs); // tristate the data bus
                                    data = (data<<1)|((argPtr.nodePtr.sumState == Stimuli.LOGIC_HIGH)?1:0); // read bits from data bus
                                }
                                if(cs == Stimuli.LOGIC_HIGH) // we are writing to the RAM
                                {
                                    for(int i=0; i<4 && addr<65536; ++addr, ++i)
                                    {
                                        mem[(int)addr] = (byte)(data&0xFF);
                                        data >>= 8;
                                    }
                                }
                                ram_.isZ = true;
                                ram_.last_cs = cs;
                                return;
                        }

                        // -------- Reading from RAM --------
                        // here "cs" is high, and rw is not high, so we read memory...
                        if(!ram_.isZ && addr==ram_.last_addr) // if bus is not Z and address has not changed, then the bus is already in the right state
                            return;
                        ram_.last_cs = cs;
                        ram_.last_addr = addr;
                        ram_.isZ = false;
                        for(int i=0; i<32 && addr<65536; addr++,i+=8)
                            data |= (mem[(int)addr]&0xFF)<<i;
                        double time = als.timeAbs + funcHead.delta /* * targetNode.load */;
                        for (int i=31; i>=0; --i)
                        {   // put all bits on the data bus
                            int bit = (((1<<i)&data)!=0)?Stimuli.LOGIC_HIGH:Stimuli.LOGIC_LOW;
                            scheduleNodeUpdate(primHead, it.next(), '=',
                                    new Integer(bit), Stimuli.GATE_STRENGTH, time); // put data on the bus
                        }
                }
	}
        
	/************************** UNUSED USER FUNCTION ROUTINES **************************/

//	/**
//	 * Class fakes out the function of a synchronous reset counter.
//	 *
//	 * Calling Arguments:
//	 *	primHead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are stored in the list
//	 *		 in the following order: ck, reset, count, out
//	 */
//	static class Counter extends ALS.UserProc
//	{
//		Counter(ALS als) { nameMe(als, "COUNTER"); }
//
//		void simulate(ALS.Model primHead)
//		{
//			ALS.ALSExport argPtr = primHead.exList.get(0);
//			int ck = argPtr.nodePtr.sumState;
//			if (ck != Stimuli.LOGIC_HIGH) return;
//
//			argPtr = primHead.exList.get(1);
//			int reset = argPtr.nodePtr.sumState;
//			argPtr = primHead.exList.get(2);
//			int count = argPtr.nodePtr.sumState;
//			ALS.ALSExport countPtr = argPtr;
//			argPtr = primHead.exList.get(3);
//			int out = argPtr.nodePtr.sumState;
//
//			if (reset == Stimuli.LOGIC_LOW)
//			{
//				scheduleNodeUpdate(primHead, countPtr, '=',
//					new Integer(0), Stimuli.GATE_STRENGTH, als.timeAbs + 30e-9);
//				scheduleNodeUpdate(primHead, argPtr, '=',
//					new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs + 30e-9);
//				return;
//			}
//
//			count = (count + 1) % 16;
//			scheduleNodeUpdate(primHead, countPtr, '=',
//				new Integer(count), Stimuli.GATE_STRENGTH, als.timeAbs + 20e-9);
//
//			if (count == 15)
//			{
//				scheduleNodeUpdate(primHead, argPtr, '=',
//					new Integer(Stimuli.LOGIC_HIGH), Stimuli.GATE_STRENGTH, als.timeAbs + 18e-9);
//			} else
//			{
//				scheduleNodeUpdate(primHead, argPtr, '=',
//					new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs + 22e-9);
//			}
//		}
//	}
//
//	/**
//	 * Class is an event driven function which is called to calculate
//	 * the binary exponential backoff time delay for an Ethernet system.
//	 *
//	 * Calling Arguments:
//	 *	primHead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are stored in the list
//	 *		 in the following order: retX, server
//	 */
//	static class DelayCalc extends ALS.UserProc
//	{
//		DelayCalc(ALS als) { nameMe(als, "DELAY_CALC"); }
//
//		void simulate(ALS.Model primHead)
//		{
//			double baseDelay = 10e-6;
//
//			ALS.ALSExport retX = primHead.exList.get(0);
//			ALS.ALSExport server = primHead.exList.get(1);
//
//			if (retX.nodePtr.sumState == 0) return;
//
//			double delay = 2.0 * Math.random() * baseDelay *
//				(1 << (retX.nodePtr.sumState - 1)) + als.timeAbs;
//			scheduleNodeUpdate(primHead, server, '=',
//				new Integer(1), Stimuli.VDD_STRENGTH, delay);
//			scheduleNodeUpdate(primHead, server, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, delay);
//		}
//	}
//
//	/**
//	 * Class simulates the operation of a FIFO element in a data
//	 * communication system.  Data is stored in a block of memory of fixed
//	 * size.  Read and write counters are used to index the memory array.  This
//	 * approach was chosen because this structure will simulate much faster than a
//	 * linked list.
//	 *
//	 * Calling Arguments:
//	 *	primHead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are stored in the list
//	 *		 in the following order: input, output, size
//	 */
//	private static final int FIFO_SIZE = 2048;
//
//	static class FIFOMem
//	{
//		int rPtr;
//		int wPtr;
//		int [] state;
//	}
//
//	static class FIFO extends ALS.UserProc
//	{
//		FIFO(ALS als) { nameMe(als, "FIFO"); }
//
//		void simulate(ALS.Model primHead)
//		{
//			ALS.ALSExport argPtr = primHead.exList.get(0);
//			ALS.ALSExport inputPtr = argPtr;
//			argPtr = primHead.exList.get(1);
//			ALS.ALSExport outputPtr = argPtr;
//			argPtr = primHead.exList.get(2);
//			ALS.ALSExport sizePtr = argPtr;
//
//			ALS.Func funcHead = (ALS.Func) primHead.ptr;
//			FIFOMem fifoHead = (FIFOMem) funcHead.userPtr;
//			if (fifoHead == null)
//			{
//				fifoHead = new FIFOMem();
//				fifoHead.rPtr = 0;
//				fifoHead.wPtr = 0;
//				fifoHead.state = new int[FIFO_SIZE];
//				funcHead.userPtr = fifoHead;
//			}
//
//			if (inputPtr.nodePtr.sumState > 0)
//			{
//				scheduleNodeUpdate(primHead, inputPtr, '=',
//					new Integer(0), Stimuli.VDD_STRENGTH, als.timeAbs);
//				scheduleNodeUpdate(primHead, inputPtr, '=',
//					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
//				if (sizePtr.nodePtr.sumState >= FIFO_SIZE)
//				{
//					System.out.println("Data loss has occured: Value = " + inputPtr.nodePtr.sumState + ", Time = " + als.timeAbs);
//					return;
//				}
//				int newSize = sizePtr.nodePtr.sumState + 1;
//				scheduleNodeUpdate(primHead, sizePtr, '=',
//					new Integer(newSize), Stimuli.VDD_STRENGTH, als.timeAbs);
//				scheduleNodeUpdate(primHead, sizePtr, '=',
//					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
//				fifoHead.state[fifoHead.wPtr] = inputPtr.nodePtr.sumState;
//				fifoHead.wPtr = ((fifoHead.wPtr) + 1) % FIFO_SIZE;
//			}
//
//			if (outputPtr.nodePtr.sumState == 0 && sizePtr.nodePtr.sumState != 0)
//			{
//				scheduleNodeUpdate(primHead, outputPtr, '=',
//					new Integer(fifoHead.state[fifoHead.rPtr]), Stimuli.VDD_STRENGTH, als.timeAbs);
//				scheduleNodeUpdate(primHead, outputPtr, '=',
//					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
//				int newSize = sizePtr.nodePtr.sumState - 1;
//				scheduleNodeUpdate(primHead, sizePtr, '=',
//					new Integer(newSize), Stimuli.VDD_STRENGTH, als.timeAbs);
//				scheduleNodeUpdate(primHead, sizePtr, '=',
//					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
//				fifoHead.rPtr = ((fifoHead.rPtr) + 1) % FIFO_SIZE;
//			}
//		}
//	}
//
//	/**
//	 * Class examines the bus to see if a message is destined for it.
//	 * If so, the data value is examined to determine if the link that the data byte
//	 * passed across is in a "congested" state.  If so this element must fire an
//	 * XOFF character to the station on the remote side of the connection.  The
//	 * data value is also passed to a gate which will examine the byte and increment
//	 * the appropriate counter.
//	 *
//	 * Calling Arguments:
//	 *	primHead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are stored in the list
//	 *		 in the following order: address, addIn, dataIn, dataType,
//	 *					 remXOff, addOut, dataOut
//	 */
//	static class RXData extends ALS.UserProc
//	{
//		RXData(ALS als) { nameMe(als, "RX_DATA"); }
//
//		void simulate(ALS.Model primHead)
//		{
//			ALS.ALSExport address = primHead.exList.get(0);
//			ALS.ALSExport addIn = primHead.exList.get(1);
//			if (address.nodePtr.sumState != addIn.nodePtr.sumState) return;
//			ALS.ALSExport dataIn = primHead.exList.get(2);
//			ALS.ALSExport dataType = primHead.exList.get(3);
//			ALS.ALSExport remXOff = primHead.exList.get(4);
//			ALS.ALSExport addOut = primHead.exList.get(5);
//			ALS.ALSExport dataOut = primHead.exList.get(6);
//
//			scheduleNodeUpdate(primHead, addIn, '=',
//				new Integer(0), Stimuli.VDD_STRENGTH, als.timeAbs);
//			scheduleNodeUpdate(primHead, addIn, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
//			scheduleNodeUpdate(primHead, dataIn, '=',
//				new Integer(0), Stimuli.VDD_STRENGTH, als.timeAbs);
//			scheduleNodeUpdate(primHead, dataIn, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
//			if (((dataIn.nodePtr.sumState) % 2) != 0)
//			{
//				scheduleNodeUpdate(primHead, dataType, '=',
//					new Integer(dataIn.nodePtr.sumState), Stimuli.VDD_STRENGTH, als.timeAbs);
//				scheduleNodeUpdate(primHead, dataType, '=',
//					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
//				if (remXOff.nodePtr.sumState != 0)
//				{
//					scheduleNodeUpdate(primHead, remXOff, '=',
//						new Integer(0), Stimuli.VDD_STRENGTH, als.timeAbs);
//					scheduleNodeUpdate(primHead, remXOff, '=',
//						new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
//					scheduleNodeUpdate(primHead, addOut, '=',
//						new Integer(address.nodePtr.sumState), Stimuli.VDD_STRENGTH, als.timeAbs + 50e-6);
//					scheduleNodeUpdate(primHead, addOut, '=',
//						new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs + 50e-6);
//					scheduleNodeUpdate(primHead, dataOut, '=',
//						new Integer(5), Stimuli.VDD_STRENGTH, als.timeAbs + 50e-6);
//					scheduleNodeUpdate(primHead, dataOut, '=',
//						new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs + 50e-6);
//				}
//			} else
//			{
//				scheduleNodeUpdate(primHead, dataType, '=',
//					new Integer(((dataIn.nodePtr.sumState) - 1)), Stimuli.VDD_STRENGTH, als.timeAbs);
//				scheduleNodeUpdate(primHead, dataType, '=',
//					new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
//				if (remXOff.nodePtr.sumState == 0)
//				{
//					scheduleNodeUpdate(primHead, remXOff, '=',
//						new Integer(1), Stimuli.VDD_STRENGTH, als.timeAbs);
//					scheduleNodeUpdate(primHead, remXOff, '=',
//						new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs);
//					scheduleNodeUpdate(primHead, addOut, '=',
//						new Integer(address.nodePtr.sumState), Stimuli.VDD_STRENGTH, als.timeAbs + 50e-6);
//					scheduleNodeUpdate(primHead, addOut, '=',
//						new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs + 50e-6);
//					scheduleNodeUpdate(primHead, dataOut, '=',
//						new Integer(7), Stimuli.VDD_STRENGTH, als.timeAbs + 50e-6);
//					scheduleNodeUpdate(primHead, dataOut, '=',
//						new Integer(Stimuli.LOGIC_X), Stimuli.OFF_STRENGTH, als.timeAbs + 50e-6);
//				}
//			}
//		}
//	}
//
//	static class AFRegisters extends ALS.UserProc
//	{
//		AFRegisters(ALS als) { nameMe(als, "A_F_REGISTERS"); }
//
//		void simulate(ALS.Model primHead)
//		{
//			ALS.ALSExport ck = primHead.exList.get(0);
//			ALS.ALSExport aIn = primHead.exList.get(1);
//			ALS.ALSExport aLoad = primHead.exList.get(2);
//			ALS.ALSExport fIn = primHead.exList.get(3);
//			ALS.ALSExport fLoad = primHead.exList.get(4);
//			ALS.ALSExport aMid = primHead.exList.get(5);
//			ALS.ALSExport aOut = primHead.exList.get(6);
//			ALS.ALSExport fMid = primHead.exList.get(7);
//			ALS.ALSExport fOut = primHead.exList.get(8);
//
//			if (ck.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//			{
//				if (aLoad.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//				{
//					scheduleNodeUpdate(primHead, aMid, '=',
//						new Integer(aIn.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				}
//				if (fLoad.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//				{
//					scheduleNodeUpdate(primHead, fMid, '=',
//						new Integer(fIn.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				}
//				return;
//			}
//
//			if (ck.nodePtr.sumState == Stimuli.LOGIC_LOW)
//			{
//				if (aLoad.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//				{
//					scheduleNodeUpdate(primHead, aOut, '=',
//						new Integer(aMid.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				}
//				if (fLoad.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//				{
//					scheduleNodeUpdate(primHead, fOut, '=',
//						new Integer(fMid.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				}
//				return;
//			}
//
//			scheduleNodeUpdate(primHead, aMid, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//			scheduleNodeUpdate(primHead, aOut, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//			scheduleNodeUpdate(primHead, fMid, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//			scheduleNodeUpdate(primHead, aOut, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//		}
//	}
//
//	static class ControlLogic extends ALS.UserProc
//	{
//		ControlLogic(ALS als) { nameMe(als, "CONTROL_LOGIC"); }
//
//		void simulate(ALS.Model primHead)
//		{
//			ALS.ALSExport aIn = primHead.exList.get(0);
//			ALS.ALSExport fIn = primHead.exList.get(1);
//			ALS.ALSExport b = primHead.exList.get(2);
//			ALS.ALSExport msb = primHead.exList.get(3);
//			ALS.ALSExport aOut = primHead.exList.get(4);
//			ALS.ALSExport fOut = primHead.exList.get(5);
//
//			if (b.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//			{
//				scheduleNodeUpdate(primHead, aOut, '=',
//					new Integer(aIn.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//			} else
//			{
//				 if (b.nodePtr.sumState == Stimuli.LOGIC_LOW)
//				 {
//					scheduleNodeUpdate(primHead, aOut, '=',
//						new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				 } else
//				 {
//					scheduleNodeUpdate(primHead, aOut, '=',
//						new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				 }
//			}
//
//			if (msb.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//			{
//				scheduleNodeUpdate(primHead, fOut, '=',
//					new Integer(fIn.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//			} else
//			{
//				if (msb.nodePtr.sumState == Stimuli.LOGIC_LOW)
//				{
//					scheduleNodeUpdate(primHead, fOut, '=',
//						new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				} else
//				{
//					scheduleNodeUpdate(primHead, fOut, '=',
//						new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				}
//			}
//		}
//	}
//
//	static class Mod2Adder extends ALS.UserProc
//	{
//		Mod2Adder(ALS als) { nameMe(als, "MOD2_ADDER"); }
//
//		void simulate(ALS.Model primHead)
//		{
//			ALS.ALSExport aIn = primHead.exList.get(0);
//			ALS.ALSExport fIn = primHead.exList.get(1);
//			ALS.ALSExport pIn = primHead.exList.get(2);
//			ALS.ALSExport ck = primHead.exList.get(3);
//			ALS.ALSExport out = primHead.exList.get(4);
//
//			if (ck.nodePtr.sumState == Stimuli.LOGIC_LOW)
//			{
//				scheduleNodeUpdate(primHead, out, '=',
//					new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				return;
//			}
//
//			int sum = 0;
//			if (ck.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//			{
//				if (aIn.nodePtr.sumState == Stimuli.LOGIC_HIGH) ++sum;
//				if (fIn.nodePtr.sumState == Stimuli.LOGIC_HIGH) ++sum;
//				if (pIn.nodePtr.sumState == Stimuli.LOGIC_HIGH) ++sum;
//
//				sum %= 2;
//				if (sum != 0)
//				{
//					scheduleNodeUpdate(primHead, out, '=',
//						new Integer(Stimuli.LOGIC_HIGH), Stimuli.GATE_STRENGTH, als.timeAbs + 5.0e-9);
//				} else
//				{
//					scheduleNodeUpdate(primHead, out, '=',
//						new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs + 5.0e-9);
//				}
//				return;
//			}
//
//			scheduleNodeUpdate(primHead, out, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 5.0e-9);
//		}
//	}
//
//	static class AboveAdder extends ALS.UserProc
//	{
//		AboveAdder(ALS als) { nameMe(als, "ABOVE_ADDER"); }
//
//		void simulate(ALS.Model primHead)
//		{
//			ALS.ALSExport ck = primHead.exList.get(0);
//			ALS.ALSExport sync = primHead.exList.get(1);
//			ALS.ALSExport loadOSR = primHead.exList.get(2);
//			ALS.ALSExport sumIn = primHead.exList.get(3);
//			ALS.ALSExport osrIn = primHead.exList.get(4);
//			ALS.ALSExport osrMid = primHead.exList.get(5);
//			ALS.ALSExport osrOut = primHead.exList.get(6);
//			ALS.ALSExport pMid = primHead.exList.get(7);
//			ALS.ALSExport pOut = primHead.exList.get(8);
//
//			if (ck.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//			{
//				if (loadOSR.nodePtr.sumState == Stimuli.LOGIC_LOW)
//				{
//					scheduleNodeUpdate(primHead, pMid, '=',
//						new Integer(sumIn.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//					scheduleNodeUpdate(primHead, osrMid, '=',
//						new Integer(osrIn.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				}
//				if (loadOSR.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//				{
//					scheduleNodeUpdate(primHead, osrMid, '=',
//						new Integer(sumIn.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				}
//				if (sync.nodePtr.sumState == Stimuli.LOGIC_HIGH)
//				{
//					scheduleNodeUpdate(primHead, pMid, '=',
//						new Integer(Stimuli.LOGIC_LOW), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				}
//				return;
//			}
//
//			if (ck.nodePtr.sumState == Stimuli.LOGIC_LOW)
//			{
//				if (loadOSR.nodePtr.sumState == Stimuli.LOGIC_LOW)
//				{
//					scheduleNodeUpdate(primHead, osrOut, '=',
//						new Integer(osrMid.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//					scheduleNodeUpdate(primHead, pOut, '=',
//						new Integer(pMid.nodePtr.sumState), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//				}
//				return;
//			}
//
//			scheduleNodeUpdate(primHead, osrMid, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//			scheduleNodeUpdate(primHead, osrOut, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//			scheduleNodeUpdate(primHead, pMid, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//			scheduleNodeUpdate(primHead, pOut, '=',
//				new Integer(Stimuli.LOGIC_X), Stimuli.GATE_STRENGTH, als.timeAbs + 3.0e-9);
//		}
//	}
//
//	/**
//	 * Class converts the value of 12 input bits into a state
//	 * representation in the range.  This function can be called for
//	 * the compact representation of the state of a bus structure in hexadecimal
//	 * format.
//	 *
//	 * Calling Arguments:
//	 *	primHead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are ordered b7, b6, b5,
//	 *		 b4, b3, b2, b1, b0, output in this list.
//	 */
//	static class Bus12ToState extends ALS.UserProc
//	{
//		Bus12ToState(ALS als) { nameMe(als, "BUS12_TO_STATE"); }
//
//		void simulate(ALS.Model primHead)
//		{
//			Iterator it = primHead.exList.iterator();
//			ALS.ALSExport argPtr = it.next();
//			int state = 0;
//			for (int i = 11; i > -1; --i)
//			{
//				int bit = argPtr.nodePtr.sumState;
//				if (bit == Stimuli.LOGIC_HIGH) state += (0x01 << i);
//				argPtr = it.next();
//			}
//
//			scheduleNodeUpdate(primHead, argPtr, '=',
//				new Integer(state), Stimuli.VDD_STRENGTH, als.timeAbs);
//		}
//	}
}

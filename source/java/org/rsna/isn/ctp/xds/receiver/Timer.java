/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.receiver;


/**
 * A class to time events.
 */
public class Timer {

	long startTime;

    /**
	 * Construct a Timer.
	 */
	public Timer() {
		reset();
	}

	/**
	 * Get the time as a String.
	 */
	public String getElapsedTime() {
		long et = System.currentTimeMillis() - startTime;
		return String.format("%.3f sec", ((float)et)/1000.);
	}

	/**
	 * Reset the timer
	 */
	public void reset() {
		this.startTime = System.currentTimeMillis();
	}

}
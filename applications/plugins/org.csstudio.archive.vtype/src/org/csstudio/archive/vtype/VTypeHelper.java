/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.vtype;

import org.epics.pvmanager.data.Alarm;
import org.epics.pvmanager.data.AlarmSeverity;
import org.epics.pvmanager.data.Display;
import org.epics.pvmanager.data.Time;
import org.epics.pvmanager.data.VDoubleArray;
import org.epics.pvmanager.data.VEnum;
import org.epics.pvmanager.data.VEnumArray;
import org.epics.pvmanager.data.VNumber;
import org.epics.pvmanager.data.VNumberArray;
import org.epics.pvmanager.data.VStatistics;
import org.epics.pvmanager.data.VString;
import org.epics.pvmanager.data.VType;
import org.epics.pvmanager.data.ValueUtil;
import org.epics.util.array.ListInt;
import org.epics.util.array.ListNumber;
import org.epics.util.time.Timestamp;

/** {@link VType} helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypeHelper
{
	/** Number of array elements to show before shortening the printout */
	final private static int MAX_ARRAY_ELEMENTS = 10;
	
	/** Read number from a {@link VType}
     *  @param value Value
     *  @return double or NaN
     */
    final public static double toDouble(final VType value)
    {
        if (value instanceof VNumber)
            return ((VNumber)value).getValue().doubleValue();
        if (value instanceof VEnum)
            return ((VEnum)value).getIndex();
        if (value instanceof VStatistics)
            return ((VStatistics)value).getAverage();
        if (value instanceof VNumberArray)
        {
            final ListNumber data = ((VNumberArray) value).getData();
            return data.getDouble(0);
        }
        if (value instanceof VEnumArray)
        {
            final ListInt data = ((VEnumArray) value).getIndexes();
            return data.getDouble(0);
        }
        return Double.NaN;
    }

    /** Read number from a {@link VType}
     *  @param value Value
     *  @param index Waveform index
     *  @return double or NaN
     */
    final public static double toDouble(final VType value, final int index)
    {
        if (index == 0)
            return toDouble(value);
        if (value instanceof VNumberArray)
        {
            final ListNumber data = ((VNumberArray) value).getData();
            if (index < data.size())
                return data.getDouble(index);
        }
        if (value instanceof VEnumArray)
        {
            final ListInt data = ((VEnumArray) value).getIndexes();
            if (index < data.size())
                return data.getDouble(index);
        }
        return Double.NaN;
    }

    
    /** Decode a {@link VType}'s time stamp
	 *  @param value Value to decode
	 *  @return {@link Timestamp}
	 */
	final public static Timestamp getTimestamp(final VType value)
	{
		if (value instanceof Time)
		{
			final Time time = (Time) value;
		    if (time.isTimeValid())
		        return time.getTimestamp();
		}
	    return Timestamp.now();
	}
	
    /** @return Copy of given value with timestamp set to 'now',
     *          or <code>null</code> if value is not handled
     */
    public static VType transformTimestampToNow(final VType value)
    {
        return transformTimestamp(value, Timestamp.now());
    }

    /** @return Copy of given value with updated timestamp,
     *          or <code>null</code> if value is not handled
     */
    public static VType transformTimestamp(final VType value,
                                           final Timestamp time)
    {
        if (value instanceof VNumber)
        {
            final VNumber number = (VNumber) value;
            return new ArchiveVNumber(time, number.getAlarmSeverity(), number.getAlarmName(), number, number.getValue());
        }
        if (value instanceof VString)
        {
            final VString string = (VString) value;
            return new ArchiveVString(time, string.getAlarmSeverity(), string.getAlarmName(), string.getValue());
        }
        if (value instanceof VDoubleArray)
        {
            final VDoubleArray number = (VDoubleArray) value;
            return new ArchiveVDoubleArray(time, number.getAlarmSeverity(), number.getAlarmName(), number, number.getData());
        }
        if (value instanceof VNumberArray)
        {
            final VNumberArray number = (VNumberArray) value;
            final ListNumber data = number.getData();
            final double[] dbl = new double[data.size()];
            for (int i=0; i<dbl.length; ++i)
                dbl[i] = data.getDouble(i);
            return new ArchiveVDoubleArray(time, number.getAlarmSeverity(), number.getAlarmName(), number, dbl);
        }
        if (value instanceof VEnum)
        {
            final VEnum labelled = (VEnum) value;
            return new ArchiveVEnum(time, labelled.getAlarmSeverity(), labelled.getAlarmName(), labelled.getLabels(), labelled.getIndex());
        }
        return null;
    }
    
	/** @param buf Buffer where value's time stamp is added
	 *  @param value {@link VType}
	 */
	final public static void addTimestamp(final StringBuilder buf, final VType value)
	{
		final Timestamp stamp = getTimestamp(value);
		buf.append(TimestampHelper.format(stamp));
	}
    
	/** @param value {@link VType} value
	 *  @return {@link AlarmSeverity}
	 */
	final public static AlarmSeverity getSeverity(final VType value)
	{
		final Alarm alarm = ValueUtil.alarmOf(value);
		if (alarm == null)
			return AlarmSeverity.NONE;
		return alarm.getAlarmSeverity();
	}

	/** @param value {@link VType} value
	 *  @return Alarm message
	 */
	final public static String getMessage(final VType value)
	{
		final Alarm alarm = ValueUtil.alarmOf(value);
		if (alarm == null)
			return "";
		return alarm.getAlarmName();
	}
	
	/** @param buf Buffer where value's alarm info is added (unless OK)
	 *  @param value {@link VType}
	 */
	final public static void addAlarm(final StringBuilder buf, final VType value)
	{
		final Alarm alarm = ValueUtil.alarmOf(value);
		if (alarm == null  ||  alarm.getAlarmSeverity() == AlarmSeverity.NONE)
			return;
		buf.append(alarm.getAlarmSeverity().toString())
       	   .append("/")
       	   .append(alarm.getAlarmName());
	}

	/** @param buf Buffer where number is added
	 *  @param display Display information to use, may be <code>null</code>
	 *  @param number Number to format
	 */
	final private static void addNumber(final StringBuilder buf,
			final Display display, final double number)
	{
		if (display != null  &&  display.getFormat() != null)
			buf.append(display.getFormat().format(number));
		else
			buf.append(number);
	}

	/** @param buf Buffer where value's actual value is added (number, ...)
	 *  @param value {@link VType}
	 */
	final public static void addValue(final StringBuilder buf, final VType value)
	{
		// After the time this is implemented, VEnum may change into a class
		// that also implements VNumber and/or VString.
		// Handle it first to assert that VEnum is always identified as such
		// and not handled as Number.
		if (value instanceof VEnum)
		{
			final VEnum enumerated = (VEnum)value;
			try
			{
				buf.append(enumerated.getValue());
				buf.append(" (").append(enumerated.getIndex()).append(")");
			}
			catch (ArrayIndexOutOfBoundsException ex)
			{	// Error getting label for invalid index?
				buf.append("<enum ").append(enumerated.getIndex()).append(">");
			}
		}
		else if (value instanceof VNumber)
		{
			final VNumber number = (VNumber) value;
			final Display display = (Display) number;
			addNumber(buf, display, number.getValue().doubleValue());
			if (display != null  &&  display.getUnits() != null)
				buf.append(" ").append(display.getUnits());
		}
		else if (value instanceof VNumberArray)
		{
			final VNumberArray array = (VNumberArray) value;
			final Display display = (Display) array;
			final ListNumber list = array.getData();
			final int N = list.size();
			if (N <= MAX_ARRAY_ELEMENTS)
			{
				if (N > 0)
					addNumber(buf, display, list.getDouble(0));
				for (int i=1; i<N; ++i)
				{
					buf.append(", ");
					addNumber(buf, display, list.getDouble(i));
				}
			}
			else
			{
				addNumber(buf, display, list.getDouble(0));
				for (int i=1; i<MAX_ARRAY_ELEMENTS/2; ++i)
				{
					buf.append(", ");
					addNumber(buf, display, list.getDouble(i));
				}
				buf.append(", ... (total ").append(N).append(" elements) ...");
				for (int i = N - MAX_ARRAY_ELEMENTS/2;  i<N;  ++i)
				{
					buf.append(", ");
					addNumber(buf, display, list.getDouble(i));
				}
			}
            if (display != null  &&  display.getUnits() != null)
				buf.append(" ").append(display.getUnits());
		}
		else if (value instanceof VStatistics)
		{
			final VStatistics stats = (VStatistics) value;
			final Display display = (Display) stats;
			buf.append(stats.getAverage());
			buf.append(" [").append(stats.getMin()).append(" ... ").append(stats.getMax());
			final Double dev = stats.getStdDev();
			if (dev > 0)
				buf.append(", dev ").append(dev);
			buf.append("]");
            if (display != null  &&  display.getUnits() != null)
				buf.append(" ").append(display.getUnits());
		}
		else if (value instanceof VString)
			buf.append(((VString)value).getValue());
		else if (value == null)
			buf.append("null");
		else
			buf.append(value.toString());
	}

	/** Format just the value of a {@link VType} as string (not timestamp, ..)
     *  @param value Value
     *  @return String representation of its value
     */
    final public static String formatValue(final VType value)
    {
        final StringBuilder buf = new StringBuilder();
        addValue(buf, value);
        return buf.toString();
    }
	
	/** Format value as string
     *  @param value Value
     *  @return String representation
     */
    final public static String toString(final VType value)
    {
    	if (value == null)
    		return "null";
    	final StringBuilder buf = new StringBuilder();
    	addTimestamp(buf, value);
    	buf.append("\t");
    	addValue(buf, value);
    	buf.append("\t");
    	addAlarm(buf, value);
        return buf.toString();
    }
}

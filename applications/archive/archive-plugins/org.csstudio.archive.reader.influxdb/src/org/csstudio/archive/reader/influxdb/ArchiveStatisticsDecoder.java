package org.csstudio.archive.reader.influxdb;

import java.time.Instant;
import java.util.logging.Level;

import org.csstudio.archive.reader.influxdb.raw.AbstractInfluxDBValueDecoder;
import org.csstudio.archive.reader.influxdb.raw.AbstractInfluxDBValueLookup;
import org.csstudio.archive.vtype.ArchiveVStatistics;
import org.diirt.vtype.AlarmSeverity;
import org.diirt.vtype.Display;
import org.diirt.vtype.Statistics;
import org.diirt.vtype.VType;

public class ArchiveStatisticsDecoder extends ArchiveDecoder
{
	//lookup names of statistics fields; used for iteration
	private static final String STATS_NAMES [] = {"mean", "max", "min", "stddev", "count"};
	
	public ArchiveStatisticsDecoder(AbstractInfluxDBValueLookup vals)
	{
		super(vals);
	}
	
    public static class Factory extends AbstractInfluxDBValueDecoder.Factory
    {
        @Override
        public AbstractInfluxDBValueDecoder create(AbstractInfluxDBValueLookup vals) {
            return new ArchiveStatisticsDecoder(vals);
        }
    }
    
    private static class StatisticsImpl implements Statistics
    {
    	private Double mean_max_min_stddev [];
    	private Integer count;
    	
    	public StatisticsImpl(Double mean_max_min_stddev [], Integer count)
    	{
    		this.mean_max_min_stddev = mean_max_min_stddev;
    		this.count = count;
    	}
    	
		@Override
		public Double getAverage() {
			return mean_max_min_stddev[0];
		}
		@Override
		public Double getMax() {
			return mean_max_min_stddev[1];
		}
		@Override
		public Double getMin() {
			return mean_max_min_stddev[2];
		}
		@Override
		public Integer getNSamples() {
			return count;
		}
		@Override
		public Double getStdDev() {
			return mean_max_min_stddev[3];
		}
    }

    @Override
    protected VType decodeLongSample(final Instant time, final AlarmSeverity severity, final String status, Display display) throws Exception
    {
    	//mean, max, min, stddev, count
    	Object stats_vals [] = new Object [5];
    	int x = 0;
    	for (String stat : STATS_NAMES)
    	{
    		stats_vals[x] = vals.getValue(stat + "_long.0");
    		if (stats_vals[x] == null)
    			throw new Exception("Did not find "+stat+"_long.0 field where expected");
    		++x;
    	}
    	
    	Double mean_max_min_stddev [] = new Double [4];
    	for (int i = 0; i < 4; ++i)
    	{
    		mean_max_min_stddev[i] = (double) fieldToLong(stats_vals[i]);
    	}
    	int count = fieldToInt(stats_vals[4]);
    	
    	return new ArchiveVStatistics(time, severity, status, display, new StatisticsImpl(mean_max_min_stddev, count));
    }

    protected VType decodeDoubleSamples(final Instant time, final AlarmSeverity severity, final String status, Display display) throws Exception
    {
    	//mean, max, min, stddev, count
    	Object stats_vals [] = new Object [5];
    	int x = 0;
    	for (String stat : STATS_NAMES)
    	{
    		stats_vals[x] = vals.getValue(stat + "_double.0");
    		if (stats_vals[x] == null)
    			//throw new Exception("Did not find "+stat+"_double.0 field where expected");
    			stats_vals[x] = x < 4 ? Double.NaN : 0;
    		++x;
    	}
    	
    	Double mean_max_min_stddev [] = new Double [4];
    	for (int i = 0; i < 4; ++i)
    	{
    		mean_max_min_stddev[i] = fieldToDouble(stats_vals[i]);
    	}
    	int count = fieldToInt(stats_vals[4]);
    	
    	return new ArchiveVStatistics(time, severity, status, display, new StatisticsImpl(mean_max_min_stddev, count));
    }
    
    private Integer fieldToInt(Object val) throws Exception
    {
        Integer integer;
        //try
        //{
        //	integer = Integer.class.cast(val);
        //}
        //catch (Exception e)
        //{
            try
            {
                integer = Double.valueOf(val.toString()).intValue();
            }
            catch (Exception e1)
            {
                throw new Exception ("Could not transform object to Integer: " + val.getClass().getName());
            }
        //}
        return integer;
    }
}

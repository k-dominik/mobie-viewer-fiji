package org.embl.mobie.viewer.table.columns;

import org.embl.mobie.viewer.table.ColumnNames;

import java.util.Collection;
import java.util.List;

public class MoBIESegmentColumnNames implements SegmentColumnNames
{
	@Override
	public String labelImageColumn()
	{
		return ColumnNames.LABEL_IMAGE_ID;
	}

	@Override
	public String labelIdColumn()
	{
		return ColumnNames.LABEL_ID;
	}

	@Override
	public String timePointColumn()
	{
		return ColumnNames.TIMEPOINT;
	}

	@Override
	public String[] anchorColumns()
	{
		return new String[]{ ColumnNames.ANCHOR_X, ColumnNames.ANCHOR_Y, ColumnNames.ANCHOR_Z };
	}

	@Override
	public String[] bbMinColumns()
	{
		return new String[]{ ColumnNames.BB_MIN_X, ColumnNames.BB_MIN_Y, ColumnNames.BB_MIN_Z };
	}

	@Override
	public String[] bbMaxColumns()
	{
		return new String[]{ ColumnNames.BB_MAX_X, ColumnNames.BB_MAX_Y, ColumnNames.BB_MAX_Z };
	}

	public static boolean matches( Collection< String > columns )
	{
		return columns.contains( ColumnNames.ANCHOR_X );
	}

}

package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import javax.annotation.Nullable;

public class TableSawAnnotatedSegment implements AnnotatedSegment
{
	private Row row;
	private final int numSegmentDimensions;
	private final Table table;
	private final int rowIndex;
	private final int timePoint;
	private final int labelId;
	private RealInterval boundingBox;
	private float[] mesh;
	private String imageId;

	public TableSawAnnotatedSegment(
			Table table,
			int rowIndex,
			@Nullable String imageId // may be present in table
	)
	{
		this.table = table;
		this.rowIndex = rowIndex;
		this.row = table.row( rowIndex );

		// segment properties
		this.numSegmentDimensions = row.columnNames().contains( ColumnNames.ANCHOR_Z ) ? 3 : 2;
		this.imageId = table.columnNames().contains( ColumnNames.LABEL_IMAGE_ID ) ? row.getString( ColumnNames.LABEL_IMAGE_ID ) : imageId;
		this.timePoint = table.columnNames().contains( ColumnNames.TIMEPOINT ) ? row.getInt( ColumnNames.TIMEPOINT ) : 0;
		this.labelId = row.getInt( ColumnNames.LABEL_ID );
		initBoundingBox( row, numSegmentDimensions );
	}

	private void initBoundingBox( Row row, int numSegmentDimensions )
	{
		if ( row.columnNames().contains( ColumnNames.BB_MIN_X ) )
		{
			if ( numSegmentDimensions == 2 )
			{
				final double[] min = {
						row.getDouble( ColumnNames.BB_MIN_X ),
						row.getDouble( ColumnNames.BB_MIN_Y )
				};
				final double[] max = {
						row.getDouble( ColumnNames.BB_MAX_X ),
						row.getDouble( ColumnNames.BB_MAX_Y )
				};
				boundingBox = new FinalRealInterval( min, max );
			}
			else if ( numSegmentDimensions == 3 )
			{
				final double[] min = {
						row.getDouble( ColumnNames.BB_MIN_X ),
						row.getDouble( ColumnNames.BB_MIN_Y ),
						row.getDouble( ColumnNames.BB_MIN_Z )
				};
				final double[] max = {
						row.getDouble( ColumnNames.BB_MAX_X ),
						row.getDouble( ColumnNames.BB_MAX_Y ),
						row.getDouble( ColumnNames.BB_MAX_Z )
				};
				boundingBox = new FinalRealInterval( min, max );
			}
		}
	}

	@Override
	public String imageId()
	{
		return imageId;
	}

	@Override
	public int labelId()
	{
		return labelId;
	}

	@Override
	public int timePoint()
	{
		return timePoint;
	}

	@Override
	public double[] anchor()
	{
		return new double[]{
				row.getDouble( ColumnNames.ANCHOR_X ),
				row.getDouble( ColumnNames.ANCHOR_Y ),
				row.getDouble( ColumnNames.ANCHOR_Z )
		};
	}

	@Override
	public RealInterval boundingBox()
	{
		return null;
	}

	@Override
	public void setBoundingBox( RealInterval boundingBox )
	{

	}

	@Override
	public float[] mesh()
	{
		return mesh;
	}

	@Override
	public void setMesh( float[] mesh )
	{
		this.mesh = mesh;
	}

	@Override
	public String id()
	{
		return null; // MUST
	}

	@Override
	public Object getValue( String columnName )
	{
		return row.getObject( columnName );
	}

	@Override
	public void setString( String columnName, String value )
	{
		row.setText( columnName, value );
	}

}

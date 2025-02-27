/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.annotation;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.bdv.GlobalMousePositionProvider;
import org.embl.mobie.lib.serialize.display.AbstractAnnotationDisplay;
import org.embl.mobie.lib.source.AnnotationType;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.Collection;
import java.util.function.Supplier;

public class SliceViewAnnotationSelector< A extends Annotation > implements Runnable
{
	private BdvHandle bdvHandle;
	private boolean is2D;
	private Supplier< Collection< AbstractAnnotationDisplay< A > > > annotationDisplaySupplier;

	public SliceViewAnnotationSelector( BdvHandle bdvHandle, boolean is2D, Supplier< Collection< AbstractAnnotationDisplay< A > > > annotationDisplaySupplier )
	{
		this.bdvHandle = bdvHandle;
		this.is2D = is2D;
		this.annotationDisplaySupplier = annotationDisplaySupplier;
	}

	public synchronized void clearSelection()
	{
		final Collection< AbstractAnnotationDisplay< A > > annotationDisplays = annotationDisplaySupplier.get();

		for ( AbstractAnnotationDisplay< A > annotationDisplay : annotationDisplays )
			annotationDisplay.selectionModel.clearSelection();

	}

	private synchronized void toggleSelectionAtMousePosition()
	{
		final GlobalMousePositionProvider positionProvider = new GlobalMousePositionProvider( bdvHandle );
		final int timePoint = positionProvider.getTimePoint();
		final RealPoint realPosition = positionProvider.getPositionAsRealPoint();

		final Collection< AbstractAnnotationDisplay< A > > annotationDisplays = annotationDisplaySupplier.get();

		for ( AbstractAnnotationDisplay< A > annotationDisplay : annotationDisplays )
		{
			final Collection< SourceAndConverter< AnnotationType< A > > > sourceAndConverters = annotationDisplay.sourceAndConverters();

			for ( SourceAndConverter< AnnotationType< A > > sourceAndConverter : sourceAndConverters )
			{
				if ( ! bdvHandle.getViewerPanel().state().isSourceVisible( sourceAndConverter ) )
					continue;

				if ( SourceAndConverterHelper.isPositionWithinSourceInterval( sourceAndConverter, realPosition, timePoint, is2D ) )
				{
					final Source< AnnotationType< A > > source = sourceAndConverter.getSpimSource();

					// Find Annotation in voxel grid space
//					final long[] voxelPosition = SourceAndConverterHelper.getVoxelPositionInSource( source, realPosition, timePoint, 0 );
//					final AnnotationType< A > annotationType = source.getSource( timePoint, 0 ).randomAccess().setPositionAndGet( voxelPosition );

					// Find Annotation in voxel grid real space
					// This is needed for sources like the SpotImage, which is
					// created in real space and does not live on a voxel grid.
					// See also: https://github.com/mobie/spatial-transcriptomics-example-project/issues/22
					AffineTransform3D sourceTransform = new AffineTransform3D();
					source.getSourceTransform( timePoint, 0, sourceTransform);
					final RealPoint positionInSource = new RealPoint( 3 );
					sourceTransform.inverse().apply( realPosition, positionInSource );
					final AnnotationType< A > annotationType = source.getInterpolatedSource( timePoint, 0, Interpolation.NEARESTNEIGHBOR ).getAt( positionInSource );

					final A annotation = annotationType.getAnnotation();

					if ( annotation != null )
					{
						annotationDisplay.selectionModel.toggle( annotation );

						if ( annotationDisplay.selectionModel.isSelected( annotation ) )
						{
							annotationDisplay.selectionModel.focus( annotation, this );
						}
					}
				}
			}
		}
	}

//	private TableRow getTableRow( int timePoint, AnnotationDisplay regionDisplay, String sourceName, double labelIndex )
//	{
//		if ( regionDisplay instanceof SegmentationDisplay )
//		{
//			final boolean containsSegment = (( SegmentationDisplay ) regionDisplay ).tableRowsAdapter.containsSegment( labelIndex, timePoint, sourceName );
//
//			if ( ! containsSegment )
//			{
//				// This happens when there is a segmentation without
//				// a segment table, or when the background label has been selected
//				return null;
//			}
//			else
//			{
//				return ( ( SegmentationDisplay ) regionDisplay ).tableRowsAdapter.getSegment( labelIndex, timePoint, sourceName );
//			}
//		}
//		else if ( regionDisplay instanceof RegionDisplay )
//		{
//			final RegionDisplay annotatedSourceDisplay = ( RegionDisplay ) regionDisplay;
//			final RegionsAdapter adapter = annotatedSourceDisplay.tableRowsAdapter;
//			return adapter.getAnnotatedMask( timePoint, labelIndex );
//		}
//		else
//		{
//			throw new UnsupportedOperationException( "Region display of type " + regionDisplay.getClass().getName() + " is not supported for selection.");
//		}
//	}

//	private static double getLabelIndex( Source< ? > source, double pixelValue )
//	{
//		if ( GridSource.instanceOf( source ) )
//		{
//			return SourceNameEncoder.getValue( Double.valueOf( pixelValue ).longValue() );
//		}
//		else
//		{
//			return pixelValue;
//		}
//	}
//
//	private static String getSourceName( Source< ? > source, double labelIndex )
//	{
//		if ( GridSource.instanceOf( source ) )
//		{
//			return SourceNameEncoder.getName( Double.valueOf( labelIndex ).longValue() );
//		}
//		else
//		{
//			return source.getName();
//		}
//	}
//
//	private static double getPixelValue( int timePoint, RealPoint realPoint, Source< ? > source )
//	{
//		final RandomAccess< RealType > randomAccess = ( RandomAccess< RealType > ) source.getSource( timePoint, 0 ).randomAccess();
//		final long[] positionInSource = SourceAndConverterHelper.getVoxelPositionInSource( source, realPoint, timePoint, 0 );
//		randomAccess.setPosition( positionInSource );
//		return randomAccess.get().getRealDouble();
//	}

	@Override
	public void run()
	{
		toggleSelectionAtMousePosition();
	}
}

/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer.view;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import org.apache.commons.lang.ArrayUtils;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.DataStore;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.annotation.AnnotatedRegion;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.viewer.bdv.view.AnnotationSliceView;
import org.embl.mobie.viewer.bdv.view.ImageSliceView;
import org.embl.mobie.viewer.bdv.view.SliceViewer;
import org.embl.mobie.viewer.color.CategoricalAnnotationColoringModel;
import org.embl.mobie.viewer.color.ColorHelper;
import org.embl.mobie.viewer.color.ColoringModels;
import org.embl.mobie.viewer.color.MobieColoringModel;
import org.embl.mobie.viewer.color.NumericAnnotationColoringModel;
import org.embl.mobie.viewer.color.lut.ColumnARGBLut;
import org.embl.mobie.viewer.color.lut.LUTs;
import org.embl.mobie.viewer.image.AnnotatedLabelImage;
import org.embl.mobie.viewer.image.DefaultAnnotatedLabelImage;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.image.RegionLabelImage;
import org.embl.mobie.viewer.image.StitchedAnnotatedLabelImage;
import org.embl.mobie.viewer.image.StitchedImage;
import org.embl.mobie.viewer.plot.ScatterPlotView;
import org.embl.mobie.viewer.select.MoBIESelectionModel;
import org.embl.mobie.viewer.serialize.DataSource;
import org.embl.mobie.viewer.serialize.RegionDataSource;
import org.embl.mobie.viewer.serialize.View;
import org.embl.mobie.viewer.serialize.display.AnnotationDisplay;
import org.embl.mobie.viewer.serialize.display.Display;
import org.embl.mobie.viewer.serialize.display.ImageDisplay;
import org.embl.mobie.viewer.serialize.display.RegionDisplay;
import org.embl.mobie.viewer.serialize.display.SegmentationDisplay;
import org.embl.mobie.viewer.serialize.display.SpotDisplay;
import org.embl.mobie.viewer.serialize.transformation.AbstractGridTransformation;
import org.embl.mobie.viewer.serialize.transformation.AffineTransformation;
import org.embl.mobie.viewer.serialize.transformation.CropTransformation;
import org.embl.mobie.viewer.serialize.transformation.GridTransformation;
import org.embl.mobie.viewer.serialize.transformation.MergedGridTransformation;
import org.embl.mobie.viewer.serialize.transformation.Transformation;
import org.embl.mobie.viewer.source.CroppedImage;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.table.AnnotationTableModel;
import org.embl.mobie.viewer.table.ColumnNames;
import org.embl.mobie.viewer.table.DefaultAnnData;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.table.TableView;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedRegion;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedRegionCreator;
import org.embl.mobie.viewer.table.saw.TableSawAnnotationCreator;
import org.embl.mobie.viewer.table.saw.TableSawAnnotationTableModel;
import org.embl.mobie.viewer.transform.MoBIEViewerTransformAdjuster;
import org.embl.mobie.viewer.transform.NormalizedAffineViewerTransform;
import org.embl.mobie.viewer.transform.SliceViewLocationChanger;
import org.embl.mobie.viewer.transform.TransformHelper;
import org.embl.mobie.viewer.transform.image.ImageTransformer;
import org.embl.mobie.viewer.ui.UserInterface;
import org.embl.mobie.viewer.ui.WindowArrangementHelper;
import org.embl.mobie.viewer.view.save.ViewSaver;
import org.embl.mobie.viewer.volume.ImageVolumeViewer;
import org.embl.mobie.viewer.volume.SegmentsVolumeViewer;
import org.embl.mobie.viewer.volume.UniverseManager;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import tech.tablesaw.api.Table;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ViewManager
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final MoBIE moBIE;
	private final UserInterface userInterface;
	private final SliceViewer sliceViewer;
	private final SourceAndConverterService sacService;
	private List< Display > currentDisplays;
	private List< Transformation > currentTransformations;
	private final UniverseManager universeManager;
	private final AdditionalViewsLoader additionalViewsLoader;
	private final ViewSaver viewSaver;
	private int numViewedTables = 0;

	public ViewManager( MoBIE moBIE, UserInterface userInterface, boolean is2D )
	{
		this.moBIE = moBIE;
		this.userInterface = userInterface;
		currentDisplays = new ArrayList<>();
		currentTransformations = new ArrayList<>();
		sliceViewer = new SliceViewer( moBIE, is2D );
		universeManager = new UniverseManager();
		additionalViewsLoader = new AdditionalViewsLoader( moBIE );
		viewSaver = new ViewSaver( moBIE );
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
	}

	private void initScatterPlotView( AnnotationDisplay< ? > display )
	{
		// TODO: implement for multiple images
		//   probably needs an AnnotationTableModel constructed
		//   from multiple tables
		//   Note that the same code is needed for the TableView,
		//   thus maybe this needs to happen within annotationDisplay?
		final AnnotatedLabelImage annotatedLabelImage = ( AnnotatedLabelImage ) display.getImages().iterator().next();
		final AnnotationTableModel annotationTableModel = annotatedLabelImage.getAnnData().getTable();

		String[] scatterPlotAxes = display.getScatterPlotAxes();
		display.scatterPlotView = new ScatterPlotView( annotationTableModel, display.selectionModel, display.coloringModel, scatterPlotAxes, new double[]{1.0, 1.0}, 0.5 );
		display.selectionModel.listeners().add( display.scatterPlotView );
		display.coloringModel.listeners().add( display.scatterPlotView );
		display.sliceViewer.getBdvHandle().getViewerPanel().addTimePointListener( display.scatterPlotView );

		if ( display.showScatterPlot() )
		{
			display.scatterPlotView.setShowConfigurationUI( false );
			display.scatterPlotView.show();
		}
	}

	public List< Display > getCurrentSourceDisplays()
	{
		return currentDisplays;
	}

	public SliceViewer getSliceViewer()
	{
		return sliceViewer;
	}

	public AdditionalViewsLoader getAdditionalViewsLoader() { return additionalViewsLoader; }

	public ViewSaver getViewsSaver() { return viewSaver; }

	private void addManualTransforms( List< Transformation > viewSourceTransforms, List< ? extends SourceAndConverter< ? >> sourceAndConverters )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			Source< ? > source = sourceAndConverter.getSpimSource();

			final TransformedSource< ? > transformedSource = SourceHelper.unwrapSource( source, TransformedSource.class );

			if ( transformedSource != null )
			{
				AffineTransform3D fixedTransform = new AffineTransform3D();
				transformedSource.getFixedTransform( fixedTransform );
				if ( ! fixedTransform.isIdentity() )
				{
					List< String > sources = new ArrayList<>();
					sources.add( source.getName() );
					viewSourceTransforms.add( new AffineTransformation( "manualTransform", fixedTransform.getRowPackedCopy(), sources ) );
				}
			}
        }
    }

	public View createViewFromCurrentState( String uiSelectionGroup, boolean isExclusive, boolean includeViewerTransform )
	{
		List< Transformation > viewTransformations = new ArrayList<>( currentTransformations );

		// Create copies of the current displays where the
		// serializable fields are properly set
		List< Display< ? > > viewDisplays = new ArrayList<>();
		for ( Display< ? > display : currentDisplays )
		{
			if ( display instanceof ImageDisplay )
				viewDisplays.add( new ImageDisplay( ( ImageDisplay ) display ) );
			else if ( display instanceof SegmentationDisplay )
				viewDisplays.add( new SegmentationDisplay( ( SegmentationDisplay ) display ) );
			else if ( display instanceof RegionDisplay )
				viewDisplays.add( new RegionDisplay( ( RegionDisplay ) display ) );
			else if ( display instanceof SpotDisplay )
				viewDisplays.add( new SpotDisplay( ( SpotDisplay) display ) );
			else
				throw new UnsupportedOperationException( "Serialisation of a " + display.getClass().getName() + " is not yet supported." );

			// Add transforms that were done by the user in BDV,
			// using, e.g., the manual transform or BigWarp.
			// Note that other transforms are already captured
			// in the {@code viewTransformations} list.
			addManualTransforms( viewTransformations, display.sourceAndConverters() );
		}

		if ( includeViewerTransform )
		{
			final BdvHandle bdvHandle = sliceViewer.getBdvHandle();
			AffineTransform3D normalisedViewTransform = TransformHelper.createNormalisedViewerTransform( bdvHandle.getViewerPanel() );
			final NormalizedAffineViewerTransform transform = new NormalizedAffineViewerTransform( normalisedViewTransform.getRowPackedCopy(), bdvHandle.getViewerPanel().state().getCurrentTimepoint() );
			return new View(uiSelectionGroup, viewDisplays, viewTransformations, transform, isExclusive);
		}
		else
		{
			return new View( uiSelectionGroup, viewDisplays, viewTransformations, isExclusive );
		}
	}

	public void show( String view )
	{
		show( moBIE.getViews().get( view ) );
	}

	public synchronized void show( View view )
	{
		final long startTime = System.currentTimeMillis();
		IJ.log( "Opening view: " + view.getName() );

		if ( view.isExclusive() )
		{
			removeAllSourceDisplays( true );
			DataStore.clearImages();
		}

		if ( view.getViewerTransform() != null )
		{
			SliceViewLocationChanger.changeLocation( sliceViewer.getBdvHandle(), view.getViewerTransform() );
		}

		// init and transform
		initData( view );

		// display the data
		final List< Display< ? > > displays = view.getDisplays();
		for ( Display< ? > display : displays )
			show( display );

		// Adjust viewer transform to accommodate the displayed sources.
		// Note that if {@code view.getViewerTransform() != null}
		// the viewer transform has already been adjusted above.
		if ( view.getViewerTransform() == null && currentDisplays.size() > 0 && ( view.isExclusive() || currentDisplays.size() == 1 ) )
		{
			final Display< ? > display = currentDisplays.get( currentDisplays.size() - 1);
			final List< ? extends SourceAndConverter< ? > > sourceAndConverters = display.sourceAndConverters();
			final AffineTransform3D multiSourceTransform = new MoBIEViewerTransformAdjuster( sliceViewer.getBdvHandle(), sourceAndConverters.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ) ).getMultiSourceTransform();
			sliceViewer.getBdvHandle().getViewerPanel().state().setViewerTransform( multiSourceTransform );
			//new ViewerTransformAdjuster( sliceViewer.getBdvHandle(), (( AbstractDisplay< ? > ) display ).getSourceAndConverters().values().iterator().next() ).run();
		}

		// trigger rendering of source name overlay
		getSliceViewer().getSourceNameRenderer().transformChanged( sliceViewer.getBdvHandle().getViewerPanel().state().getViewerTransform() );

		// adapt time point
		if ( view.getViewerTransform() != null )
		{
			// This needs to be done after adding all the sources,
			// because otherwise the requested timepoint may not yet
			// exist in BDV
			SliceViewLocationChanger.adaptTimepoint( sliceViewer.getBdvHandle(), view.getViewerTransform() );
		}

		IJ.log("Opened view: " + view.getName() + " in " + (System.currentTimeMillis() - startTime) + " ms." );
	}

	// initialize and transform
	public void initData( View view )
	{
		// fetch names of all data sources that are
		// either to be shown or to be transformed
		final Map< String, Object > sourceToTransformOrDisplay = view.getSources();
		if ( sourceToTransformOrDisplay.size() == 0 ) return;

		// instantiate source that can be directly opened
		// ( other sources may be created later,
		// by a display or transformation )
		final List< DataSource > dataSources = moBIE.getDataSources( sourceToTransformOrDisplay.keySet() );

		for ( DataSource dataSource : dataSources )
		{
			final Object transformOrDisplay = sourceToTransformOrDisplay.get( dataSource.getName() );
			if ( transformOrDisplay instanceof MergedGridTransformation )
			{
				final MergedGridTransformation mergedGridTransformation = ( MergedGridTransformation ) transformOrDisplay;
				if ( mergedGridTransformation.metadataSource != null )
				{
					if ( dataSource.getName().equals( mergedGridTransformation.metadataSource ) )
						dataSource.preInit( true );
					else
						dataSource.preInit( false );
				}
				else // no metadata source specified, use the first in the grid as metadata source
				{
					final String firstImageInGrid = mergedGridTransformation.targetImageNames().get( 0 );
					if ( dataSource.getName().equals( firstImageInGrid ) )
						dataSource.preInit( true );
					else
						dataSource.preInit( false );
				}
			}
			else
			{
				dataSource.preInit( true );
			}
		}

		moBIE.initDataSources( dataSources );

		// transform images
		// this may create new images with new names

		// TODO Factor this out int an image transformer class

		final List< Transformation > transformations = view.getTransformations();
		if ( transformations != null )
		{
			for ( Transformation transformation : transformations )
			{
				currentTransformations.add( transformation );

				if ( transformation instanceof AffineTransformation )
				{
					final AffineTransformation< ? > affineTransformation = ( AffineTransformation< ? > ) transformation;

					final Set< Image< ? > > images = DataStore.getImageSet( transformation.targetImageNames() );

					for ( Image< ? > image : images )
					{
						final Image< ? > transformedImage =
							ImageTransformer.transform(
									image,
									affineTransformation.getAffineTransform3D(),
									affineTransformation.getTransformedImageName( image.getName() ) );
						DataStore.putImage( transformedImage );
					}
				}
				else if ( transformation instanceof CropTransformation )
				{
					final CropTransformation< ? > cropTransformation = ( CropTransformation< ? > ) transformation;
					final List< String > targetImageNames = transformation.targetImageNames();
					for ( String imageName : targetImageNames )
					{
						final CroppedImage< ? > croppedImage = new CroppedImage<>(
								DataStore.getImage( imageName ),
								cropTransformation.getTransformedImageName( imageName ),
								cropTransformation.min,
								cropTransformation.max,
								cropTransformation.centerAtOrigin );
						DataStore.putImage( croppedImage );
					}
				}
				else if ( transformation instanceof MergedGridTransformation )
				{
					final MergedGridTransformation mergedGridTransformation = ( MergedGridTransformation ) transformation;
					final List< String > targetImageNames = transformation.targetImageNames();
					final List< ? extends Image< ? > > gridImages = DataStore.getImageList( targetImageNames );

					// Fetch grid metadata image
					Image< ? > metadataImage = ( mergedGridTransformation.metadataSource == null ) ? gridImages.get( 0 ) : DataStore.getImage( mergedGridTransformation.metadataSource );

					// Create the stitched grid image
					//
					if ( gridImages.get( 0 ) instanceof AnnotatedLabelImage )
					{
						final StitchedAnnotatedLabelImage< ? extends Annotation> annotatedStitchedImage = new StitchedAnnotatedLabelImage( gridImages, metadataImage, mergedGridTransformation.positions, mergedGridTransformation.mergedGridSourceName, AbstractGridTransformation.RELATIVE_GRID_CELL_MARGIN );
						DataStore.putImage( annotatedStitchedImage );
					}
					else
					{
						final StitchedImage stitchedImage = new StitchedImage( gridImages, metadataImage, mergedGridTransformation.positions, mergedGridTransformation.mergedGridSourceName, AbstractGridTransformation.RELATIVE_GRID_CELL_MARGIN );
						DataStore.putImage( stitchedImage );
					}
				}
				else if ( transformation instanceof GridTransformation )
				{
					final GridTransformation gridTransformation = ( GridTransformation ) transformation;

					final List< List< String > > nestedSources = gridTransformation.nestedSources;
					final List< List< ? extends Image< ? > > > nestedImages = new ArrayList<>();
					for ( List< String > sources : nestedSources )
					{
						final List< ? extends Image< ? > > images = DataStore.getImageList( sources );
						nestedImages.add( images );
					}

					// The size of the tile of the grid is the size of the
					// largest union mask of the images at
					// the grid positions.
					double[] tileRealDimensions = new double[ 2 ];
					for ( List< ? extends Image< ? > > images : nestedImages )
					{
						final RealMaskRealInterval unionMask = TransformHelper.getUnionMask( images, 0 );
						final double[] realDimensions = TransformHelper.getRealDimensions( unionMask );
						for ( int d = 0; d < 2; d++ )
							tileRealDimensions[ d ] = realDimensions[ d ] > tileRealDimensions[ d ] ? realDimensions[ d ] : tileRealDimensions[ d ];
					}

					// Add a margin to the tiles
					for ( int d = 0; d < 2; d++ )
						tileRealDimensions[ d ] = tileRealDimensions[ d ] * ( 1.0 + 2 * GridTransformation.RELATIVE_GRID_CELL_MARGIN );

					// Compute the corresponding offset of where to place
					// the images within the tile
					final double[] offset = new double[ 2 ];
					for ( int d = 0; d < 2; d++ )
						offset[ d ] = tileRealDimensions[ d ] * GridTransformation.RELATIVE_GRID_CELL_MARGIN;

					final List< int[] > gridPositions = gridTransformation.positions == null ? TransformHelper.createGridPositions( nestedSources.size() ) : gridTransformation.positions;

					final List< ? extends Image< ? > > transformedImages = ImageTransformer.gridTransform( nestedImages, gridTransformation.transformedNames, gridPositions, tileRealDimensions, gridTransformation.centerAtOrigin, offset );

					DataStore.putImages( transformedImages );
				}
				else
				{
					throw new UnsupportedOperationException( "Transformations of type " + transformation.getClass().getName() + " are not yet implemented.");
				}
			}
		}

		// Instantiate images that are created by
		// RegionDisplay
		for ( Display< ? > display : view.getDisplays() )
		{
			// https://github.com/mobie/mobie-viewer-fiji/issues/818
			if ( display instanceof RegionDisplay )
			{
				// Note that the imageNames that are referred
				// to here must exist in this view.
				// Thus the {@code RegionDisplay} must be build
				// *after* the above transformations,
				// which may create new images
				// that could be referred to here.

				final RegionDisplay< ? > regionDisplay = ( RegionDisplay< ? > ) display;
				final RegionDataSource regionDataSource = ( RegionDataSource ) DataStore.getRawData( regionDisplay.tableSource );

				// Table has been loaded already during
				// initialisation of that data source
				Table table = regionDataSource.table;

				// Only keep the subset of rows that are actually needed.
				final Map< String, List< String > > regionIdToImageNames = regionDisplay.sources;
				final Set< String > regionIDs = regionIdToImageNames.keySet();
				final ArrayList< Integer > dropRows = new ArrayList<>();
				final int rowCount = table.rowCount();
				for ( int rowIndex = 0; rowIndex < rowCount; rowIndex++ )
				{
					final String regionId = table.row( rowIndex ).getObject( ColumnNames.REGION_ID ).toString();
					if ( ! regionIDs.contains( regionId ) )
						dropRows.add( rowIndex );
				}

				if ( dropRows.size() > 0 )
					table = table.dropRows( dropRows.stream().mapToInt( i -> i ).toArray() );

				final TableSawAnnotationCreator< TableSawAnnotatedRegion > annotationCreator = new TableSawAnnotatedRegionCreator( table, regionIdToImageNames );

				final TableSawAnnotationTableModel< AnnotatedRegion > tableModel = new TableSawAnnotationTableModel( display.getName(), annotationCreator, moBIE.getTableStore( regionDataSource.tableData ), TableDataFormat.DEFAULT_TSV, table );

				// REMOVE
				final ArrayList< AnnotatedRegion > annotatedRegions = tableModel.annotations();
				// FIXME This should already have the AnnotationType
				//   Probably make RegionLabelImage implement AnnotatedImage
				//   and give the AnnData in the constructor
				final DefaultAnnData< AnnotatedRegion > annData = new DefaultAnnData<>( tableModel );
				final Image< UnsignedIntType > labelImage = new RegionLabelImage( regionDisplay.getName(), annotatedRegions );

				// REMOVE
				final DefaultAnnotationAdapter< AnnotatedRegion > annotationAdapter = new DefaultAnnotationAdapter<>( annData );
				// FIXME Just DefaultAnnotatedImage will do

				// REMOVE
				final DefaultAnnotatedLabelImage regionImage = new DefaultAnnotatedLabelImage( labelImage, annData, annotationAdapter );

				DataStore.putImage( regionImage );
			}
		}
	}

	public synchronized < A extends Annotation > void show( Display< ? > display )
	{
		if ( currentDisplays.contains( display ) ) return;

		if ( display instanceof ImageDisplay )
		{
			for ( String name : display.getSources() )
				display.addImage( ( Image ) DataStore.getImage( name ) );
			showImageDisplay( ( ImageDisplay ) display );
		}
		else if ( display instanceof AnnotationDisplay )
		{
			// FIXME What about label mask images that are NOT annotated,
			//   i.e. don't have a table?
			//   Maybe implement an AnnData that lazily builds up
			//   upon browsing around?
			//   This may be tricky, because if we want to support semantic
			//   segmentations there is no anchor(), maybe still fine, but
			//   then the table would not move the slice view anywhere.

			final AnnotationDisplay< A > annotationDisplay = ( AnnotationDisplay ) display;

			// Register all images that are shown by this display.
			// This is needed for the annotationDisplay to create the annData,
			// combining the annData from all the annotated images.
			for ( String name : display.getSources() )
			{
				final Image< ? > image = DataStore.getImage( name );
				try
				{
					annotationDisplay.addImage( ( AnnotatedLabelImage ) image );
				}
				catch ( Exception e )
				{
					e.printStackTrace();
					throw new UnsupportedOperationException( "The image " + name + " does not implement AnnotatedLabelImage, but is an " + image.getClass().getName() + " and thus cannot be shown by " + annotationDisplay.getClass().getName() );
				}
			}

			// Now that all images are added to the display,
			// create an annData object,
			// potentially combining the annData from
			// several images.
			annotationDisplay.createAnnData();

			// Load additional tables (to be merged)
			final List< String > additionalTables = annotationDisplay.getAdditionalTables();
			if ( additionalTables != null )
				for ( String table : additionalTables )
				{
					final AnnotationTableModel< A > tableModel = annotationDisplay.getAnnData().getTable();
					final String dataStore = tableModel.dataStore();
					tableModel.requestTable( IOHelper.combinePath( dataStore, table ) );
				}

			// configure selection model
			//
			annotationDisplay.selectionModel = new MoBIESelectionModel<>();

			// set selected segments
			//
			final Set< String > selectedAnnotationIds = annotationDisplay.selectedAnnotationIds();
			if ( selectedAnnotationIds != null )
			{
				final Set< A > annotations = annotationDisplay.annotationAdapter().getAnnotations( selectedAnnotationIds );
				annotationDisplay.selectionModel.setSelected( annotations, true );
			}

			// configure coloring model
			//
			String lut = annotationDisplay.getLut();

			if ( LUTs.isCategorical( lut ) )
			{
				CategoricalAnnotationColoringModel< Annotation > coloringModel = ColoringModels.createCategoricalModel( annotationDisplay.getColoringColumnName(), lut, LUTs.TRANSPARENT );

				if ( LUTs.getLut( lut ) instanceof ColumnARGBLut )
				{
					// note that this currently forces loading of the table(s)
					// for big data this may need some improvement
					final AnnotationTableModel< A > table = annotationDisplay.getAnnData().getTable();
					for ( A annotation : table.annotations() )
					{
						String argbString = annotation.getValue( annotationDisplay.getColoringColumnName() ).toString();

						if ( argbString.equals("") )
							continue;

						final ARGBType argbType = ColorHelper.getArgbType( argbString );

						coloringModel.assignColor( argbString, argbType.get() );
					}
				}

				coloringModel.setRandomSeed( annotationDisplay.getRandomColorSeed() );
				annotationDisplay.coloringModel = new MobieColoringModel( coloringModel, annotationDisplay.selectionModel );
			}
			else if ( LUTs.isNumeric( lut ) )
			{
				NumericAnnotationColoringModel< Annotation > coloringModel
						= ColoringModels.createNumericModel(
								annotationDisplay.getColoringColumnName(),
								lut,
								annotationDisplay.getValueLimits(),
						 true
							);

				annotationDisplay.coloringModel = new MobieColoringModel( coloringModel, annotationDisplay.selectionModel );
			}
			else
			{
				throw new UnsupportedOperationException("Coloring LUT " + lut + " is not supported.");
			}

			// show in slice viewer
			//
			annotationDisplay.sliceViewer = sliceViewer;
			annotationDisplay.sliceView = new AnnotationSliceView<>( moBIE, annotationDisplay );
			initTableView( annotationDisplay );
			initScatterPlotView( annotationDisplay );
			if ( annotationDisplay instanceof SegmentationDisplay )
				initSegmentVolumeViewer( ( SegmentationDisplay ) annotationDisplay );
		}

		userInterface.addSourceDisplay( display );
		currentDisplays.add( display );
	}

	public synchronized void removeAllSourceDisplays( boolean closeImgLoader )
	{
		// create a copy of the currently shown displays...
		final ArrayList< Display > currentDisplays = new ArrayList<>( this.currentDisplays ) ;

		// ...such that we can remove the displays without
		// modifying the list that we iterate over
		for ( Display display : currentDisplays )
		{
			// removes display from all viewers and
			// also from the list of currently shown sourceDisplays
			// also close all ImgLoaders to free the cache
			removeDisplay( display, closeImgLoader );
		}
	}

	private void showImageDisplay( ImageDisplay imageDisplay )
	{
		imageDisplay.sliceViewer = sliceViewer;
		imageDisplay.imageSliceView = new ImageSliceView( moBIE, imageDisplay );
		initImageVolumeViewer( imageDisplay );
	}

	// compare with initSegmentationVolumeViewer
	private void initImageVolumeViewer( ImageDisplay< ? > imageDisplay )
	{
		imageDisplay.imageVolumeViewer = new ImageVolumeViewer( imageDisplay.sourceAndConverters(), universeManager );
		Double[] resolution3dView = imageDisplay.getResolution3dView();
		if ( resolution3dView != null ) {
			imageDisplay.imageVolumeViewer.setVoxelSpacing( ArrayUtils.toPrimitive(imageDisplay.getResolution3dView() ));
		}
		imageDisplay.imageVolumeViewer.showImages( imageDisplay.showImagesIn3d() );

		final Collection< ? extends SourceAndConverter< ? > > sourceAndConverters = imageDisplay.sourceAndConverters();
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
			sacService.setMetadata( sourceAndConverter, ImageVolumeViewer.class.getName(), imageDisplay.imageVolumeViewer );

	}

	private void initTableView( AnnotationDisplay< ? extends Annotation > display )
	{
		display.tableView = new TableView( display );
		// Note that currently we must show the table here
		// in order to instantiate the window.
		// This window is needed in {@code UserInterfaceHelper}
		// in the function {@code createWindowVisibilityCheckbox},
		// in which the table window will be
		// hidden, if {@code display.showTable == false}.
		display.tableView.show();
		setTablePosition( display.sliceViewer.getWindow(), display.tableView.getWindow() );
		display.selectionModel.listeners().add( display.tableView );
		display.coloringModel.listeners().add( display.tableView );
		numViewedTables++;
	}

	private void setTablePosition( Window reference, Window table )
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int shift = screenSize.height / 20;
		SwingUtilities.invokeLater( () -> WindowArrangementHelper.bottomAlignWindow( reference, table, ( numViewedTables - 1 ) * shift ) );
	}

	private void initSegmentVolumeViewer( SegmentationDisplay< ? extends AnnotatedSegment > display )
	{
		display.segmentsVolumeViewer = new SegmentsVolumeViewer( display.selectionModel, display.coloringModel, display.getImages(), universeManager );
		Double[] resolution3dView = display.getResolution3dView();
		if ( resolution3dView != null ) {
			display.segmentsVolumeViewer.setVoxelSpacing( ArrayUtils.toPrimitive( display.getResolution3dView() ) );
		}
		display.segmentsVolumeViewer.showSegments( display.showSelectedSegmentsIn3d(), true );
		display.coloringModel.listeners().add( display.segmentsVolumeViewer );
		display.selectionModel.listeners().add( display.segmentsVolumeViewer );
	}

	public synchronized void removeDisplay( Display display, boolean closeImgLoader )
	{
		if ( display instanceof AnnotationDisplay )
		{
			final AnnotationDisplay< ? > annotationDisplay = ( AnnotationDisplay< ? > ) display;
			annotationDisplay.sliceView.close( closeImgLoader );

			if ( annotationDisplay.tableView != null )
			{
				numViewedTables--;
				annotationDisplay.tableView.close();
				annotationDisplay.scatterPlotView.close();
				if ( annotationDisplay instanceof SegmentationDisplay )
					( ( SegmentationDisplay ) annotationDisplay ).segmentsVolumeViewer.close();
			}

		}
		else if ( display instanceof ImageDisplay )
		{
			final ImageDisplay imageDisplay = ( ImageDisplay ) display;
			imageDisplay.imageSliceView.close( false );
		}

		userInterface.removeDisplaySettingsPanel( display );
		currentDisplays.remove( display );

		updateCurrentSourceTransformers();
	}

	private void updateCurrentSourceTransformers()
	{
		// remove any sourceTransformers, where none of its relevant sources are displayed

		// create a copy of the currently shown source transformers, so we don't iterate over a list that we modify
		final ArrayList< Transformation > imageTransformersCopy = new ArrayList<>( this.currentTransformations ) ;

		Set<String> currentlyDisplayedSources = new HashSet<>();
		for ( Display display: currentDisplays )
			currentlyDisplayedSources.addAll( display.getSources() );

		for ( Transformation imageTransformation : imageTransformersCopy )
		{
			if ( ! currentlyDisplayedSources.stream().anyMatch( s -> imageTransformation.targetImageNames().contains( s ) ) )
				currentTransformations.remove( imageTransformation );
		}
	}

	// TODO: typing ( or remove )
	public Collection< AnnotationDisplay< ? > > getAnnotationDisplays()
	{
		final List< AnnotationDisplay< ? > > collect = getCurrentSourceDisplays().stream().filter( s -> s instanceof AnnotationDisplay ).map( s -> ( AnnotationDisplay< ? > ) s ).collect( Collectors.toList() );

		return collect;
	}

	public void close()
	{
		IJ.log( "Closing BDV..." );
		removeAllSourceDisplays( true );
		sliceViewer.getBdvHandle().close();
		IJ.log( "Closing 3D Viewer..." );
		universeManager.close();
		IJ.log( "Closing UI..." );
		userInterface.close();
	}
}

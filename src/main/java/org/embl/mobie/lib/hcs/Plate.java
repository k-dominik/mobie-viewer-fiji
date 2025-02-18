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
package org.embl.mobie.lib.hcs;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.measure.Calibration;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.io.TPosition;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

public class Plate
{
	private final String hcsDirectory;
	private HCSPattern hcsPattern;
	private HashMap< Channel, Map< Well, Set< Site > > > channelWellSites;
	private double[] siteRealDimensions;
	private int sitesPerWell;
	private int[] siteDimensions;
	private VoxelDimensions voxelDimensions;
	private Set< TPosition > tPositions;
	private int wellsPerPlate;
	private ImageDataFormat imageDataFormat;
	private OperettaMetadata metadata;

	public Plate( String hcsDirectory ) throws IOException
	{
		this.hcsDirectory = hcsDirectory;

		final List< String > imageSitePaths;
		if ( hcsDirectory.endsWith( ".zarr" ) )
		{
			final int minDepth = 3;
			final int maxDepth = 3;
			final Path rootPath = Paths.get(hcsDirectory);
			final int rootPathDepth = rootPath.getNameCount();
			imageSitePaths = Files.walk( rootPath, maxDepth )
					.filter( e -> e.toFile().isDirectory() )
					.filter( e -> e.getNameCount() - rootPathDepth >= minDepth )
					.map( e -> e.toString() )
					.collect( Collectors.toList() );
		}
		else
		{
			imageSitePaths = Files.walk( Paths.get( hcsDirectory ), 3 ).map( p -> p.toString() ).collect( Collectors.toList() );
		}

		hcsPattern = determineHCSPattern( hcsDirectory, imageSitePaths );

		if ( hcsPattern == HCSPattern.Operetta )
		{
			//final File xml = new File( hcsDirectory, "Index.idx.xml" );
			final File xml = new File( hcsDirectory, "Index.xml" );
			metadata = new OperettaMetadata( xml );
		}

		buildPlateMap( imageSitePaths );
	}

	private void buildPlateMap( List< String > paths )
	{
		channelWellSites = new HashMap<>();
		tPositions = new HashSet<>();
		int numImages = 0;

		IJ.log("Files: " + paths.size() );

		for ( String path : paths )
		{
			if ( ! hcsPattern.setPath( path ) )
				continue;

			if ( metadata != null )
			{
				if ( ! metadata.contains( path ) )
				{
					IJ.log( "[WARNING] No metadata found for " + path );
					continue;
				}
			}

			numImages++;
			//System.out.println( path );

			// store this file's channel, well, and site
			//

			// TODO: https://github.com/mobie/mobie-viewer-fiji/issues/972
			//    WellH06_PointH06_0007_ChannelDAPI,WF_GFP,TRITC,WF_Cy5,DIA_Seq0502.tiff
			//    Maybe change to hcsPattern.getChannels(); (plural) ?
			//    and then loop through the channels

			// channel
			//
			String channelGroup = hcsPattern.getChannelGroup();
			Channel channel = getChannel( channelWellSites, channelGroup );
			if ( channel == null )
			{
				// configure channel properties
				//

				channel = new Channel( channelGroup );
				channelWellSites.put( channel, new HashMap<>() );

				// TODO: implement this properly
				if ( imageDataFormat == null )
				{
					imageDataFormat = ImageDataFormat.fromPath( path );
					IJ.log( "Image data format: " + imageDataFormat.toString() );
				}

				ImagePlus imagePlus;
				if ( imageDataFormat.equals( ImageDataFormat.Tiff ) )
				{
					final File file = new File( path );
					imagePlus = ( new Opener() ).openTiff( file.getParent(), file.getName() );
				}
				else
				{
					imagePlus = IJ.openImage( path );
				}
				final String color = ColorHelper.getString( imagePlus.getLuts()[ 0 ] );
				channel.setColor( color );
				final double[] contrastLimits = new double[]
				{
					imagePlus.getDisplayRangeMin(),
					imagePlus.getDisplayRangeMax()
				};
				channel.setContrastLimits( contrastLimits );

				if ( voxelDimensions == null )
				{
					// set spatial calibrations for the whole plate
					//
					if ( metadata == null )
					{
						final Calibration calibration = imagePlus.getCalibration();
						voxelDimensions = new FinalVoxelDimensions( calibration.getUnit(), calibration.pixelWidth, calibration.pixelHeight, calibration.pixelDepth );
					}
					else
					{
						voxelDimensions = metadata.getVoxelDimensions( path );
					}

					siteRealDimensions = new double[]
					{
							imagePlus.getWidth() * voxelDimensions.dimension( 0 ),
							imagePlus.getHeight() * voxelDimensions.dimension( 1 )
					};

					siteRealDimensions = new double[]
					{
							imagePlus.getWidth() * voxelDimensions.dimension( 0 ),
							imagePlus.getHeight() * voxelDimensions.dimension( 1 )
					};

					siteDimensions = new int[]
					{
							imagePlus.getWidth(),
							imagePlus.getHeight()
					};
				}
			}

			// well
			//
			String wellGroup = hcsPattern.getWellGroup();
			Well well = getWell( channelWellSites, channel, wellGroup );
			if ( well == null )
			{
				well = new Well( wellGroup );
				channelWellSites.get( channel ).put( well, new HashSet<>() );
				final int numWells = channelWellSites.get( channel ).size();
				if ( numWells > wellsPerPlate )
					wellsPerPlate = numWells;
			}

			// site
			//
			final String siteGroup = hcsPattern.getSiteGroup();
			Site site = getSite( channelWellSites, channel, well, siteGroup );
			if ( site == null )
			{
				site = new Site( siteGroup, imageDataFormat );
				site.setDimensions( siteDimensions );
				site.setVoxelDimensions( voxelDimensions );
				channelWellSites.get( channel ).get( well ).add( site );
				final int numSites = channelWellSites.get( channel ).get( well ).size();
				if ( numSites > sitesPerWell )
					sitesPerWell = numSites; // needed to compute the site position within a well
			}

			final String t = hcsPattern.getT();
			final String z = hcsPattern.getZ();
			site.addPath( t, z, path );

			tPositions.add( new TPosition( t ) );
		}

		IJ.log( "Initialised HCS plate: " + getName() );
		IJ.log( "Images: " + numImages );
		IJ.log( "Channels: " + channelWellSites.keySet().size() );
		IJ.log( "Wells: " + wellsPerPlate );
		IJ.log( "Sites per well: " + sitesPerWell );
	}

	private Channel getChannel( HashMap< Channel, Map< Well, Set< Site > > > channelWellSites, String channelName )
	{
		try
		{
			return channelWellSites.keySet().stream().filter( c -> c.getName().equals( channelName ) ).findFirst().get();
		}
		catch ( NoSuchElementException e )
		{
			return null;
		}

	}

	private Well getWell( HashMap< Channel, Map< Well, Set< Site > > > channelWellSites, Channel channel, String wellName )
	{
		try
		{
			return channelWellSites.get( channel ).keySet().stream().filter( w -> w.getName().equals( wellName ) ).findFirst().get();
		}
		catch ( NoSuchElementException e )
		{
			return null;
		}
	}

	private Site getSite( HashMap< Channel, Map< Well, Set< Site > > > channelWellSites, Channel channel, Well well, String siteName )
	{
		try
		{
			return channelWellSites.get( channel ).get( well ).stream().filter( s -> s.getName().equals( siteName ) ).findFirst().get();
		}
		catch ( NoSuchElementException e )
		{
			return null;
		}
	}

	private HCSPattern determineHCSPattern( String hcsDirectory, List< String > paths )
	{
		for ( String path : paths )
		{
			final HCSPattern hcsPattern = HCSPattern.fromPath( path );
			if ( hcsPattern != null )
			{
				IJ.log( "HCS Pattern: " + hcsPattern );
				return hcsPattern;
			}
		}

		throw new RuntimeException( "Could not determine HCSPattern for " + hcsDirectory );
	}

	public Set< Channel > getChannels()
	{
		return channelWellSites.keySet();
	}

	public Set< Well > getWells( Channel channel )
	{
		return channelWellSites.get( channel ).keySet();
	}

	public Set< Site > getSites( Channel channel, Well well )
	{
		return channelWellSites.get( channel ).get( well );
	}

	public int[] getGridPosition( Site site )
	{
		switch ( hcsPattern )
		{
			case Operetta:
				return getOperettaGridPosition( site );
			case MolecularDevices:
				return getMolecularDevicesGridPosition( site );
			default:
				return getDefaultGridPosition( site );
		}
	}

	public int[] getMolecularDevicesGridPosition( Site site )
	{
		if ( sitesPerWell == 1 )
			return new int[]{ 0, 0 };

		int siteIndex = Integer.parseInt( site.getName() ) - 1;
		int numColumns = (int) Math.ceil( Math.sqrt( sitesPerWell ) );

		int[] gridPosition = new int[ 2 ];
		gridPosition[ 0 ] = siteIndex % numColumns; // column
		gridPosition[ 1 ] = siteIndex / numColumns; // row

		return gridPosition;
	}

	private int[] getOperettaGridPosition( Site site )
	{
		final String path = site.getPaths().values().iterator().next().values().iterator().next();
		final double[] realPosition = metadata.getRealPosition( path );

		final int[] position = new int[ 2 ];
		for ( int d = 0; d < 2; d++ )
		{
			position[ d ] = (int) ( realPosition[ d ] / siteRealDimensions [ d ] );
		}

		return position;
	}

	public int[] getDefaultGridPosition( Site site )
	{
		if ( sitesPerWell == 1 )
			return new int[]{ 0, 0 };

		int siteIndex = Integer.parseInt( site.getName() ) - 1;
		int numSiteColumns = (int) Math.sqrt( sitesPerWell );

		int[] gridPosition = new int[ 2 ];
		gridPosition[ 0 ] = siteIndex % numSiteColumns; // column
		gridPosition[ 1 ] = siteIndex / numSiteColumns; // row

		return gridPosition;
	}

	public int[] getWellGridPosition( Well well )
	{
		return hcsPattern.decodeWellGridPosition( well.getName() );
	}

	public boolean is2D()
	{
		return true; // for now...
	}

	public String getName()
	{
		return new File( hcsDirectory ).getName();
	}

	public HCSPattern getHcsPattern()
	{
		return hcsPattern;
	}

	public double[] getSiteRealDimensions()
	{
		return siteRealDimensions;
	}

	public int[] getSiteDimensions()
	{
		return siteDimensions;
	}

	public Set< TPosition > getTPositions()
	{
		return tPositions;
	}

	public int getSitesPerWell()
	{
		return sitesPerWell;
	}
}

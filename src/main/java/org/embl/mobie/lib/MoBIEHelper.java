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
package org.embl.mobie.lib;

import bdv.SpimSource;
import ij.IJ;
import ij.ImagePlus;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.Dimensions;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.lib.source.Metadata;
import org.embl.mobie.lib.source.SourceToImagePlusConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class MoBIEHelper
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static int[] asInts( long[] longs) {
		int[] ints = new int[longs.length];

		for(int i = 0; i < longs.length; ++i)
			ints[i] = (int)longs[i];

		return ints;
	}

	public static long[] asLongs( int[] ints) {
		long[] longs = new long[ints.length];

		for(int i = 0; i < longs.length; ++i)
			longs[i] = ints[i];

		return longs;
	}

	// from https://www.geeksforgeeks.org/print-longest-common-substring/
	public static String longestCommonSubstring( String s1, String s2 )
	{
		int m = s1.length();
		int n = s2.length();

		// Create a table to store lengths of longest common
		// suffixes of substrings.   Note that LCSuff[i][j]
		// contains length of longest common suffix of X[0..i-1]
		// and Y[0..j-1]. The first row and first column entries
		// have no logical meaning, they are used only for
		// simplicity of program
		int[][] LCSuff = new int[m + 1][n + 1];

		// To store length of the longest common substring
		int len = 0;

		// To store the index of the cell which contains the
		// maximum value. This cell's index helps in building
		// up the longest common substring from right to left.
		int row = 0, col = 0;

	   /* Following steps build LCSuff[m+1][n+1] in bottom
	   up fashion. */
		for (int i = 0; i <= m; i++) {
			for (int j = 0; j <= n; j++) {
				if (i == 0 || j == 0)
					LCSuff[i][j] = 0;

				else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
					LCSuff[i][j] = LCSuff[i - 1][j - 1] + 1;
					if (len < LCSuff[i][j]) {
						len = LCSuff[i][j];
						row = i;
						col = j;
					}
				}
				else
					LCSuff[i][j] = 0;
			}
		}

		// if true, then no common substring exists
		if (len == 0) {
			System.out.println("No Common Substring");
			return s1;
		}

		// allocate space for the longest common substring
		String resultStr = "";

		// traverse up diagonally form the (row, col) cell
		// until LCSuff[row][col] != 0
		while (LCSuff[row][col] != 0) {
			resultStr = s1.charAt(row - 1) + resultStr; // or Y[col-1]
			--len;

			// move diagonally up to previous cell
			row--;
			col--;
		}

		//  longest common substring
		return resultStr;
	}

	public static ImagePlus openWithBioFormats( String path, int seriesIndex )
	{
		try
		{
			ImporterOptions opts = new ImporterOptions();
			opts.setId( path );
			opts.setVirtual( true );
			opts.setSeriesOn( seriesIndex, true );
			ImportProcess process = new ImportProcess( opts );
			process.execute();
			ImagePlusReader impReader = new ImagePlusReader( process );
			ImagePlus[] imps = impReader.openImagePlus();
			return imps[ 0 ];
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return null;
		}
	}

	public static boolean is2D( AbstractSpimData< ? > spimData, int setupIndex )
	{
		final Dimensions size = spimData.getSequenceDescription().getViewSetupsOrdered().get( setupIndex ).getSize();
		return size.dimension( 2 ) == 1;
	}

	public static List< String > getGroupNames( String regex )
	{
		List< String > namedGroups = new ArrayList<>();

		Matcher m = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher( regex );

		while ( m.find() ) {
			namedGroups.add(m.group(1));
		}

		return namedGroups;
	}

	public static Metadata getMetadataFromImageFile( String path )
	{
		if ( path.contains( ".zarr" ) )
		{
			final int setupID = 0;
			final ImagePlus imagePlus = openOMEZarrAsImagePlus( path, setupID );
			final Metadata metadata = new Metadata();
			metadata.color = "White";
			metadata.contrastLimits = new double[]{ imagePlus.getDisplayRangeMin(), imagePlus.getDisplayRangeMax() };
			metadata.numTimePoints = imagePlus.getNFrames();
			return metadata;
		}
		else
		{
			final ImagePlus imagePlus = IJ.openVirtual( path );
			final Metadata metadata = new Metadata();
			metadata.color = "White";
			metadata.contrastLimits = new double[]{ imagePlus.getDisplayRangeMin(), imagePlus.getDisplayRangeMax() };
			metadata.numTimePoints = imagePlus.getNFrames();
			return metadata;
		}
	}

	public static ImagePlus openOMEZarrAsImagePlus( String path, int setupID )
	{
		try
		{
			AbstractSpimData< ? > spimData = new SpimDataOpener().open( path, ImageDataFormat.OmeZarr );
			final SpimSource< ? > spimSource = new SpimSource( spimData, setupID, "" );
			final ImagePlus imagePlus = new SourceToImagePlusConverter<>( spimSource ).getImagePlus( 0 );
			return imagePlus;
		} catch ( SpimDataException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}



	public enum FileLocation
	{
		Project,
		FileSystem
	}
}

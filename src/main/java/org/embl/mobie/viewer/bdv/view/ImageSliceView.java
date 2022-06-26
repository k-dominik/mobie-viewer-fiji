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
package org.embl.mobie.viewer.bdv.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.tables.color.ColorUtils;
import ij.IJ;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.display.ImageDisplay;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.bdv.utils.converters.RandomARGBConverter.goldenRatio;

public class ImageSliceView extends AbstractSliceView
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final ImageDisplay< ? > display;

	public ImageSliceView( MoBIE moBIE, ImageDisplay< ? > display )
	{
		super( moBIE, display );
		this.display = display;
		show();
	}

	private void show( )
	{
		Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter = new HashMap<>();
		for ( String name : display.getSources() )
		{
			sourceNameToSourceAndConverter.put( name, moBIE.sourceNameToSourceAndConverter().get( name ) );
		}

		for ( String name : sourceNameToSourceAndConverter.keySet() )
		{
			SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( name );

			adaptColor( sourceAndConverter );

			// below command will configure opacity,
			// blending mode and visibility
			display.sliceViewer.show( sourceAndConverter, display );

			adaptContrastLimits( sourceAndConverter );
		}
	}

	private void adaptContrastLimits( SourceAndConverter< ? > sourceAndConverter )
	{
		final ConverterSetup converterSetup = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sourceAndConverter );
		converterSetup.setDisplayRange( display.getContrastLimits()[ 0 ], display.getContrastLimits()[ 1 ] );
	}


	private void adaptColor( SourceAndConverter< ? > sourceAndConverter )
	{
		if ( display.getColor() != null )
		{
			final String color = display.getColor();

			if ( color.equals( "randomFromGlasbey" ) )
			{
				final GlasbeyARGBLut glasbeyARGBLut = new GlasbeyARGBLut();
				double random = sourceAndConverter.getSpimSource().getName().hashCode() * goldenRatio;
				random = random - ( long ) Math.floor( random );
				final int argb = glasbeyARGBLut.getARGB( random );
				new ColorChanger( sourceAndConverter, new ARGBType( argb ) ).run();
			}
			else
			{
				final ARGBType argbType = ColorUtils.getARGBType( color );
				if ( argbType == null )
				{
					IJ.log( "[WARN] Could not parse color: " + color );
				} else
				{
					new ColorChanger( sourceAndConverter, argbType ).run();
				}
			}
		}
	}
}

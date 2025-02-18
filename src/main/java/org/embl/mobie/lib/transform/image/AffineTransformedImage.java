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
package org.embl.mobie.lib.transform.image;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.lib.image.DefaultSourcePair;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.SourcePair;

public class AffineTransformedImage< T > implements Image< T >
{
	protected final AffineTransform3D affineTransform3D;
	protected final Image< T > image;
	protected final String transformedImageName;
	private RealMaskRealInterval mask;

	public AffineTransformedImage( Image< T > image )
	{
		this.image = image;
		this.transformedImageName = image.getName();
		this.affineTransform3D = new AffineTransform3D();
	}

	public AffineTransformedImage( Image< T > image, String transformedImageName, AffineTransform3D affineTransform3D )
	{
		this.image = image;
		this.transformedImageName = transformedImageName;
		this.affineTransform3D = affineTransform3D;
	}

	public AffineTransform3D getAffineTransform3D()
	{
		return affineTransform3D;
	}

	@Override
	public synchronized SourcePair< T > getSourcePair()
	{
		final SourcePair< T > sourcePair = image.getSourcePair();
		final Source< T > source = sourcePair.getSource();
		final Source< ? extends Volatile< T > > volatileSource = sourcePair.getVolatileSource();

		final TransformedSource transformedSource = new TransformedSource( source, transformedImageName );
		transformedSource.setFixedTransform( affineTransform3D );
		final TransformedSource volatileTransformedSource = new TransformedSource( volatileSource, transformedSource );

		return new DefaultSourcePair<>( transformedSource, volatileTransformedSource );
	}

	@Override
	public String getName()
	{
		return transformedImageName;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		this.affineTransform3D.preConcatenate( affineTransform3D );
	}

	@Override
	public RealMaskRealInterval getMask( )
	{
		if ( mask == null )
			return image.getMask().transform( affineTransform3D.inverse() );
		else
			return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		this.mask = mask;
	}
}

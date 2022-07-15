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
package mobie3.viewer.display;

import bdv.viewer.SourceAndConverter;
import mobie3.viewer.bdv.render.BlendingMode;
import mobie3.viewer.bdv.view.SliceViewer;
import mobie3.viewer.source.AnnotatedImage;
import mobie3.viewer.source.AnnotationType;
import mobie3.viewer.source.Image;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractDisplay< T > implements Display< T >
{
	// Serialization
	protected String name;
	protected double opacity = 1.0;
	protected boolean visible = true;
	protected BlendingMode blendingMode;

	// Runtime
	public transient Map< String, SourceAndConverter< T > > nameToSourceAndConverter = new HashMap<>();
	private transient Set< Image< T > > images = new HashSet<>();
	public transient SliceViewer sliceViewer;

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public double getOpacity()
	{
		return opacity;
	}

	@Override
	public boolean isVisible() { return visible; }

	@Override
	public BlendingMode getBlendingMode()
	{
		return BlendingMode.Sum;
	}

	@Override
	public Set< Image< T > > getImages()
	{
		return images;
	}

	@Override
	public void addImage( Image< T > image )
	{
		images.add( image );
	}

}

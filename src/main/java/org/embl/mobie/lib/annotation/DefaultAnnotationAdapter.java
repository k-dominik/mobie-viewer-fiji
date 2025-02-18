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

import org.embl.mobie.lib.table.AnnData;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DefaultAnnotationAdapter< A extends Annotation > implements AnnotationAdapter< A >
{
	private final AtomicBoolean throwError = new AtomicBoolean( true );
	private final AnnData< A > annData;
	private Map< String, A > uuidToAnnotation; // FIXME this should go somewhere else!
	private Map< String, A > stlToAnnotation; // source, timepoint, label

	public DefaultAnnotationAdapter( AnnData< A > annData )
	{
		this.annData = annData;
	}

	@Override
	public A createVariable()
	{
		// TODO
		//  is this OK?
		//  or do we need to create a copy of that?
		return annData.getTable().annotation( 0 );
	}

	// UUID for de-serialisation of selected segments
	// https://github.com/mobie/mobie-viewer-fiji/issues/827
	@Override
	public A getAnnotation( String uuid )
	{
		if ( uuidToAnnotation == null )
		{
			uuidToAnnotation = new ConcurrentHashMap<>();
			final Iterator< A > iterator = annData.getTable().annotations().iterator();
			while( iterator.hasNext() )
			{
				A annotation = iterator.next();
				uuidToAnnotation.put( annotation.uuid(), annotation );
			}
		}

		return uuidToAnnotation.get( uuid );
	}

	// This is for mapping for voxels within an
	// {@code AnnotatedLabelSource}
	// to the corresponding annotation.
	@Override
	public synchronized A getAnnotation( String source, int timePoint, int label )
	{
		if ( label == 0 )
		{
			// 0 is the background label
			// null is the background annotation
			return null ;
		}

		// TODO the fact that this method currently is synchronized may cause
		//   rendering in BDV effectively single threaded!
		//   In theory, once stlToAnnotation is initialised this does
		//   not need to be synchronised anymore; but I did not figure out
		//   yet how to fix concurrency issues.
		if ( stlToAnnotation == null )
			initMapping();

		final String stl = stlKey( source, timePoint, label );
		final A annotation = stlToAnnotation.get( stl );

		if ( annotation == null )
		{
			if ( throwError.get() )
			{
				System.err.println( "AnnotationAdapter: Missing annotation: " + source+ "; time point = " + timePoint + "; label = " + label + "\nSuppressing further errors of that kind." );
				System.err.println( "AnnotationAdapter: Suppressing further errors of that kind.");
			}

			throwError.set( false ); // Not to crash the system by too many Serr prints
		}

		return annotation;
	}

	private void initMapping()
	{
		stlToAnnotation = new ConcurrentHashMap<>();
		final Iterator< A > iterator = annData.getTable().annotations().iterator();
		while( iterator.hasNext() )
		{
			A annotation = iterator.next();
			stlToAnnotation.put( stlKey( annotation.source(), annotation.timePoint(), annotation.label() ), annotation );
		}
	}

	private String stlKey( String source, int timePoint, int label )
	{
		return source + ";" + timePoint + ";" + label;
	}

	@Override
	public Set< A > getAnnotations( Set< String > uuids )
	{
		return uuids.stream().map( uuid -> getAnnotation( uuid ) ).collect( Collectors.toSet() );
	}
}

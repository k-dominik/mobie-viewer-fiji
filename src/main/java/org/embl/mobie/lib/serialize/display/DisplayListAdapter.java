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
package org.embl.mobie.lib.serialize.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import org.embl.mobie.lib.serialize.JsonHelper;
import org.embl.mobie.lib.serialize.transformation.Transformation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DisplayListAdapter implements JsonSerializer< List< Display< ? > > >, JsonDeserializer< List< Display< ? > > >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("imageDisplay", ImageDisplay.class);
		classToName.put( ImageDisplay.class.getName(), "imageDisplay");
		nameToClass.put("segmentationDisplay", SegmentationDisplay.class);
		classToName.put( SegmentationDisplay.class.getName(), "segmentationDisplay");
		nameToClass.put("regionDisplay", RegionDisplay.class);
		classToName.put( RegionDisplay.class.getName(), "regionDisplay");
		nameToClass.put("spotDisplay", SpotDisplay.class);
		classToName.put( SpotDisplay.class.getName(), "spotDisplay");
	}

	@Override
	public List< Display< ? > > deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		List list = new ArrayList< Transformation >();
		JsonArray ja = json.getAsJsonArray();

		for (JsonElement je : ja)
			list.add( JsonHelper.createObjectFromJsonValue( context, je, nameToClass ) );

		return list;
	}

	@Override
	public JsonElement serialize( List< Display< ? > > displays, Type type, JsonSerializationContext context ) {
		JsonArray ja = new JsonArray();
		for ( Display< ? > display : displays )
		{
			Map< String, Display< ? > > nameToSourceDisplay = new HashMap<>();
			nameToSourceDisplay.put( classToName.get( display.getClass().getName() ), display );

			if ( display instanceof ImageDisplay ) {
				ja.add( context.serialize( nameToSourceDisplay, new TypeToken< Map< String, ImageDisplay > >() {}.getType() ) );
			} else if ( display instanceof SegmentationDisplay ) {
				ja.add( context.serialize( nameToSourceDisplay , new TypeToken< Map< String, SegmentationDisplay > >() {}.getType() ) );
			} else if ( display instanceof RegionDisplay ) {
				ja.add( context.serialize( nameToSourceDisplay, new TypeToken< Map< String, RegionDisplay > >() {}.getType() ) );
			}
			else if ( display instanceof SpotDisplay ) {
				ja.add( context.serialize( nameToSourceDisplay, new TypeToken< Map< String, SpotDisplay > >() {}.getType() ) );
			}
			else
			{
				throw new UnsupportedOperationException( "Could not serialise SourceDisplay of type: " + display.getClass() );
			}
		}

		return ja;
	}
}

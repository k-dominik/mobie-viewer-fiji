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
package org.embl.mobie.lib.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.command.context.BigWarpRegistrationCommand;
import org.embl.mobie.command.context.ConfigureLabelRenderingCommand;
import org.embl.mobie.command.context.ManualRegistrationCommand;
import org.embl.mobie.command.context.ScreenShotMakerCommand;
import org.embl.mobie.command.context.ShowRasterImagesCommand;
import org.embl.mobie.command.context.SourceInfoLoggerCommand;
import org.embl.mobie.command.view.ViewerTransformLoggerCommand;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.annotation.SliceViewAnnotationSelector;
import org.embl.mobie.lib.bdv.MobieBdvSupplier;
import org.embl.mobie.lib.bdv.MobieSerializableBdvOptions;
import org.embl.mobie.lib.bdv.ImageNameOverlay;
import org.embl.mobie.lib.bdv.SourcesAtMousePositionSupplier;
import org.embl.mobie.lib.bdv.blend.AccumulateAlphaBlendingProjectorARGB;
import org.embl.mobie.lib.bdv.blend.BlendingMode;
import org.embl.mobie.lib.color.OpacityHelper;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.serialize.display.AbstractDisplay;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.ui.WindowArrangementHelper;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SliceViewer
{
	public static final String UNDO_SEGMENT_SELECTIONS = "Undo Segment Selections [ Ctrl Shift N ]";
	public static final String LOAD_ADDITIONAL_VIEWS = "Load Additional Views";
	public static final String SAVE_CURRENT_SETTINGS_AS_VIEW = "Save Current View";
	public static final String FRAME_TITLE = "MoBIE BigDataViewer";
	public static boolean tileRenderOverlay = false;

	private final SourceAndConverterBdvDisplayService sacDisplayService;
	private BdvHandle bdvHandle;
	private final MoBIE moBIE;
	private final boolean is2D;
	private final ArrayList< String > projectCommands;

	private SourceAndConverterContextMenuClickBehaviour contextMenu;
	private final SourceAndConverterService sacService;
	private ImageNameOverlay imageNameOverlay;

	public SliceViewer( MoBIE moBIE, boolean is2D )
	{
		this.moBIE = moBIE;
		this.is2D = is2D;
		this.projectCommands = moBIE.getProjectCommands();

		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
		sacDisplayService = SourceAndConverterServices.getBdvDisplayService();

		bdvHandle = getBdvHandle();

		if ( tileRenderOverlay )
		{
			bdvHandle.getViewerPanel().showDebugTileOverlay();
			tileRenderOverlay = false; // don't show twice
		}

		sacDisplayService.registerBdvHandle( bdvHandle );

		imageNameOverlay = new ImageNameOverlay( bdvHandle, false, this );

		installContextMenuAndKeyboardShortCuts();

		WindowArrangementHelper.rightAlignWindow( moBIE.getUserInterface().getWindow(), SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() ), true, true );

	}

	public ImageNameOverlay getImageNameOverlay()
	{
		return imageNameOverlay;
	}

	public synchronized BdvHandle getBdvHandle()
	{
		if ( bdvHandle == null )
		{
			bdvHandle = createBdv( is2D, FRAME_TITLE );
			sacDisplayService.registerBdvHandle( bdvHandle );
			AccumulateAlphaBlendingProjectorARGB.bdvHandle = bdvHandle;
		}

		return bdvHandle;
	}

	private void installContextMenuAndKeyboardShortCuts( )
	{
		final SliceViewAnnotationSelector sliceViewAnnotationSelector = new SliceViewAnnotationSelector( bdvHandle, is2D, () -> moBIE.getViewManager().getAnnotationDisplays() );

		sacService.registerAction( UNDO_SEGMENT_SELECTIONS, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			sliceViewAnnotationSelector.clearSelection();
		} );

		sacService.registerAction( LOAD_ADDITIONAL_VIEWS, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			moBIE.getViewManager().getAdditionalViewsLoader().loadAdditionalViewsDialog();
		} );

		sacService.registerAction( SAVE_CURRENT_SETTINGS_AS_VIEW, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			moBIE.getViewManager().getViewsSaver().saveCurrentSettingsAsViewDialog();
		} );

		final Set< String > actionsKeys = sacService.getActionsKeys();

		final ArrayList< String > actions = new ArrayList< String >();
		actions.add( sacService.getCommandName( ScreenShotMakerCommand.class ) );
		actions.add( sacService.getCommandName( ShowRasterImagesCommand.class ) );
		actions.add( sacService.getCommandName( ViewerTransformLoggerCommand.class ) );
		actions.add( sacService.getCommandName( SourceInfoLoggerCommand.class ) );
		actions.add( sacService.getCommandName( BigWarpRegistrationCommand.class ) );
		actions.add( sacService.getCommandName( ManualRegistrationCommand.class ) );
		actions.add( UNDO_SEGMENT_SELECTIONS );
		actions.add( LOAD_ADDITIONAL_VIEWS );
		actions.add( SAVE_CURRENT_SETTINGS_AS_VIEW );

		if ( projectCommands != null )
		{
			for ( String commandName : projectCommands )
			{
				actions.add( commandName );
			}
		}

		contextMenu = new SourceAndConverterContextMenuClickBehaviour( bdvHandle, new SourcesAtMousePositionSupplier( bdvHandle, is2D ), actions.toArray( new String[0] ) );

		// Install keyboard shortcuts

		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.behaviour( contextMenu, "Context menu", "button3", "shift P");
		behaviours.install( bdvHandle.getTriggerbindings(), "MoBIE" );

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> sliceViewAnnotationSelector.run() ).start(),
				"Toggle selection", "ctrl button1" ) ;

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> sliceViewAnnotationSelector.clearSelection() ).start(),
				"Clear selection", "ctrl shift N" ) ;

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () ->
						{
							final SourceAndConverter[] sourceAndConverters = sacService.getSourceAndConverters().toArray( new SourceAndConverter[ 0 ] );
							ConfigureLabelRenderingCommand.incrementRandomColorSeed( sourceAndConverters, bdvHandle );
						}).start(),
				"Change random color seed", "ctrl L" ) ;
	}

	public static BdvHandle createBdv( boolean is2D, String frameTitle )
	{
		final MobieSerializableBdvOptions sOptions = new MobieSerializableBdvOptions();
		sOptions.is2D = is2D;
		sOptions.frameTitle = frameTitle;

		IBdvSupplier bdvSupplier = new MobieBdvSupplier( sOptions );
		SourceAndConverterServices.getBdvDisplayService().setDefaultBdvSupplier( bdvSupplier );
		BdvHandle bdvHandle = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

		return bdvHandle;
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() );
	}

	public void show( Image< ? > image, SourceAndConverter< ? > sourceAndConverter, AbstractDisplay display )
	{
		// register
		SourceAndConverterServices.getSourceAndConverterService().register( sourceAndConverter );
		display.sourceAndConverters().add( sourceAndConverter );

		// link to image
		SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, Image.class.getName(), image );

		// blending mode
		SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.class.getName(), display.getBlendingMode() );

		// time added (for alpha blending)
		SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.TIME_ADDED, System.currentTimeMillis() );

		// opacity
		OpacityHelper.setOpacity( sourceAndConverter, display.getOpacity() );

		// show in Bdv
		SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, display.isVisible(), sourceAndConverter );

		updateTimepointSlider();
	}

	public void updateTimepointSlider()
	{
		if ( bdvHandle == null ) return;
		if ( bdvHandle.getViewerPanel() == null ) return;
		if ( bdvHandle.getViewerPanel().state()	 == null ) return;

		final List< SourceAndConverter< ? > > sacs = bdvHandle.getViewerPanel().state().getSources();
		if ( sacs.size() == 0 ) return;

		int maxNumTimePoints = 1;
		for ( SourceAndConverter< ? > sac : sacs )
		{
			int numTimepoints = SourceHelper.getNumTimepoints( sac.getSpimSource() );
			if ( numTimepoints > maxNumTimePoints ) maxNumTimePoints = numTimepoints;
		}
		bdvHandle.getViewerPanel().state().setNumTimepoints( maxNumTimePoints );
	}

}

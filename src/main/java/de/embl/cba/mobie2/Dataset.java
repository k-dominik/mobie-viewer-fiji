package de.embl.cba.mobie2;

import de.embl.cba.mobie2.view.View;

import java.util.Map;

public class Dataset
{
	public boolean is2D;
	public Map< String, SourceSupplier > sources;
	public Map< String, View > views;
}

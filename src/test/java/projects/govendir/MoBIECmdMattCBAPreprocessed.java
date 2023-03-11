package projects.govendir;

import org.embl.mobie.cmd.MoBIECmd;

class MoBIECmdMattCBAPreprocessed
{
	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ "/Volumes/cba/exchange/matt-govendir/data/preprocessed/*--jun.tif" };
		cmd.grids = new String[]{ "*" };
		cmd.call();
	}
}
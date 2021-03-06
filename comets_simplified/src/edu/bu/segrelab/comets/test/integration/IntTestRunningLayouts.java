package edu.bu.segrelab.comets.test.integration;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;

import javax.swing.JFileChooser;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.IWorld;
import edu.bu.segrelab.comets.fba.FBAModel;
import edu.bu.segrelab.comets.fba.FBAParameters;
import edu.bu.segrelab.comets.reaction.ReactionModel;
import edu.bu.segrelab.comets.test.classes.TComets;
import edu.bu.segrelab.comets.test.etc.TestKineticParameters;

/**A class to include complete simulation runs in order to check their results
 * 
 * @author mquintin
 *
 */
public class IntTestRunningLayouts {

	TComets comets;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		comets = new TComets();
	}

	@After
	public void tearDown() throws Exception {
		IWorld.getReactionModel().clear();
	}
	
	/*//testing Eclipse file IO with FileWriter
	@Test
	public void testFileWriter() throws IOException{
		URL scriptFolderURL = TestKineticParameters.class.getResource("../resources/");
		String folderPath = scriptFolderURL.getPath();
		//String scriptPath = folderPath + File.separator + "comets_script_temp.txt";
		//String layoutPath = folderPath + File.separator + layoutFileName;
		String scriptPath = folderPath + "testFileWriter.txt";
		String layoutPath = folderPath + "comets_layout_unnecessary_cellulase.txt";
		//scriptPath = "C:/Users/mquintin/userworkspace/comets/src/edu/bu/segrelab/comets/test/resources";
		////Don't use absolute paths!
		FileWriter fw = new FileWriter(new File(scriptPath), false);
		fw.write("load_layout " + layoutPath);
		fw.close();
		
		FileReader fr = new FileReader(new File(scriptPath));
		int readresult = fr.read();
		System.out.println(readresult);
		fr.close();
	} */
	
	/*//testing Eclipse file IO with PrintWriter
	@Test
	public void testPrintWriter() throws IOException{
		URL scriptFolderURL = TestKineticParameters.class.getResource("../resources/");
		String folderPath = scriptFolderURL.getPath();
		//String scriptPath = folderPath + File.separator + "comets_script_temp.txt";
		//String layoutPath = folderPath + File.separator + layoutFileName;
		String scriptPath = folderPath + "testPrintWriter.txt";
		String layoutPath = folderPath + "comets_layout_unnecessary_cellulase.txt";
		PrintWriter fw = new PrintWriter(new File(scriptPath), "UTF-8");
		fw.write("load_layout " + layoutPath);
		fw.close();
		
	}*/
	
	/*Test a layout with extracellular enzymatic and decay reactions, and all media components
	 * set to excessive levels.
	 * This test created c.a. 1/21/2020 version 2.7.2 because the model for 
	 * testGrowthWithUnnecessaryExtracellularCellulase was being called infeasible, but older 
	 * versions of Comets were able to get growth from the same files.
	 * If the other test was set up with a skipped essential metabolite, this should work.
	 * 
	 * uses the files yeastGEMxml.txt, comets_layout_excessall_withrxns.txt
	 */
	@Test
	public void testGrowthWithExcessMediaAndExtracellularReactions() throws IOException {
		String layoutFilePath = "comets_layout_excessall_withrxns.txt";
		String scriptFilePath = comets.createScriptForLayout(layoutFilePath);
		comets.loadScript(scriptFilePath);
		double initBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		comets.run();
		double finalBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		assert(finalBiomass > initBiomass);
	}


	/*Test a layout with extracellular enzymatic and enzyme decay reactions. 
	 * Cellulose in the media is converted to glc__D_e by enzyme_e
	 * Enzyme_e also decays over time
	 * This layout includes glucose, so the cellulase is not actually necessary for growth. This test
	 * simply confirms that running the extracellular reaction engine doesn't break feasibility of the
	 * model or the media contents of the FBA cell
	 * 
	 * uses the files yeastGEMxml.txt, comets_layout_unnecessary_cellulase.txt
	 * 
	 * Putting this out of commission for now, it appears the model is just not being given the right 
	 * metabolites
	 */
	@Test
	public void testGrowthWithUnnecessaryExtracellularCellulase() throws IOException {
		String layoutFilePath = "comets_layout_unnecessary_cellulase.txt";
		String scriptFilePath = comets.createScriptForLayout(layoutFilePath);
		comets.loadScript(scriptFilePath);
		double initBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		comets.run();
		double finalBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		assert(finalBiomass > initBiomass);
	}
	
	/*Test that extracellular reactions still run when there is no flux through any metabolic models
	 * 
	 */
	@Test
	public void testRxnsRunWithoutMetabolicActivity() throws IOException {
		String layoutFilePath = "comets_layout_rxns_nogrowth.txt";
		String scriptFilePath = comets.createScriptForLayout(layoutFilePath);
		comets.loadScript(scriptFilePath);
		//double initBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		
		/*int glcIdx = 0;
		String[] medianames = comets.getWorld().getMediaNames();
		for (int i = 0; i < medianames.length; i++) {
			if ("glc-D[e]".equals(medianames[i])) glcIdx = i;
		}*/
		
		int glcIdx = ArrayUtils.indexOf(comets.getWorld().getMediaNames(), "glc-D[e]");
		double initGlc = comets.getWorld().getMediaAt(0, 0)[glcIdx];
		comets.run();
		double finalGlc = comets.getWorld().getMediaAt(0, 0)[glcIdx];
		assert(finalGlc > initGlc);
		
	}
	
	/**Test that if there are objectives besides the biomass reaction, and the biomass reaction
	 * has no flux, the other reactions still run and update the media. 
	 * This behavior is keyed off the FBAParameter allowFluxWithoutGrowth == true
	 * @throws IOException 
	 */
	@Test
	public void testFluxWithoutGrowth() throws IOException {
		//Use the model described in IntTestFBAModelOptimization.testMultiObjective except:
		// -the Biomass reaction is explicitly set to Reaction 4
		// -the bound for Reaction 4 are set to 0
		//There should still be flux through Reactions 3 and 5
		String layoutFilePath = "comets_layout_fluxesWithoutGrowth.txt";
		String scriptFilePath = comets.createScriptForLayout(layoutFilePath);
		comets.loadScript(scriptFilePath);
		double initBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
				
		int cIdx = ArrayUtils.indexOf(comets.getWorld().getMediaNames(), "c1");
		double initC = comets.getWorld().getMediaAt(0, 0)[cIdx];
		int bIdx = ArrayUtils.indexOf(comets.getWorld().getMediaNames(), "bio");
		double initB = comets.getWorld().getMediaAt(0, 0)[bIdx];

		comets.doCommandLineRunWithoutLoading();
		//check that C1 was consumed
		double finalC = comets.getWorld().getMediaAt(0, 0)[cIdx];
		assert(finalC < initC);
		//check that Bio was produced
		double finalB = comets.getWorld().getMediaAt(0, 0)[bIdx];
		assert(finalB > initB);
		//check that Biomass did not change
		double finalBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		assert(finalBiomass == initBiomass);
	}
	
	/**Test that if there are objectives besides the biomass reaction, and the biomass reaction
	 * has no flux, the other reactions will be prevented from running.
	 * This behavior is keyed off the FBAParameter allowFluxWithoutGrowth == false 
	 * @throws IOException 
	 */
	@Test
	public void testDisallowFluxWithoutGrowth() throws IOException {
		//Use the same input as testFluxWithoutGrowth()
		String layoutFilePath = "comets_layout_fluxesWithoutGrowth.txt";
		String scriptFilePath = comets.createScriptForLayout(layoutFilePath);
		comets.loadScript(scriptFilePath);
		double initBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		double[] initMedia = comets.getWorld().getMediaAt(0, 0);
		
		//disable FBAParameters.allowFluxWithoutGrowth
		((FBAParameters)comets.getPackageParameters()).setAllowFluxWithoutGrowth(false);
		//comets.synchronizeParameters();
		
		comets.doCommandLineRunWithoutLoading();
		
		//check that no media changed
		double[] finalMedia = comets.getWorld().getMediaAt(0, 0);
		boolean mediaUnchanged = true;
		for (int i = 0; i < initMedia.length; i++) {
			if (initMedia[i] != finalMedia[i]) mediaUnchanged = false;
		}
		assert(mediaUnchanged);
		//check that Biomass did not change
		double finalBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		assert(finalBiomass == initBiomass);
	}


}

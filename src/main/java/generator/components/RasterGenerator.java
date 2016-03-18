/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package generator.components;

import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.imageio.IIOMetadataDumper;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.expression.ParseException;
import org.springframework.stereotype.Component;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;

/**
 * 
 * @author Sonny.Saniev
 * 
 */
@Component
public class RasterGenerator {
//	@Value("${s3.key.access:}")
//	private String AMAZONS3_ACCESS_KEY;
//	@Value("${s3.key.private:}")
//	private String AMAZONS3_PRIVATE_KEY;

	private static final String STATIC_LOCAL_FIELD = "URL";
	
	public String cropGeoTiff() {
		
		return "true";
	}
	
	/**
	 * Create a cropped coverage.
	 * 
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws UnsupportedOperationException
	 * @throws ParseException
	 * @throws FactoryException
	 * @throws TransformException
	 */
	public void testWriteCroppedCoverage() throws IllegalArgumentException,
			IOException, UnsupportedOperationException, ParseException,
			FactoryException, TransformException {

		boolean interactive = false;

		// Grab the original file to want to crop from s3
		final File readdir = new File("C:\\geoFiles\\geotiff"); //TestData.file(GeoTiffWriterTest.class, "");
		final File writedir = new File(new StringBuilder(readdir.getAbsolutePath()).append("\\writeDir").toString());
		writedir.mkdir();

		//String geotiffPath = String.format("%s%s%s", readdir.getAbsolutePath(), File.separator, "nasa_land_ocean_ice.tif");
		String geotiffPath = String.format("%s%s%s", readdir.getAbsolutePath(), File.separator, "NASA-GDEM-10km-colorized.tif");
		
		final File tiff = new File(geotiffPath);
		assert tiff.exists() && tiff.canRead() && tiff.isFile();
		
		// Create format and reader
		final GeoTiffFormat format = new GeoTiffFormat();
		// getting a reader
		GridCoverageReader reader = format.getReader(tiff);

		// print metadata
		IIOMetadataDumper metadataDumper = new IIOMetadataDumper(((GeoTiffReader) reader).getMetadata().getRootNode());
		System.out.println("----------------------------------- metadata from tiff: " + tiff.getAbsolutePath() + "\n" + metadataDumper.getMetadata());
		
		// Read the original coverage.
		GridCoverage2D gc = (GridCoverage2D) reader.read(null);
		System.out.println(new StringBuilder("\n-----\nCoverage before: ").append("\n").append(gc.getCoordinateReferenceSystem().toWKT())
				.append(gc.getEnvelope().toString()).toString());

		final CoordinateReferenceSystem sourceCRS = gc.getCoordinateReferenceSystem2D();
		final GeneralEnvelope sourceEnvelope = (GeneralEnvelope) gc.getEnvelope();
		final GridGeometry2D sourcedGG = (GridGeometry2D) gc.getGridGeometry();
		final MathTransform sourceG2W = sourcedGG.getGridToCRS(PixelInCell.CELL_CENTER);

		// Crop the raster
		double xc = sourceEnvelope.getMedian(0);
		double yc = sourceEnvelope.getMedian(1);
		double xl = sourceEnvelope.getSpan(0);
		double yl = sourceEnvelope.getSpan(1);
		final GeneralEnvelope cropEnvelope = new GeneralEnvelope(new double[] { xc - xl / 4.0, yc - yl / 4.0 }, new double[] { xc + xl / 4.0, yc + yl / 4.0 });
		final CoverageProcessor processor = CoverageProcessor.getInstance();
		final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
		param.parameter("Source").setValue(gc);
		param.parameter("Envelope").setValue(cropEnvelope);
		final GridCoverage2D cropped = (GridCoverage2D) processor.doOperation(param);


		// checking the ranges of the output image.
		final GridGeometry2D croppedGG = (GridGeometry2D) cropped.getGridGeometry();
		final GridEnvelope croppedGR = croppedGG.getGridRange();
		final MathTransform croppedG2W = croppedGG.getGridToCRS(PixelInCell.CELL_CENTER);
		final GeneralEnvelope croppedEnvelope = (GeneralEnvelope) cropped.getEnvelope();
		
		// check that the affine transform are the same thing
		//Assert.assertTrue("The Grdi2World tranformations of the original and the cropped covearage do not match", sourceG2W.equals(croppedG2W));
		
		// check that the envelope is correct
		final GeneralEnvelope expectedEnvelope = new GeneralEnvelope(croppedGR,PixelInCell.CELL_CENTER, croppedG2W, cropped.getCoordinateReferenceSystem2D());
		//Assert.assertTrue("Expected envelope is different from the computed one",expectedEnvelope.equals(croppedEnvelope, XAffineTransform.getScale((AffineTransform) croppedG2W) / 2.0, false));

		// Dispose things
		cropped.dispose(true);
		gc.dispose(true);
		try {
			if (reader != null)
				reader.dispose();
		} catch (Throwable e) {
		}
		
		// WRITING AND TESTING
		final File writeFile = new File(new StringBuilder(writedir.getAbsolutePath()).append(File.separator).append(cropped.getName().toString()).append(".tif").toString());
		final GridCoverageWriter writer = format.getWriter(writeFile);

		try{
			writer.write(cropped, null);
		}catch (IOException e) {
		}
		finally{
			try{
				writer.dispose();
			}catch (Throwable e) {
			}
		}

//		try{
//			reader = new GeoTiffReader(writeFile, null);
////			assertNotNull(reader);
//			
//			gc = (GridCoverage2D) reader.read(null);
////			assertNotNull(gc);
//			
//			final CoordinateReferenceSystem targetCRS = gc.getCoordinateReferenceSystem2D();
//			//Assert.assertTrue("Source and Target coordinate reference systems do not match", CRS.equalsIgnoreMetadata(sourceCRS, targetCRS));
//			//Assert.assertEquals("Read-back and Cropped envelopes do not match", cropped.getEnvelope(), croppedEnvelope);
//	
//			if (interactive) {
//				System.out.println(new StringBuilder("Coverage after: ").append("\n").append(gc.getCoordinateReferenceSystem().toWKT())
//						.append(gc.getEnvelope().toString()).toString());
//				if (interactive)
//					gc.show();
//				else
//					PlanarImage.wrapRenderedImage(gc.getRenderedImage()).getTiles();
//			}
//
//		} finally {
//			try {
//				if (reader != null) {
//					reader.dispose();
//				}
//			} catch (Throwable e) {
//			}
//
//			// if(!interactive)
//			// gc.dispose(true);
//		}
		
		
		
		
		
		
	}
	
	
	
	
}
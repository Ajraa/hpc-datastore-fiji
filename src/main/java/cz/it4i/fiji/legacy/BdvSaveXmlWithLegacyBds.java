/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy;

import client.base.GraphQLClient;
import client.base.GraphQLException;
import models.DataReturn;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.command.Command;
import org.scijava.log.LogService;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import client.BigDataService;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>HPC DataStore>BigDataViewer>Save XML (legacy BDS)")
public class BdvSaveXmlWithLegacyBds implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String url = "someHostname:9080";

	@Parameter(label = "UUID of the dataset to be modified:", persistKey = "datasetdatasetid")
	public String datasetID = "someDatasetUUID";

	@Parameter(label = "Selected version:",
			description = "provide number, or keyword: mixedLatest",
			persistKey="datasetversion")
	public String versionAsStr = "latest";

	@Parameter(label = "Save as .xml:", style = "file,extensions:xml")
	public File outputXml;

	@Parameter(label = "UseGraphQL:", persistKey = "usegraphql")
	public boolean useGraphql = false;

	@Parameter
	public LogService logService;

	@Override
	public void run() {
		if (useGraphql) {
			GraphQLClient client = GraphQLClient.getInstance(url + "/graphql");
            try {
                DataReturn dt = new BigDataService(client).getCell(datasetID, versionAsStr, null);
				if (dt.getReturnType() != DataReturn.ReturnType.XML) throw new Exception("Expected XML, got " + dt.getReturnType());

				try (InputStream in = new ByteArrayInputStream(dt.getData().getBytes(StandardCharsets.UTF_8));
					 OutputStream out = new FileOutputStream(outputXml)) {

					final byte[] buffer = new byte[8192];
					int size = in.read(buffer);
					while (size > 0) {
						out.write(buffer, 0, size);
						size = in.read(buffer);
					}
					out.flush();

					logService.info("Written file: " + outputXml);
				}
            } catch (GraphQLException e) {
				logService.error(e.toString());
				e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
		else {
			String queryUrl = "http://"+url+"/bdv/"+datasetID+"/"+versionAsStr;
			logService.info("Polling URL: "+queryUrl);
			try (InputStream in = new URL(queryUrl).openStream();
				 OutputStream out = new FileOutputStream(outputXml)) {

				final byte[] buffer = new byte[8192];
				int size = in.read(buffer);
				while (size > 0) {
					out.write(buffer, 0, size);
					size = in.read(buffer);
				}
				out.flush();

				logService.info("Written file: "+outputXml);
			}
			catch (Exception e) {
				logService.error("Some connection problem while fetching XML: "+e.getMessage());
				logService.error("It is likely that UUID is wrong, or no such version exists for the particular dataset.");
				e.printStackTrace();
			}
		}
	}
}

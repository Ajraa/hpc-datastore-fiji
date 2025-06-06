/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy;

import client.base.GraphQLException;
import client.builders.DatasetBuilder;
import cz.it4i.fiji.rest.util.DatasetInfo;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.io.IOException;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>HPC DataStore>Query>Dataset label")
public class QueryDatasetLabel implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String url = "someHostname:9080";

	@Parameter(label = "UUID of the dataset to be queried:", persistKey = "datasetdatasetid")
	public String datasetID = "someDatasetUUID";

	@Parameter(label = "UseGraphQL:", persistKey = "usegraphql")
	public boolean useGraphql = false;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Override
	public void run() {
		//logging facility
		myLogger = mainLogger.subLogger("HPC QueryDataset", LogLevel.INFO);

		try {
			myLogger.info("Quering "+datasetID+" from "+url);
			DatasetInfo di = DatasetInfo.createFrom(url, datasetID, useGraphql, new DatasetBuilder().label());
			myLogger.info(di.toString());
			datasetLabel = di.getLabel();
			datasetSource = datasetID+" @ "+url;

		} catch (IOException e) {
			myLogger.error("Some connection problem:");
			e.printStackTrace();
		} catch (GraphQLException e) {
			myLogger.error(e.toString());
			throw new RuntimeException(e);
        }
    }

	@Parameter(type = ItemIO.OUTPUT, label="Label of the queried dataset:")
	public String datasetLabel = "(likely a connection error)";
	@Parameter(type = ItemIO.OUTPUT, label="UUID + host of the queried dataset:")
	public String datasetSource = "(likely a connection error)";
}

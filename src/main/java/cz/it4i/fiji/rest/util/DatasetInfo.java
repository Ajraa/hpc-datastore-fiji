/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.rest.util;

import java.net.URL;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import client.RegisterService;
import client.base.GraphQLClient;
import client.base.GraphQLException;
import client.builders.DatasetBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.QLDatasetDTO;
import models.QLResolution;
import models.QLResolutionLevel;

public class DatasetInfo {
	public String uuid;
	public String voxelType;

	//6D space as 3D+1+1+1
	public List<Integer> dimensions;
	public int timepoints;
	public int channels;
	public int angles;

	//sizes and units along the 6 axes
	public String voxelUnit;
	public List<Double> voxelResolution;
	public ResolutionWithOwnUnit timepointResolution;
	public ResolutionWithOwnUnit channelResolution;
	public ResolutionWithOwnUnit angleResolution;

	public String compression;

	public List<ResolutionLevel> resolutionLevels;
	public List<Integer> versions;
	public String label;

	/** a common label provider with default fallback response/label */
	public static final String NO_LABEL_MSG = "(no label is attached)";
	public String getLabel() {
		if (label == null || label.length() == 0) return NO_LABEL_MSG;
		return label;
	}

	public double[][] transformations;

	public double[][] viewRegistrations;

	public List<Integer> timepointIds;

	public String datasetType;

	public DatasetInfo() {}

	@Override
	public String toString() {
		return "UUID = " + uuid +
				" (storage type " + (datasetType == null?"BDV's N5":datasetType) + ")" +
				"\nlabel = " + label +
				"\nvoxelType = " + voxelType +
				"\ndimensions,timepoints,channels,angles = " +
					dimensions + ", " + timepoints + "," + channels + "," + angles +
				"\ndeclared timepoints = " + timepointIds +
				"\ntransformations array length = " + (transformations != null ? transformations.length : "NA") +
				"\nviewRegistrations array length = " + (viewRegistrations != null ? viewRegistrations.length : "NA") +
				"\nvoxelResolution = " + voxelResolution + " " + voxelUnit +
				"\ntimepointResolution = " + timepointResolution +
				"\nchannelResolution = " + channelResolution +
				"\nangleResolution = " + angleResolution +
				"\ncompression = " + compression +
				"\nresolutionLevels = [\n  " +// resolutionLevels +
					resolutionLevels.stream().map(l -> l.toString()+"\n  ").collect(Collectors.joining()) +
				"]\nversions = " + versions;
	}


	// ========= compound-types corresponding to compound-elements in the JSON messages =========
	public static class ResolutionWithOwnUnit {
		public ResolutionWithOwnUnit() {
			//2nd arg intentionally as a String
			this(0.0, "null");
		}
		public ResolutionWithOwnUnit(final double value, final String unit) {
			this.value = value;
			this.unit = unit;
		}

		public double value;
		public String unit;

		@Override
		public String toString() {
			return value + " " + (unit != null ? unit : "null");
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || o.getClass() != ResolutionWithOwnUnit.class) return false;
			final ResolutionWithOwnUnit that = (ResolutionWithOwnUnit)o;
			return this.value == that.value && this.unit.equals(that.unit);
		}
		@Override
		public int hashCode() {
			return (int)(value*unit.hashCode());
		}
	}

	public static class ResolutionLevel {
		public ResolutionLevel() {
			//intentionally invalid values!
			this(-1,-1,-1, -1,-1,-1);
		}
		public ResolutionLevel(final int res_x,   final int res_y,   final int res_z,
		                       final int block_x, final int block_y, final int block_z) {
			resolutions = Arrays.asList(res_x, res_y, res_z);
			blockDimensions = Arrays.asList(block_x, block_y, block_z);
		}

		public List<Integer> resolutions;
		public List<Integer> blockDimensions;
		public int[] dimensions = new int[3];

		public void setDimensions(final List<Integer> fullResDims) {
			for (int d = 0; d < 3; ++d) {
				dimensions[d] = fullResDims.get(d) / resolutions.get(d);
				if (dimensions[d] == 0) dimensions[d] = 1; //make sure no dimension is lost (by making it 0 pixels wide)
			}
		}

		@Override
		public String toString() {
			return "down-level " + resolutions
					+ " (-> img dims ["+dimensions[0]+","+dimensions[1]+","+dimensions[2]+"])"
					+ " with blocks " + blockDimensions;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || o.getClass() != ResolutionLevel.class) return false;
			final ResolutionLevel that = (ResolutionLevel)o;
			return this.resolutions.equals(that.resolutions)
					&& this.blockDimensions.equals(that.blockDimensions);
		}
		@Override
		public int hashCode() {
			return resolutions.hashCode()-blockDimensions.hashCode();
		}
	}


	public static DatasetInfo createFrom(final String hostnameURL, final String datasetID)
	throws IOException
	{
		final ObjectMapper om = new ObjectMapper();
		final DatasetInfo di = om.readValue(new URL("http://"+hostnameURL+"/datasets/"+datasetID), DatasetInfo.class);
		//finish what ObjectMapper couldn't do...
		for (ResolutionLevel l : di.resolutionLevels)
			l.setDimensions(di.dimensions);
		return di;
	}

	public static DatasetInfo createFrom(final String hostnameURL, final String datasetID, boolean useGraphql)
            throws IOException, GraphQLException {
		if (!useGraphql) return createFrom(hostnameURL, datasetID);

		GraphQLClient client = GraphQLClient.getInstance(hostnameURL + "/graphql");
		QLDatasetDTO dto = new RegisterService(client).queryDataset(datasetID);
		return new DatasetInfo(dto);
	}

	public static DatasetInfo createFrom(final String hostnameURL, final String datasetID, boolean useGraphql, DatasetBuilder b)
			throws IOException, GraphQLException {
		if (!useGraphql) return createFrom(hostnameURL, datasetID);

		GraphQLClient client = GraphQLClient.getInstance(hostnameURL + "/graphql");
		QLDatasetDTO dto = new RegisterService(client).queryDataset(datasetID, b);
		return new DatasetInfo(dto);
	}

	public QLDatasetDTO convertToQL() {
		QLDatasetDTO dto = new QLDatasetDTO();

		dto.setUuid(this.uuid);
		dto.setVoxelType(this.voxelType);

		List<Long> longDimensions = this.dimensions.stream()
				.mapToLong(Integer::longValue)
				.boxed()
				.collect(Collectors.toList());
		dto.setDimensions(longDimensions);

		dto.setTimepoints(this.timepoints);
		dto.setChannels(this.channels);
		dto.setAngles(this.angles);
		dto.setVoxelUnit(this.voxelUnit);

		List<Float> floatResolution = this.voxelResolution.stream()
				.map(Double::floatValue)
				.collect(Collectors.toList());
		dto.setVoxelResolution(floatResolution);

		dto.setTimepointResolution(new QLResolution(this.timepointResolution.unit, (float) this.timepointResolution.value));
		dto.setChannelResolution(new QLResolution(this.channelResolution.unit, (float) this.channelResolution.value));
		dto.setAngleResolution(new QLResolution(this.angleResolution.unit, (float) this.angleResolution.value));
		dto.setCompression(this.compression);

		List<QLResolutionLevel> resolutionLevels = new ArrayList<>();
		for (ResolutionLevel l : this.resolutionLevels)
			resolutionLevels.add(new QLResolutionLevel(l.resolutions, l.blockDimensions));
		dto.setResolutionLevels(resolutionLevels);

		dto.setVersions(this.versions);
		dto.setLabel(this.label);

		return dto;
	}
	public DatasetInfo(QLDatasetDTO dataset) {
		this.uuid = dataset.getUuid();
		this.voxelType = dataset.getVoxelType();

        this.dimensions = dataset.getDimensions().stream()
                .map(Long::intValue)
                .collect(Collectors.toList());

		this.timepoints = dataset.getTimepoints();
		this.channels = dataset.getChannels();
		this.angles = dataset.getAngles();
		this.voxelUnit = dataset.getVoxelUnit();

		this.voxelResolution = dataset.getVoxelResolution().stream()
				.map(Float::doubleValue)
				.collect(Collectors.toList());

		this.timepointResolution = new ResolutionWithOwnUnit();
		this.timepointResolution.unit = dataset.getTimepointResolution().getUnit();
		this.timepointResolution.value = dataset.getTimepointResolution().getValue();

		this.channelResolution = new ResolutionWithOwnUnit();
		this.channelResolution.unit = dataset.getChannelResolution().getUnit();
		this.channelResolution.value = dataset.getChannelResolution().getValue();

		this.angleResolution = new ResolutionWithOwnUnit();
		this.angleResolution.unit = dataset.getAngleResolution().getUnit();
		this.angleResolution.value = dataset.getAngleResolution().getValue();

		this.compression = dataset.getCompression();

		this.resolutionLevels = new ArrayList<>();
		for (QLResolutionLevel l : dataset.getResolutionLevels()) {
			ResolutionLevel rl = new ResolutionLevel();
			rl.resolutions = l.getResolutions();
			rl.blockDimensions = l.getBlockDimensions();
			resolutionLevels.add(rl);
		}

		this.versions = dataset.getVersions();
		this.label = dataset.getLabel();
	}
}

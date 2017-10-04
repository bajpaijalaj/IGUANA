package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.utils.FileUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;

/**
 * 
 * A Worker using SPARQL 1.1 to create service request.
 * 
 * @author f.conrads
 *
 */
public class SPARQLWorker extends AbstractWorker {

	private String service;
	private long timeOut;
	private int currentQueryID = 0;

	private Random queryPatternChooser;

	public SPARQLWorker() {
	}

	public SPARQLWorker(String taskID, String workerID, String workerType, String timeLimit, String service, String timeOut,
			String queryFileName, String fixedLatency, String gaussianLatency) {
		this(taskID, Integer.parseInt(workerID), workerType, Long.parseLong(timeLimit), service, Long.parseLong(timeOut),
				queryFileName, Integer.parseInt(fixedLatency), Integer.parseInt(gaussianLatency));
	}
	
	public SPARQLWorker(String taskID, int workerID, String workerType, Long timeLimit, String service, Long timeOut,
			String queryFileName, Integer fixedLatency, Integer gaussianLatency) {
		super(taskID, workerID, workerType, timeLimit, queryFileName, fixedLatency, gaussianLatency);
		this.service = service;
		this.timeOut = 180000;
		if(timeOut != null)
			this.timeOut = timeOut;

		queryPatternChooser = new Random(this.workerID);
		
	}

	@Override
	public void init(Properties p) {
		// At first call init from AbstractWorker!
		super.init(p);
		this.service = p.getProperty(CONSTANTS.SPARQL_CURRENT_ENDPOINT);
		this.timeOut = (long) p.getOrDefault(CONSTANTS.SPARQL_TIMEOUT, 180000);
		
		queryPatternChooser = new Random(this.workerID);
		
	}

	@Override
	public long getTimeForQueryMs(String query, String queryID) {
		QueryExecution exec = QueryExecutionFactory.sparqlService(service, query);
		// Set query timeout				
		exec.setTimeout(this.timeOut, this.timeOut);
		try {
			long start = System.currentTimeMillis();
			// Execute Query
			ResultSet res = exec.execSelect();
			// check ResultSet.
			int size = ResultSetFormatter.consume(res);
			long end = System.currentTimeMillis();
			LOGGER.debug("Worker[{{}} : {{}}]: Query with ID {{}} took {{}} and has {{}} results.", this.workerType,
					this.workerID, queryID, end - start, size);
			// Return time
			return end - start;
		} catch (Exception e) {
			LOGGER.warn("Worker[{{}} : {{}}]: Could not execute the following query\n{{}}\n due to", this.workerType,
					this.workerID, query, e);
		}
		// Exception was thrown, return error
		return -1L;
	}

	@Override
	public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
		// get next Query File and next random Query out of it.
		File currentQueryFile = this.queryFileList[this.currentQueryID++];
		queryID.append(currentQueryFile.getName());

		int queriesInFile = FileUtils.countLines(currentQueryFile);
		int queryLine = queryPatternChooser.nextInt(queriesInFile);
		queryStr.append(FileUtils.readLineAt(queryLine, currentQueryFile));

		// If there is no more query(Pattern) start from beginning.
		if (this.currentQueryID >= this.queryFileList.length) {
			this.currentQueryID = 0;
		}
		
	}

	@Override
	public void setQueriesList(File[] queries) {
		super.setQueriesList(queries);
		this.currentQueryID = queryPatternChooser.nextInt(this.queryFileList.length);
	}

	
}

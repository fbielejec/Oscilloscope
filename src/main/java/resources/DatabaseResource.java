package resources;

import static spark.Spark.get;
import static spark.Spark.post;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sql2o.Sql2o;

import com.google.gson.Gson;

import data.Line;
import data.OscilloscopeData;
import model.Database;
import model.Sql2oDatabase;
import utils.JsonTransformer;

public class DatabaseResource {

	private static final String ILLEGAL_CHARACTER = ".";
	private static final String API_CONTEXT = "/database";
	private static final String TEST_TABLE = "test";
	private final Database db;

	public DatabaseResource() {

		String username = "postgres";
		String password = "chaos555";
		String db_name = "postgres";
		String url = "jdbc:postgresql://localhost:5432/" + db_name;
		Sql2o sql2o = new Sql2o(url, username, password);
		this.db = new Sql2oDatabase(sql2o);

		setupEndpoints();
	}

	private void setupEndpoints() {

		// post for column names, calls model createTable
		post(API_CONTEXT + "/create", (request, response) -> {
			try {

				ColumnNames columns = new Gson().fromJson(request.body(), ColumnNames.class);
				List<String> colnames = columns.getColumnNames();

				if (!isColnamesValid(colnames)) {
					response.status(400);
					return response;
				}

				db.dropTable(TEST_TABLE);

				System.out.println("DB " + TEST_TABLE + " dropped");

				db.createTable(TEST_TABLE, columns.getColumnNames());

				System.out.println("DB " + TEST_TABLE + " created");

				response.status(200);
				response.type("application/json");

				return response;
			} catch (Exception e) {
				e.printStackTrace();
				response.status(400);
				return response;
			}
		});

		// post for row inserts
		post(API_CONTEXT + "/insert", (request, response) -> {
			try {

				Row row = new Gson().fromJson(request.body(), Row.class);

				db.insertRow(row.getRow(), TEST_TABLE);

				response.status(200);
				response.type("application/json");

				return response;
			} catch (Exception e) {
				e.printStackTrace();
				response.status(400);
				return response;
			}
		});

		// get for results
		get(API_CONTEXT + "/data/all", "application/json", (request, response) -> {

			// TODO: needed?
			List<String> colnames = db.getColumnNames(TEST_TABLE);
			List<Line> lines = db.getAllRows(colnames, TEST_TABLE);

			return new OscilloscopeData(lines);
		} , new JsonTransformer());

		// get for results
		get(API_CONTEXT + "/data/:n", "application/json", (request, response) -> {
			
			Integer n = Integer.valueOf(request.params(":n"));

			// TODO: needed?
			List<String> colnames = db.getColumnNames(TEST_TABLE);
			List<Line> lines = db.getLastNRows(colnames, TEST_TABLE, n);

			return new OscilloscopeData(lines);
		} , new JsonTransformer());

	}

	private Boolean isColnamesValid(List<String> colnames) {

		List<String> illegals = colnames.stream().filter(name -> name.contains(ILLEGAL_CHARACTER))
				.collect(Collectors.toList());

		if (illegals.size() > 0) {
			System.out.println(
					"Log contains illegal character " + ILLEGAL_CHARACTER + "in columns " + illegals.toString());
			return false;
		}

		Set<?> duplicates = colnames.stream().filter(i -> Collections.frequency(colnames, i) > 1)
				.collect(Collectors.toSet());

		if (duplicates.size() > 0) {
			System.out.println("Log contains duplicate elements " + duplicates.toString());
			return false;
		}

		return true;
	}

}

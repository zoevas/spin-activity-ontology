import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.spin.SpinSail;
/**
* <h1>Overlapped datetimes spin rule</h1>
* The SpinActivityOntology program implements an application that
* modifies the start value of an activity if there is a datetime overlap.
* It loads an ontology for activities of sensors installed in a house, 
* the spin rule checking for datetime overlaps, the sensor measurements.
* Finally, it outputs all the start and end datetimes of the elements in order to
* notice the modified start datetimes.
* <p>
* <b>Note:</b> The Activity_Statements.owl has been produced by IoTSemantic application.
*
* @author  Zoe Vasileiou
* @version 1.0
* @since   2020-02-11
*/

public class SpinActivityOntology {
	public static void main(String[] args) throws RDFParseException, RepositoryException, IOException {
		// TODO Auto-generated method stub

		// create a basic Sail Stack with a simple Memory Store and SPIN inferencing support
		SpinSail spinSail = new SpinSail();
		spinSail.setBaseSail(new MemoryStore());
		// create a repository with the Sail stack:
		Repository rep = new SailRepository(spinSail);
		rep.init();
		
		// Open a connection to the database
		try (RepositoryConnection conn = rep.getConnection()) {
			//Add Ontology to the repository
			conn.add(SpinActivityOntology.class.getResourceAsStream("/Activity_Ontology.owl"), "urn:base", RDFFormat.TURTLE);
			
			//Add Spin Modify rule to the repository
			conn.add(SpinActivityOntology.class.getResourceAsStream("/Spin_Rule_Overlap.ttl"), "urn:base", RDFFormat.TURTLE);
			
			//Add the rdf statements to the repository created by IoTSemantic app
			conn.add(SpinActivityOntology.class.getResourceAsStream("/Activity_Statements.owl"), "urn:base", RDFFormat.TURTLE);
			
			String queryString = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \r\n" + 
					"PREFIX ac: <http://www.semanticweb.org/user/ontologies/2020/1/activity#>\r\n" + 
					"SELECT ?e ?start_date ?end_date\r\n" + 
					"WHERE { \r\n" + 
					"    ?a ac:hasElement  ?e . \r\n" + 
					"    ?a a ac:Activity . \r\n" + 
					"    ?a ac:hasObservation ?o .\r\n" + 
					"	?o a ac:Observation .\r\n" + 
					"   ?e ac:hasStartDate ?sd .\r\n" + 
					"	?e ac:hasEndDate ?ed .\r\n" +
					"   bind (str(?sd) as ?start_date)\r\n" +
					"   bind (str(?ed) as ?end_date)\r\n" +
					"}";
			
			
			//Just querying for validating the spin modify rule
			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,queryString);
			
			// A QueryResult is also an AutoCloseable resource, so make sure it gets closed when done.
			try (TupleQueryResult result = query.evaluate()) {
				// we just iterate over all solutions in the result...
				while (result.hasNext()) {
					BindingSet solution = result.next();
					IRI e = (IRI) solution.getBinding("e").getValue();
					System.out.println("?e = " + e.getLocalName() + ", ?sd = " + solution.getValue("start_date") + ", ?ed = " + solution.getValue("end_date"));
				}
			}
		} finally {
			// Before our program exits, make sure the database is properly shut down.
			rep.shutDown();
		}
	}

}

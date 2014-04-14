package org.aksw.rdfunit.sources;

import org.aksw.rdfunit.enums.TestAppliesTo;
import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheCoreEx;
import org.aksw.jena_sparql_api.cache.extra.CacheCoreH2;
import org.aksw.jena_sparql_api.cache.extra.CacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheExImpl;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Dimitris Kontokostas
 * Describes an arbitary datatest source TODO make this abstract and create an EndpointSource and a DumpSource
 * Created: 9/16/13 1:54 PM
 */

public class DatasetSource extends Source {

    private final String sparqlEndpoint;
    private final java.util.Collection <String> sparqlGraph;

    public DatasetSource(String prefix, String uri) {
        this(prefix, uri, uri, new ArrayList<String>(), null);
    }

    public DatasetSource(String prefix, String uri, String sparqlEndpoint, java.util.Collection <String> sparqlGraph, java.util.Collection <SchemaSource> schemata) {
        super(prefix, uri);
        this.sparqlEndpoint = sparqlEndpoint;
        this.sparqlGraph = new ArrayList<String>(sparqlGraph);
        if (schemata != null)
            addReferencesSchemata(schemata);
    }

    public DatasetSource(DatasetSource source) {
        this(source.getPrefix(), source.getUri(), source.getSparqlEndpoint(), source.getSparqlGraphs(), source.getReferencesSchemata());
    }

    @Override
    public TestAppliesTo getSourceType() {
        return TestAppliesTo.Dataset;
    }

    @Override
    protected QueryExecutionFactory initQueryFactory() {

        QueryExecutionFactory qef;
        // if empty
        if (getSparqlGraphs() == null || getSparqlGraphs().isEmpty() )
            qef = new QueryExecutionFactoryHttp(getSparqlEndpoint());
        else
         qef = new QueryExecutionFactoryHttp(getSparqlEndpoint(), getSparqlGraphs());


        // Add delay in order to be nice to the remote server (delay in milli seconds)
        qef = new QueryExecutionFactoryDelay(qef, 7000);

        QueryExecutionFactory qefBackup = qef;

        try {
            // Set up a cache
            // Cache entries are valid for 7 days
            long timeToLive = 7l * 24l * 60l * 60l * 1000l;

            // This creates a 'cache' folder, with a database file named 'sparql.db'
            // Technical note: the cacheBackend's purpose is to only deal with streams,
            // whereas the frontend interfaces with higher level classes - i.e. ResultSet and Model
            CacheCoreEx cacheBackend = CacheCoreH2.create(getPrefix(), timeToLive, true);
            CacheEx cacheFrontend = new CacheExImpl(cacheBackend);
            qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);
        } catch (Exception e) {
            //Try to create cache, if fails continue...
            qef = qefBackup;
        }

        // Add pagination
        qef = new QueryExecutionFactoryPaginated(qef, 900);

        return qef;
    }

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

    public Collection <String> getSparqlGraphs() {
        return sparqlGraph;
    }

    public String getFirstSparqlGraph() {
        for (String graph : getSparqlGraphs())
            return graph;
        return "";
    }
}
